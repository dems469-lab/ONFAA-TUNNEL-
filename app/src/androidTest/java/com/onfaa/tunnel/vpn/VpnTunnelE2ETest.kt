package com.onfaa.tunnel.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onfaa.tunnel.MainActivity
import com.onfaa.tunnel.core.vpn.AppFilter
import com.onfaa.tunnel.core.xray.XrayBridge
import com.onfaa.tunnel.onfaaAppContainer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * Real end-to-end validation against an official Xray-core server built from
 * the pinned commit on the CI host. Requires instrumentation arguments:
 *   onfaa.e2e=true
 *   onfaa.e2e.uuid, onfaa.e2e.host, onfaa.e2e.vlessPort, onfaa.e2e.vmessPort
 *   onfaa.e2e.trojanPort, onfaa.e2e.trojanPassword, onfaa.e2e.trojanPcs
 *   onfaa.e2e.ssPort, onfaa.e2e.ssMethod, onfaa.e2e.ssPassword
 *   onfaa.e2e.dns, onfaa.e2e.probeUrl, onfaa.e2e.token
 *
 * A TEST-NET address (192.0.2.10) is routed by the local server to a token
 * HTTP server bound on the host loopback alias. It is unreachable directly
 * from the emulator, so a successful request proves the packet traversed the
 * real Xray client -> server path for each protocol.
 */
