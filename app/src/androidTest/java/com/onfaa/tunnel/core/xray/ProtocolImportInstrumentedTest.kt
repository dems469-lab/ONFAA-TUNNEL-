package com.onfaa.tunnel.core.xray

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onfaa.tunnel.domain.model.TunnelProtocol
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Conversion and engine validation of the protocols added in Phase 5 (lot 1)
 * against the real libXray AAR. Skipped when the AAR is not bundled.
 *
 * These tests only prove that a link/JSON is converted and accepted by the
 * real engine; the network path of Trojan and Shadowsocks is proven by
 * [com.onfaa.tunnel.vpn.VpnTunnelE2ETest]. No credential is ever logged.
 */
@RunWith(AndroidJUnit4::class)
class ProtocolImportInstrumentedTest {

    private fun requireLib() {
        assumeTrue(
            "libXray AAR not bundled (app/libs). Skipping real-artifact test.",
            XrayBridge.isAvailable()
        )
    }

    private fun base64(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    @Test
    fun importsTrojanLinkAndAcceptsConvertedConfiguration() = runBlocking {
        requireLib()
        val link = "trojan://onfaa-trojan-pass@example.com:443" +
            "?security=tls&sni=example.com&type=tcp#CI%20Trojan"
        val profile = ConfigImporter.importText(link).getOrThrow()
        assertEquals(TunnelProtocol.TROJAN, profile.protocol)
        assertEquals("example.com:443", profile.endpoint)
        assertFalse(profile.incomplete)
        assertTrue(profile.outboundJson.contains("\"trojan\""))
    }

    @Test
    fun importsShadowsocksLinkAndAcceptsConvertedConfiguration() = runBlocking {
        requireLib()
        val userInfo = base64("aes-256-gcm:onfaa-ss-pass")
        val link = "ss://$userInfo@example.com:8388#CI%20Shadowsocks"
        val profile = ConfigImporter.importText(link).getOrThrow()
        assertEquals(TunnelProtocol.SHADOWSOCKS, profile.protocol)
        assertEquals("example.com:8388", profile.endpoint)
        assertFalse(profile.incomplete)
        assertTrue(profile.outboundJson.contains("\"shadowsocks\""))
    }

    @Test
    fun importsSocksLinkAndAcceptsConvertedConfiguration() = runBlocking {
        requireLib()
        val userInfo = base64("onfaa-user:onfaa-pass")
        val link = "socks://$userInfo@example.com:1080#CI%20SOCKS"
        val profile = ConfigImporter.importText(link).getOrThrow()
        assertEquals(TunnelProtocol.SOCKS, profile.protocol)
        assertEquals("example.com:1080", profile.endpoint)
        assertFalse(profile.incomplete)
        assertTrue(profile.outboundJson.contains("\"socks\""))
    }

    @Test
    fun importsShadowsocksJsonWithServersArray() = runBlocking {
        requireLib()
        val json = """
            {"protocol":"shadowsocks","settings":{"servers":[
              {"address":"example.com","port":8388,"method":"aes-256-gcm","password":"onfaa-ss-pass"}
            ]}}
        """.trimIndent()
        val profile = ConfigImporter.importText(json).getOrThrow()
        assertEquals(TunnelProtocol.SHADOWSOCKS, profile.protocol)
        assertEquals("example.com:8388", profile.endpoint)
    }

    @Test
    fun importsSocksJsonWithoutAuthentication() = runBlocking {
        requireLib()
        val json = """{"protocol":"socks","settings":{"address":"example.com","port":1080}}"""
        val profile = ConfigImporter.importText(json).getOrThrow()
        assertEquals(TunnelProtocol.SOCKS, profile.protocol)
        assertEquals("example.com:1080", profile.endpoint)
    }
}
