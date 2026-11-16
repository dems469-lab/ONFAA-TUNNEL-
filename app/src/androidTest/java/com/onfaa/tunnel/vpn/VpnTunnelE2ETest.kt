package com.onfaa.tunnel.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onfaa.tunnel.MainActivity
import com.onfaa.tunnel.core.xray.XrayBridge
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
 *   onfaa.e2e.dns, onfaa.e2e.probeUrl, onfaa.e2e.token
 *
 * A TEST-NET address (192.0.2.10) is routed by the local server to a token
 * HTTP server bound on the host loopback alias. It is unreachable directly
 * from the emulator, so a successful request proves the packet traversed the
 * real Xray client -> VLESS/VMess server path.
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
        // the local Xray server (host loopback alias).
        val byName = httpGet(probeUrl, 15_000)
        assertEquals(200, byName.first)
        assertEquals(token, byName.second.trim())
        val byIp = httpGet("http://192.0.2.10:18080/token", 15_000)
        assertEquals(200, byIp.first)
        assertEquals(token, byIp.second.trim())

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
