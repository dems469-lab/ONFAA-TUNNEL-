package com.onfaa.tunnel.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.onfaa.tunnel.MainActivity
import com.onfaa.tunnel.R
import com.onfaa.tunnel.core.xray.EngineSettings
import com.onfaa.tunnel.core.xray.XrayBridge
import com.onfaa.tunnel.core.xray.XrayConfigBuilder
import com.onfaa.tunnel.core.xray.XrayErrorSanitizer
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.onfaaAppContainer
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Real Android VPN service backed by official XTLS/Xray-core via libXray.
 *
 * Lifecycle: VpnService.prepare (UI) -> startForegroundService -> establish TUN
 * -> protect outbound fds -> setDNS -> runXray -> real probe -> Connected.
 * Reverse order on stop. No state is reported as connected before the engine
 * is running and the probe succeeded.
 */
class OnfaaTunnelVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.onfaa.tunnel.action.CONNECT"
        const val ACTION_DISCONNECT = "com.onfaa.tunnel.action.DISCONNECT"

        const val EXTRA_SHARE_LINK = "com.onfaa.tunnel.extra.SHARE_LINK"
        const val EXTRA_DNS_SERVER = "com.onfaa.tunnel.extra.DNS_SERVER"
        const val EXTRA_PROBE_URL = "com.onfaa.tunnel.extra.PROBE_URL"

        const val DEFAULT_DNS_SERVER = XrayConfigBuilder.DEFAULT_RESOLVER_ENDPOINT
        const val DEFAULT_PROBE_URL = "https://www.gstatic.com/generate_204"

        private const val CHANNEL_ID = "onfaa_tunnel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "OnfaaTunnel"

        // Android applies the VPN link (routes, DNS) asynchronously after
        // establish() returns, so the first probe may still resolve through the
        // previous network. Wait for the VPN to be the app network, then retry.
        private const val VPN_READY_TIMEOUT_MS = 15_000L
        private const val PROBE_ATTEMPTS = 6
        private const val PROBE_RETRY_DELAY_MS = 3_000L
        private const val PROBE_TOTAL_TIMEOUT_MS = 40_000L

        // After Connected, the tunnel is re-proven with a real request. A
        // failure opens a bounded recovery window: the state is Reconnecting
        // (never Connected) until a probe succeeds again.
        private const val HEALTH_CHECK_INTERVAL_MS = 20_000L
        private const val RECONNECT_TIMEOUT_MS = 90_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycle = Mutex()

    private var tun: ParcelFileDescriptor? = null
    private var engineRunning = false
    private var trafficJob: Job? = null
    private var healthJob: Job? = null

    // True while the tunnel is known to forward traffic (IP probe) but
    // hostname resolution through it has not succeeded yet.
    @Volatile
    private var dnsPendingNotice = false

    private var connectedSinceEpochMs: Long = 0L

    @Volatile
    private var destroyed = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                showForeground("Connexion…")
                scope.launch { connect(intent) }
            }

            ACTION_DISCONNECT -> scope.launch { disconnect("Déconnexion demandée") }

            else -> {
                TunnelStatusRepository.log("Service démarré sans action")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun connect(intent: Intent) = lifecycle.withLock {
        val currentState = TunnelStatusRepository.state.value
        if (engineRunning ||
            currentState is TunnelState.Connecting ||
            currentState is TunnelState.Verifying ||
            currentState is TunnelState.Connected ||
            currentState is TunnelState.Reconnecting
        ) {
            TunnelStatusRepository.log("Connexion déjà active — demande ignorée", LogSeverity.WARNING)
            return@withLock
        }

        TunnelStatusRepository.log("Connexion demandée")

        try {
            // Consent first: never import a link or touch the engine when the
            // user (or the platform) has not granted the VPN permission.
            if (VpnService.prepare(this) != null) {
                throw IllegalStateException("Permission VPN refusée par Android")
            }

            TunnelStatusRepository.set(TunnelState.Preparing)
            updateNotification("Préparation du tunnel…")
            val profile = resolveProfile(intent)
            TunnelStatusRepository.log("Configuration ${profile.protocol.name} validée")

            if (!XrayBridge.isAvailable()) {
                throw IllegalStateException("Moteur libXray indisponible dans cette compilation")
            }

            val dnsEndpointExtra = intent.getStringExtra(EXTRA_DNS_SERVER)
            // Engine options come from the persisted user preferences; the
            // intent DNS extra stays a deterministic override for tests and
            // deep links. Every value below is really applied to the config.
            val prefs = applicationContext.onfaaAppContainer.userPreferencesRepository.first()
            val settings = EngineSettings.from(
                preferences = prefs,
                logLevelFloor = if (isDebuggable) "info" else XrayConfigBuilder.DEFAULT_LOG_LEVEL
            )
            val dnsEndpoints = dnsEndpointExtra?.let { listOf(it) } ?: settings.dnsEndpoints
            // setDNS expects a plain host:port, without the Xray transport scheme
            // (tcp://, tcp+local://, udp://, https://) carried by the DNS config.
            val bootstrapEndpoint = dnsEndpointExtra ?: settings.bootstrapResolverEndpoint()
            val resolverEndpoint = bootstrapEndpoint
                .substringAfter("://", bootstrapEndpoint)
                .substringBefore('/')
            TunnelStatusRepository.log(
                "Options moteur appliquées : MTU ${settings.mtu} · routage ${settings.routingMode} · " +
                    "DNS ${if (dnsEndpointExtra != null) "override" else settings.dnsEndpoints.first()} · " +
                    "mux ${if (settings.muxEnabled) "on" else "off"} · " +
                    "fragmentation ${if (settings.tlsFragment) "on" else "off"} · " +
                    "adblock ${if (settings.adBlock) "on" else "off"} · " +
                    "hotspot ${if (settings.hotspotSharing) "on" else "off"}",
                LogSeverity.INFO
            )

            // Validate with an inert descriptor first (fd 0 is a valid, harmless
            // file in the app process): testXray only constructs handlers and
            // never reads the TUN. The live VPN descriptor is created afterwards.
            val engineLogLevel = settings.logLevel
            Log.i(TAG, "Engine mode: ${profile.protocol.name} -> ${profile.endpoint} (log=$engineLogLevel)")
            val validation = XrayBridge.testXray(
                XrayConfigBuilder.build(
                    profile = profile,
                    tunFd = 0,
                    mtu = settings.mtu,
                    dnsEndpoints = dnsEndpoints,
                    logLevel = engineLogLevel,
                    injectMode = profile.injectMode,
                    sni = profile.sni,
                    routingMode = settings.routingMode,
                    muxEnabled = settings.muxEnabled,
                    tlsFragment = settings.tlsFragment,
                    adBlock = settings.adBlock,
                    hotspotSharing = settings.hotspotSharing
                )
            )
            if (!validation.success) {
                throw IllegalStateException(
                    "Configuration refusée par Xray : ${XrayErrorSanitizer.sanitize(validation.error)}"
                )
            }

            val descriptor = buildTun(settings.mtu) ?: throw IllegalStateException("Permission VPN refusée par Android")
            tun = descriptor
            Log.i(TAG, "TUN established")
            TunnelStatusRepository.set(TunnelState.Connecting)
            TunnelStatusRepository.log("TUN établi, démarrage du moteur Xray")

            val protectFailures = AtomicInteger(0)
            val protectSuccesses = AtomicInteger(0)
            val protector: (Int) -> Boolean = { fd ->
                val protected = try {
                    protect(fd)
                } catch (t: Throwable) {
                    Log.w(TAG, "VpnService.protect threw ${t.javaClass.simpleName}")
                    false
                }
                if (protected) {
                    if (protectSuccesses.incrementAndGet() == 1) {
                        Log.i(TAG, "VpnService.protect granted for an engine socket")
                    }
                } else if (protectFailures.incrementAndGet() <= 5) {
                    Log.w(TAG, "VpnService.protect returned false for an engine socket")
                }
                protected
            }
            if (!XrayBridge.registerSocketProtector(protector)) {
                throw IllegalStateException("Impossible d'enregistrer la protection des sockets VPN")
            }
            XrayBridge.setDNS(protector, resolverEndpoint)

            val config = XrayConfigBuilder.build(
                profile = profile,
                tunFd = descriptor.fd,
                mtu = settings.mtu,
                dnsEndpoints = dnsEndpoints,
                logLevel = engineLogLevel,
                injectMode = profile.injectMode,
                sni = profile.sni,
                routingMode = settings.routingMode,
                muxEnabled = settings.muxEnabled,
                tlsFragment = settings.tlsFragment,
                adBlock = settings.adBlock,
                hotspotSharing = settings.hotspotSharing
            )

            val started = XrayBridge.runXray(config)
            if (!started.success) {
                throw IllegalStateException(
                    "Démarrage Xray refusé : ${XrayErrorSanitizer.sanitize(started.error)}"
                )
            }
            if (!XrayBridge.getXrayState()) {
                throw IllegalStateException("Le moteur Xray n'est pas actif après démarrage")
            }
            engineRunning = true
            TunnelStatusRepository.log("Moteur Xray démarré (libXray ${XrayBridge.API_VERSION})")

            TunnelStatusRepository.set(TunnelState.Verifying)
            updateNotification("Vérification de la connectivité…")
            val probeUrl = intent.getStringExtra(EXTRA_PROBE_URL) ?: DEFAULT_PROBE_URL
            val probe = awaitConnectivity(
                probeUrl,
                totalTimeoutMs = prefs.connectionTimeoutSec.coerceIn(5, 120) * 1000L
            )
            if (!probe.ok && !ConnectivityProbe.isDnsFailure(probe)) {
                throw IllegalStateException(
                    "Tunnel démarré mais connectivité non vérifiée : ${probe.detail}"
                )
            }
            if (!probe.ok) {
                // DNS-only failure: hostname resolution warms up late through a
                // cold tunnel. Prove the engine forwards traffic with raw IP
                // connects before deciding anything, and never kill a working
                // tunnel over a late resolver.
                val forwarding = ConnectivityProbe.checkIpProbe()
                if (!forwarding.ok) {
                    throw IllegalStateException(
                        "Tunnel démarré mais connectivité non vérifiée : " +
                            "${probe.detail} ; ${forwarding.detail}"
                    )
                }
                dnsPendingNotice = true
                TunnelStatusRepository.log(
                    "Tunnel actif : trafic vérifié (${forwarding.detail}) ; " +
                        "résolution DNS en attente : ${probe.detail}",
                    LogSeverity.WARNING
                )
            } else {
                dnsPendingNotice = false
                TunnelStatusRepository.log("Connectivité réelle vérifiée (${probe.detail})")
            }

            if (destroyed) {
                teardownEngine()
                return@withLock
            }
            connectedSinceEpochMs = System.currentTimeMillis()
            TunnelStatusRepository.set(
                TunnelState.Connected(
                    profileName = profile.displayName,
                    protocol = profile.protocol,
                    sinceEpochMs = connectedSinceEpochMs
                )
            )
            TunnelStatusRepository.log("Connecté avec succès")
            startTrafficSampling()
            startHealthMonitor(
                profile,
                probeUrl,
                autoReconnect = prefs.autoReconnect,
                reconnectDelayMs = prefs.reconnectDelaySec.coerceIn(1, 60) * 1000L
            )
            updateNotification("Connecté — ${profile.displayName}")
        } catch (c: kotlinx.coroutines.CancellationException) {
            teardownEngine()
            throw c
        } catch (t: Throwable) {
            val message = XrayErrorSanitizer.sanitize(t.message ?: t.javaClass.simpleName)
            teardownEngine()
            if (!destroyed) {
                TunnelStatusRepository.error(message)
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private suspend fun disconnect(reason: String) = lifecycle.withLock {
        if (TunnelStatusRepository.state.value is TunnelState.Idle && tun == null && !engineRunning) {
            stopSelf()
            return@withLock
        }
        TunnelStatusRepository.set(TunnelState.Disconnecting)
        TunnelStatusRepository.log(reason)
        teardownEngine()
        TunnelStatusRepository.set(TunnelState.Idle)
        TunnelStatusRepository.log("Tunnel arrêté", LogSeverity.INFO)
        stopForegroundCompat()
        stopSelf()
    }

    /**
     * Waits until Android reports the VPN as the app network, then probes the
     * URL, retrying a few times. The first probe can run before netd applied
     * the VPN link and its DNS servers, which would resolve through the
     * previous network and fail with NXDOMAIN.
     */
    private suspend fun awaitConnectivity(
        url: String,
        totalTimeoutMs: Long = PROBE_TOTAL_TIMEOUT_MS
    ): ConnectivityProbe.Result {
        var attempt = 1
        var result = ConnectivityProbe.Result(false, "not attempted")
        val deadline = SystemClock.elapsedRealtime() + totalTimeoutMs
        while (attempt <= PROBE_ATTEMPTS && !destroyed && SystemClock.elapsedRealtime() < deadline) {
            waitForVpnNetwork()
            result = ConnectivityProbe.check(url, timeoutMs = 6_000)
            Log.i(TAG, "Connectivity probe attempt $attempt/${PROBE_ATTEMPTS}: ok=${result.ok} detail=${result.detail}")
            if (result.ok) return result
            attempt++
            if (attempt <= PROBE_ATTEMPTS && SystemClock.elapsedRealtime() < deadline) {
                TunnelStatusRepository.log("Vérification reportée (${attempt}/${PROBE_ATTEMPTS}) : ${result.detail}")
                delay(PROBE_RETRY_DELAY_MS)
            }
        }
        return result
    }

    /** Blocks until the app's active network is the VPN, or the timeout ends. */
    private suspend fun waitForVpnNetwork() {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val deadline = SystemClock.elapsedRealtime() + VPN_READY_TIMEOUT_MS
        while (!destroyed && SystemClock.elapsedRealtime() < deadline) {
            val network = manager.activeNetwork
            val capabilities = network?.let { manager.getNetworkCapabilities(it) }
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                Log.i(TAG, "VPN network is the active network")
                return
            }
            delay(200)
        }
        Log.w(TAG, "VPN network not active after ${VPN_READY_TIMEOUT_MS}ms")
    }

    private suspend fun resolveProfile(intent: Intent): TunnelProfile {
        TunnelConfigRepository.ensureInitialized(this)
        val link = intent.getStringExtra(EXTRA_SHARE_LINK)
        val profile = if (!link.isNullOrBlank()) {
            TunnelConfigRepository.import(link).getOrElse { failure ->
                throw IllegalStateException(failure.message ?: "Configuration invalide")
            }
        } else {
            TunnelConfigRepository.selectedProfile()
                ?: throw IllegalStateException("Aucune configuration sélectionnée")
        }
        if (profile.incomplete) {
            throw IllegalStateException(
                "Configuration incomplète : réimportez le lien ou le fichier JSON de « ${profile.displayName} »"
            )
        }
        return profile
    }

    /**
     * Real post-connection health watch: every check is an actual request
     * through the tunnel. A failure moves the state to Reconnecting and starts
     * a bounded recovery window; the state only returns to Connected after a
     * probe succeeds again. Nothing reports Connected while the tunnel is not
     * proven.
     */
    /**
     * Hostname probe with an IP-literal fallback: while name resolution is
     * down but the engine forwards traffic, the tunnel is kept up and the
     * detail states exactly what was proven (TCP via tunnel, DNS pending).
     */
    private suspend fun verifyTunnel(probeUrl: String): ConnectivityProbe.Result {
        val probe = ConnectivityProbe.check(probeUrl, timeoutMs = 8_000)
        if (probe.ok) {
            if (dnsPendingNotice) {
                dnsPendingNotice = false
                TunnelStatusRepository.log(
                    "Résolution DNS opérationnelle via le tunnel",
                    LogSeverity.SUCCESS
                )
            }
            return probe
        }
        if (ConnectivityProbe.isDnsFailure(probe)) {
            val forwarding = ConnectivityProbe.checkIpProbe()
            if (forwarding.ok) {
                dnsPendingNotice = true
                return ConnectivityProbe.Result(
                    true,
                    "${forwarding.detail} — DNS en attente : ${probe.detail}"
                )
            }
        }
        return probe
    }

    private fun startHealthMonitor(
        profile: TunnelProfile,
        probeUrl: String,
        autoReconnect: Boolean,
        reconnectDelayMs: Long
    ) {
        healthJob?.cancel()
        healthJob = scope.launch {
            while (isActive && !destroyed) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                if (destroyed) break
                if (TunnelStatusRepository.state.value !is TunnelState.Connected) continue
                val probe = verifyTunnel(probeUrl)
                if (probe.ok) {
                    Log.i(TAG, "Health probe ok: ${probe.detail}")
                    continue
                }
                if (destroyed) break
                TunnelStatusRepository.log(
                    "Vérification périodique échouée : ${probe.detail}",
                    LogSeverity.WARNING
                )
                if (!autoReconnect) {
                    TunnelStatusRepository.error(
                        "Tunnel perdu : reconnexion automatique désactivée"
                    )
                    lifecycle.withLock { teardownEngine() }
                    stopForegroundCompat()
                    stopSelf()
                    break
                }
                awaitRecovery(profile, probeUrl, reconnectDelayMs)
                if (TunnelStatusRepository.state.value is TunnelState.Error) break
            }
        }
    }

    private suspend fun awaitRecovery(
        profile: TunnelProfile,
        probeUrl: String,
        reconnectDelayMs: Long
    ) {
        val deadline = SystemClock.elapsedRealtime() + RECONNECT_TIMEOUT_MS
        var attempt = 1
        TunnelStatusRepository.set(TunnelState.Reconnecting(attempt, profile.displayName))
        updateNotification("Réseau perdu — reconnexion…")
        while (!destroyed && SystemClock.elapsedRealtime() < deadline) {
            delay(reconnectDelayMs)
            if (destroyed) return
            val probe = verifyTunnel(probeUrl)
            if (probe.ok) {
                val since = connectedSinceEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                TunnelStatusRepository.set(
                    TunnelState.Connected(profile.displayName, profile.protocol, since)
                )
                TunnelStatusRepository.log(
                    "Réseau rétabli — tunnel de nouveau vérifié (${probe.detail})",
                    LogSeverity.SUCCESS
                )
                updateNotification("Connecté — ${profile.displayName}")
                return
            }
            attempt++
            if (attempt % 3 == 0) {
                TunnelStatusRepository.log(
                    "Reconnexion ${attempt} non aboutie : ${probe.detail}",
                    LogSeverity.WARNING
                )
            }
            if (!destroyed) TunnelStatusRepository.set(TunnelState.Reconnecting(attempt, profile.displayName))
        }
        if (destroyed) return
        TunnelStatusRepository.error(
            "Réseau indisponible : tunnel arrêté après ${RECONNECT_TIMEOUT_MS / 1_000} s de reconnexion"
        )
        lifecycle.withLock { teardownEngine() }
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopHealthMonitor() {
        healthJob?.cancel()
        healthJob = null
        connectedSinceEpochMs = 0L
    }

    private val isDebuggable: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun buildTun(mtu: Int): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("ONFAA TUNNEL")
            .setMtu(mtu)
            .addAddress("10.10.0.2", 24)
            .addDnsServer("10.10.0.1")
            .addRoute("0.0.0.0", 0)
            .addAddress("fd00::2", 64)
            .addDnsServer("fd00::1")
            .addRoute("::", 0)
        return try {
            builder.establish()
        } catch (failure: Throwable) {
            throw IllegalStateException("Permission VPN refusée par Android", failure)
        }
    }

    /**
     * Samples android.net.TrafficStats for the app UID. Xray's proxy sockets
     * belong to this process, so the deltas are real session traffic. The
     * platform can return UNSUPPORTED (-1): in that case the UI is told the
     * counters are unavailable rather than displaying invented values.
     */
    private fun startTrafficSampling() {
        trafficJob?.cancel()
        val uid = Process.myUid()
        val baseRx = TrafficStats.getUidRxBytes(uid)
        val baseTx = TrafficStats.getUidTxBytes(uid)
        if (baseRx < 0L || baseTx < 0L) {
            TrafficStatsRepository.markUnavailable()
            TunnelStatusRepository.log(
                "Compteurs de trafic indisponibles sur cet appareil",
                LogSeverity.WARNING
            )
            return
        }
        TrafficStatsRepository.start(System.currentTimeMillis())
        trafficJob = scope.launch {
            var lastRx = baseRx
            var lastTx = baseTx
            var lastElapsed = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1_000L)
                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)
                if (rx < 0L || tx < 0L) {
                    TrafficStatsRepository.markUnavailable()
                    break
                }
                val now = SystemClock.elapsedRealtime()
                val elapsed = (now - lastElapsed).coerceAtLeast(1L)
                val downRate = ((rx - lastRx).coerceAtLeast(0L) * 1_000L) / elapsed
                val upRate = ((tx - lastTx).coerceAtLeast(0L) * 1_000L) / elapsed
                TrafficStatsRepository.update(
                    downBytes = rx - baseRx,
                    upBytes = tx - baseTx,
                    downBytesPerSec = downRate,
                    upBytesPerSec = upRate,
                    sampleEpochMs = System.currentTimeMillis()
                )
                lastRx = rx
                lastTx = tx
                lastElapsed = now
            }
        }
    }

    private fun stopTrafficSampling() {
        trafficJob?.cancel()
        trafficJob = null
        TrafficStatsRepository.stop()
    }

    private suspend fun teardownEngine() = withContext(NonCancellable) {
        stopHealthMonitor()
        stopTrafficSampling()
        runCatching { XrayBridge.stopXray() }
            .onFailure { TunnelStatusRepository.log("Arrêt Xray : ${XrayErrorSanitizer.sanitize(it.message)}") }
        runCatching { XrayBridge.resetDNS() }
            .onFailure { TunnelStatusRepository.log("Reset DNS : ${XrayErrorSanitizer.sanitize(it.message)}") }
        XrayBridge.clearSocketProtector()
        engineRunning = false
        tun?.let { descriptor ->
            runCatching { descriptor.close() }
        }
        tun = null
    }

    override fun onRevoke() {
        scope.launch { disconnect("VPN révoqué par Android") }
        super.onRevoke()
    }

    override fun onDestroy() {
        destroyed = true
        scope.cancel()
        runBlocking(Dispatchers.IO) { teardownEngine() }
        if (TunnelStatusRepository.state.value !is TunnelState.Error) {
            TunnelStatusRepository.set(TunnelState.Idle)
        }
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tunnel ONFAA",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "État du tunnel Xray"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun showForeground(text: String) {
        startForeground(NOTIFICATION_ID, buildNotification(text))
    }

    private fun updateNotification(text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(text))
        }
    }

    private fun buildNotification(text: String): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disconnect = PendingIntent.getService(
            this,
            1,
            Intent(this, OnfaaTunnelVpnService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ONFAA TUNNEL")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Déconnecter", disconnect)
            .build()
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
