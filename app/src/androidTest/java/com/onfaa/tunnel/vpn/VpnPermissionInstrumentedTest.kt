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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Android VPN permission behaviour. Uses shell appops to simulate the
 * user grant/deny without automating the system dialog.
 */
@RunWith(AndroidJUnit4::class)
class VpnPermissionInstrumentedTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        runBlocking { runCatching { XrayBridge.stopXray() } }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        shell("appops set ${context.packageName} ACTIVATE_VPN allow")
        runCatching {
            context.startService(
                Intent(context, OnfaaTunnelVpnService::class.java)
                    .setAction(OnfaaTunnelVpnService.ACTION_DISCONNECT)
            )
        }
        await(10_000) { TunnelStatusRepository.state.value is TunnelState.Idle }
        runBlocking { runCatching { XrayBridge.stopXray() } }
        scenario?.close()
        scenario = null
    }

    @Test
    fun prepareRequiresConsentWhenAppOpIsNotAllowed() {
        shell("appops set ${context.packageName} ACTIVATE_VPN ignore")
        assertNotNull("Android must request VPN consent", VpnService.prepare(context))
        shell("appops set ${context.packageName} ACTIVATE_VPN allow")
        await(15_000) { VpnService.prepare(context) == null }
        assertNull(VpnService.prepare(context))
    }

    @Test
    fun connectWithoutConsentReportsErrorAndLeavesEngineStopped() {
        shell("appops set ${context.packageName} ACTIVATE_VPN ignore")
        val link = "vless://00000000-0000-4000-8000-000000000001@192.0.2.1:443" +
            "?encryption=none&security=none&type=tcp#NoConsent"
        val intent = Intent(context, OnfaaTunnelVpnService::class.java)
            .setAction(OnfaaTunnelVpnService.ACTION_CONNECT)
            .putExtra(OnfaaTunnelVpnService.EXTRA_SHARE_LINK, link)
            .putExtra(OnfaaTunnelVpnService.EXTRA_PROBE_URL, "http://onfaa.test:18080/token")
        ContextCompat.startForegroundService(context, intent)

        if (!await(30_000) { TunnelStatusRepository.state.value is TunnelState.Error }) {
            fail("Expected a VPN permission error, got ${TunnelStatusRepository.state.value}")
        }
        val error = TunnelStatusRepository.state.value as TunnelState.Error
        assertTrue(error.message, error.message.contains("Permission VPN", ignoreCase = true))
        assertFalse(runBlocking { XrayBridge.getXrayState() })
    }

    private fun shell(command: String) {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    private fun await(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (predicate()) return true
            Thread.sleep(200)
        }
        return predicate()
    }
}