@RunWith(AndroidJUnit4::class)
class VpnTunnelE2ETest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val args get() = InstrumentationRegistry.getArguments()

    private val e2eEnabled get() = args.getString("onfaa.e2e") == "true"
    private val uuid get() = args.getString("onfaa.e2e.uuid").orEmpty()
    private val host get() = args.getString("onfaa.e2e.host") ?: "10.0.2.2"
    private val vlessPort get() = args.getString("onfaa.e2e.vlessPort") ?: "18443"
    private val vmessPort get() = args.getString("onfaa.e2e.vmessPort") ?: "18444"
    private val trojanPort get() = args.getString("onfaa.e2e.trojanPort") ?: "18445"
    private val trojanPassword get() = args.getString("onfaa.e2e.trojanPassword").orEmpty()
    private val trojanPcs get() = args.getString("onfaa.e2e.trojanPcs").orEmpty()
    private val ssPort get() = args.getString("onfaa.e2e.ssPort") ?: "18446"
    private val ssMethod get() = args.getString("onfaa.e2e.ssMethod") ?: "aes-256-gcm"
    private val ssPassword get() = args.getString("onfaa.e2e.ssPassword").orEmpty()
    private val dns get() = args.getString("onfaa.e2e.dns") ?: "tcp://10.0.2.2:15353"
    private val probeUrl get() = args.getString("onfaa.e2e.probeUrl") ?: "http://onfaa.test:18080/token"
    private val token get() = args.getString("onfaa.e2e.token") ?: ""

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        assumeTrue("E2E infrastructure arguments are required", e2eEnabled)
        assertTrue("onfaa.e2e.uuid is required", uuid.isNotBlank())
        runBlocking { runCatching { XrayBridge.stopXray() } }
        shell("settings put global private_dns_mode off")
        shell("appops set ${context.packageName} ACTIVATE_VPN allow")
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitCondition(15_000, "VPN consent not available through appops") {
            VpnService.prepare(context) == null
        }
        assertNull(VpnService.prepare(context))
    }

    @After
    fun tearDown() {
        if (e2eEnabled) {
            runCatching { sendAction(OnfaaTunnelVpnService.ACTION_DISCONNECT) }
            awaitConditionOrNull(15_000) { TunnelStatusRepository.state.value is TunnelState.Idle || TunnelStatusRepository.state.value is TunnelState.Error }
            runBlocking { runCatching { XrayBridge.stopXray() } }
        }
        scenario?.close()
        scenario = null
    }

    /**
     * Harness sanity check, independent of the VPN: the emulator must reach
     * host-published ports through its 10.0.2.2 host-loopback alias. If this
     * fails, the E2E fixture itself is unreachable and the tunnel results
     * cannot be interpreted.
     */
    @Test
    fun emulatorReachesHostFixtureThroughHostAlias() {
        val direct = httpGet(tokenUrlOnHost(), 5_000)
        assertEquals("host loopback alias unreachable from the emulator", 200, direct.first)
        assertEquals(token, direct.second.trim())
    }

    @Test
    fun vlessTunnelForwardsTrafficResolvesDnsAndStopsCleanly() {
        val link = vlessLink()
        assertDirectTrafficFails()
        runTunnelScenario(link)
    }

    @Test
    fun vmessTunnelForwardsTrafficAndStopsCleanly() {
        val link = vmessLink()
        assertDirectTrafficFails()
        runTunnelScenario(link)
    }

    @Test
    fun trojanTunnelForwardsTrafficAndStopsCleanly() {
        val link = trojanLink()
        assertDirectTrafficFails()
        runTunnelScenario(link)
    }

    @Test
    fun shadowsocksTunnelForwardsTrafficAndStopsCleanly() {
        val link = ssLink()
        assertDirectTrafficFails()
        runTunnelScenario(link)
    }

    /**
     * Real per-application split tunneling, allowed side: the test app is the
     * only package allowed into the VPN. If the filter were not applied by the
     * platform builder, nothing would change; if it were applied wrongly
     * (excluding the app), its own sockets would bypass the tunnel and the
     * fixture, which is only reachable through it, would be unreachable.
     */
    @Test
    fun allowOnlySelfTunnelStillForwardsRealTraffic() {
        setAppFilter(AppFilter.MODE_ALLOW, listOf(context.packageName))
        try {
            runTunnelScenario(vlessLink())
        } finally {
            setAppFilter(AppFilter.MODE_OFF, emptyList())
        }
    }

    /**
     * Real per-application split tunneling, excluded side: the test app is
     * excluded from the VPN. The tunnel itself still connects, but this app's
     * sockets bypass it, so the fixture (reachable only through the tunnel)
     * must become unreachable from the test process — the observable,
     * honest proof that addDisallowedApplication was really applied.
     */
    @Test
    fun blockSelfBypassesTheTunnelForThisApp() {
        setAppFilter(AppFilter.MODE_BLOCK, listOf(context.packageName))
        try {
            sendConnect(vlessLink())
            awaitCondition(90_000, "tunnel did not connect") {
                TunnelStatusRepository.state.value is TunnelState.Connected
            }
            val bypassed = runCatching { httpGet(probeUrl, 3_000) }
            assertTrue("excluded app reached the fixture directly: $bypassed", bypassed.isFailure)
            sendAction(OnfaaTunnelVpnService.ACTION_DISCONNECT)
            awaitCondition(20_000, "tunnel did not stop") {
                TunnelStatusRepository.state.value is TunnelState.Idle
            }
        } finally {
            setAppFilter(AppFilter.MODE_OFF, emptyList())
        }
    }

    private fun setAppFilter(mode: String, packages: List<String>) {
        runBlocking {
            val repository = context.onfaaAppContainer.userPreferencesRepository
            repository.setAppFilterMode(mode)
            repository.setAppFilterPackages(AppFilter.serializePackages(packages))
        }
    }

    @Test
    fun unreachableServerNeverReportsConnected() {
        val link = "vless://$uuid@$host:9?encryption=none&security=none&type=tcp#Unreachable"
        sendConnect(link)
        val state = awaitCondition(60_000, "service must report an error") {
            TunnelStatusRepository.state.value is TunnelState.Error
        }
        val error = TunnelStatusRepository.state.value as TunnelState.Error
        assertTrue(error.message.isNotBlank())
        assertFalse(runBlocking { XrayBridge.getXrayState() })
        awaitConditionOrNull(10_000) { TunnelStatusRepository.state.value !is TunnelState.Connecting }
    }

    private fun runTunnelScenario(link: String) {
        sendConnect(link)
        awaitCondition(90_000, "tunnel did not connect") {
            TunnelStatusRepository.state.value is TunnelState.Connected
        }
        val connected = TunnelStatusRepository.state.value as TunnelState.Connected
        assertTrue(connected.profileName.isNotBlank())
        assertTrue(runBlocking { XrayBridge.getXrayState() })

        // DNS through the VPN: onfaa.test is only served by the host DNS responder.
        val resolved = InetAddress.getByName("onfaa.test")
        assertEquals("192.0.2.10", resolved.hostAddress)

        // Routing through the tunnel: this TEST-NET address only exists behind
        // the local Xray server (host loopback alias). The IP probe does not
        // depend on DNS, the hostname probe exercises the resolver end-to-end.
        val byIp = httpGet(probeUrl, 15_000)
        assertEquals(200, byIp.first)
        assertEquals(token, byIp.second.trim())
        val byName = httpGet(dnsProbeUrl(), 15_000)
        assertEquals(200, byName.first)
        assertEquals(token, byName.second.trim())

        sendAction(OnfaaTunnelVpnService.ACTION_DISCONNECT)
        awaitCondition(20_000, "tunnel did not stop") {
            TunnelStatusRepository.state.value is TunnelState.Idle
        }
        assertFalse(runBlocking { XrayBridge.getXrayState() })
        assertDirectTrafficFails()
    }

    private fun assertDirectTrafficFails() {
        val direct = runCatching { httpGet(probeUrl, 2_500) }
        assertTrue("direct request unexpectedly succeeded: $direct", direct.isFailure)
    }

    private fun sendConnect(link: String) {
        val intent = Intent(context, OnfaaTunnelVpnService::class.java)
            .setAction(OnfaaTunnelVpnService.ACTION_CONNECT)
            .putExtra(OnfaaTunnelVpnService.EXTRA_SHARE_LINK, link)
            .putExtra(OnfaaTunnelVpnService.EXTRA_DNS_SERVER, dns)
            .putExtra(OnfaaTunnelVpnService.EXTRA_PROBE_URL, probeUrl)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun sendAction(action: String) {
        context.startService(Intent(context, OnfaaTunnelVpnService::class.java).setAction(action))
    }

    private fun vlessLink(): String =
        "vless://$uuid@$host:$vlessPort?encryption=none&security=none&type=tcp#CI%20VLESS"

    private fun vmessLink(): String =
        "vmess://$uuid@$host:$vmessPort?encryption=auto&security=none&type=tcp#CI%20VMess"

    /**
     * Trojan links always negotiate TLS (libXray forces it for the scheme).
     * The fixture certificate is self-signed and Xray 26 removed
     * allowInsecure, so the link pins the certificate SHA-256 (hex) through the
     * pcs parameter, exactly as a real share link would.
     */
    private fun trojanLink(): String =
        "trojan://$trojanPassword@$host:$trojanPort" +
            "?security=tls&sni=onfaa.test&pcs=$trojanPcs&type=tcp#CI%20Trojan"

    /**
     * SIP002 Shadowsocks link: URL-safe base64 userinfo carrying
     * method:password, decoded by libXray's real share-link converter.
     */
    private fun ssLink(): String {
        val userInfo = Base64.encodeToString(
            "$ssMethod:$ssPassword".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
        return "ss://$userInfo@$host:$ssPort#CI%20Shadowsocks"
    }

    /** Token endpoint addressed by DNS name, as production users would. */
    private fun dnsProbeUrl(): String {
        val probe = URL(probeUrl)
        val port = if (probe.port > 0) probe.port else 80
        return "http://onfaa.test:$port${probe.path}"
    }

    /** Same token endpoint as [probeUrl] but addressed to the host alias. */
    private fun tokenUrlOnHost(): String {
        val probe = URL(probeUrl)
        val port = if (probe.port > 0) probe.port else 80
        return "http://$host:$port${probe.path}"
    }

    private fun httpGet(url: String, timeoutMs: Int): Pair<Int, String> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        return try {
            val code = connection.responseCode
            val body = connection.inputStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            code to body
        } finally {
            connection.disconnect()
        }
    }

    private fun shell(command: String) {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    private fun awaitCondition(
        timeoutMs: Long,
        message: String,
        predicate: () -> Boolean
    ): Unit {
        if (!awaitConditionOrNull(timeoutMs, predicate)) {
            fail("$message (last state: ${TunnelStatusRepository.state.value})")
        }
    }

    private fun awaitConditionOrNull(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (predicate()) return true
            Thread.sleep(250)
        }
        return predicate()
    }
}
