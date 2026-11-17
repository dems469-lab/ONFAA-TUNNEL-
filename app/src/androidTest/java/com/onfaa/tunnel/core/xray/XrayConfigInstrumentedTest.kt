package com.onfaa.tunnel.core.xray

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-artifact tests: these run only when the official libXray AAR is present
 * (CI PoC workflow). They exercise the actual gomobile native API, not fakes.
 */
@RunWith(AndroidJUnit4::class)
class XrayConfigInstrumentedTest {

    private val uuid = "00000000-0000-4000-8000-000000000001"

    // Xray refuses a VLESS outbound without TLS/encryption when the server is a
    // public address ("vless without TLS or other encryption is prohibited
    // unless the server address is a private IP or domain"), so the plain-VLESS
    // fixture uses a LAN address. Public VLESS is covered by the TLS test.
    private val vlessLink =
        "vless://$uuid@192.168.1.10:443?encryption=none&security=none&type=tcp#Test%20VLESS"
    private val vmessLink =
        "vmess://$uuid@192.0.2.1:443?encryption=auto&security=none&type=tcp#Test%20VMess"

    @Before
    fun ensureStopped() {
        runBlocking {
            runCatching { XrayBridge.stopXray() }
            runCatching { XrayBridge.resetDNS() }
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            runCatching { XrayBridge.stopXray() }
            runCatching { XrayBridge.resetDNS() }
        }
    }

    @Test
    fun realEngineIsAvailable() {
        assertTrue(XrayBridge.isAvailable())
        assertTrue(XrayBridge.loadNativeLibrary())
        assertTrue(XrayBridge.resolvedSignature()!!.contains("invoke"))
    }

    @Test
    fun convertsVlessShareLinkWithEngine() = runBlocking {
        val response = XrayBridge.convertShareLink(vlessLink)
        assertTrue(response.error, response.success)
        val outbounds = response.data!!.getJSONArray("outbounds")
        assertEquals(1, outbounds.length())
        val outbound = outbounds.getJSONObject(0)
        assertEquals("vless", outbound.getString("protocol"))
        val settings = outbound.getJSONObject("settings")
        assertEquals("192.168.1.10", settings.getString("address"))
        assertEquals(443, settings.getInt("port"))
        assertEquals("none", settings.getString("encryption"))
    }

    /**
     * A public VLESS endpoint over TLS must import through the real engine: the
     * plain-VLESS fixture above is only accepted because LAN addresses are
     * exempt from Xray's "no VLESS without TLS" rule.
     */
    @Test
    fun convertsPublicVlessTlsShareLinkWithEngine() = runBlocking {
        val response = XrayBridge.convertShareLink(
            "vless://$uuid@vless.example.com:443?encryption=none&security=tls&sni=vless.example.com&type=tcp#TLS"
        )
        assertTrue(response.error, response.success)
        val outbound = response.data!!.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("vless", outbound.getString("protocol"))
        assertEquals("tls", outbound.getJSONObject("streamSettings").getString("security"))
        assertEquals("vless.example.com", outbound.getJSONObject("settings").getString("address"))
    }

    @Test
    fun convertsVmessShareLinkWithEngine() = runBlocking {
        val response = XrayBridge.convertShareLink(vmessLink)
        assertTrue(response.error, response.success)
        val outbounds = response.data!!.getJSONArray("outbounds")
        assertEquals(1, outbounds.length())
        val outbound = outbounds.getJSONObject(0)
        assertEquals("vmess", outbound.getString("protocol"))
        assertEquals("192.0.2.1", outbound.getJSONObject("settings").getString("address"))
    }

    @Test
    fun fullTunConfigIsAcceptedByRealEngine() = runBlocking {
        val profile = ConfigImporter.importLink(vlessLink).getOrThrow()
        val config = XrayConfigBuilder.build(profile, tunFd = 0)
        val validation = XrayBridge.testXray(config)
        assertTrue(validation.error, validation.success)

        val json = JSONObject(config)
        assertEquals("0", json.getJSONObject("env").getString("xray.tun.fd"))
        val inbound = json.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("tun", inbound.getString("protocol"))
        assertEquals(XrayConfigBuilder.TUN_INBOUND_TAG, inbound.getString("tag"))
        val outbounds = json.getJSONArray("outbounds")
        assertEquals(XrayConfigBuilder.PROXY_OUTBOUND_TAG, outbounds.getJSONObject(0).getString("tag"))
        assertEquals("dns", outbounds.getJSONObject(1).getString("protocol"))
        assertEquals(1500, inbound.getJSONObject("settings").getInt("mtu"))
    }

    @Test
    fun invalidShareLinkIsRejectedWithoutCrash() = runBlocking {
        val result = ConfigImporter.importLink("vless://not-a-valid-link")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ConfigException)
    }

    @Test
    fun runXrayWithInvalidFdFailsAndLeavesEngineStopped() = runBlocking {
        val profile = ConfigImporter.importLink(vlessLink).getOrThrow()
        val config = XrayConfigBuilder.build(profile, tunFd = -1)
        val before = XrayBridge.getXrayState()
        assertFalse(before)
        val response = XrayBridge.runXray(config)
        assertFalse("runXray must fail with an invalid TUN fd", response.success)
        assertFalse(XrayBridge.getXrayState())
    }

    @Test
    fun stopXrayIsIdempotent() = runBlocking {
        assertTrue(XrayBridge.stopXray().success)
        assertTrue(XrayBridge.stopXray().success)
        assertFalse(XrayBridge.getXrayState())
    }

    @Test
    fun socketProtectorAndDnsControllersAreCallable() {
        // Xray-core's controller list is append-only and every dial must satisfy
        // all registered controllers, so this process-wide registration must
        // always return true: a `false` controller would break the real E2E
        // dials that run later in this same instrumentation process.
        assertTrue(XrayBridge.registerSocketProtector { true })
        XrayBridge.setDNS({ true }, "1.1.1.1:53")
        XrayBridge.resetDNS()
        XrayBridge.clearSocketProtector()
    }
}
