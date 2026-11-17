package com.onfaa.tunnel.core.xray

import com.onfaa.tunnel.domain.model.InjectMode
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.domain.model.TunnelProtocol
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the Xray outbound configuration builder (no native
 * engine required): transport and inject-mode wiring, SNI injection and the
 * rejections that must stay impossible at the engine boundary.
 */
class XrayConfigBuilderTest {

    private fun profile(outboundJson: String, endpoint: String = "127.0.0.1:18445"): TunnelProfile =
        TunnelProfile(
            id = "test-profile",
            displayName = "Fixture",
            protocol = TunnelProtocol.TROJAN,
            endpoint = endpoint,
            security = "tls",
            outboundJson = outboundJson
        )

    private fun outboundOf(configJson: String): JSONObject =
        JSONObject(configJson).getJSONArray("outbounds").getJSONObject(0)

    @Test
    fun `injects SNI and overrides the transport from the inject mode`() {
        val config = XrayConfigBuilder.build(
            profile(
                outboundJson = """
                    {"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"},
                     "streamSettings":{"security":"tls","network":"tcp","sni":"onfaa.test"}}
                """.trimIndent()
            ),
            tunFd = 0,
            injectMode = InjectMode.PROXY_SNI,
            sni = "onfaa.test",
            transport = "ws"
        )
        val outbound = outboundOf(config)
        assertEquals("ws", outbound.getJSONObject("streamSettings").getString("network"))
        assertEquals("onfaa.test", outbound.getJSONObject("streamSettings").getString("sni"))
    }

    @Test
    fun `proxy with SNI falls back to the server address when no SNI is set`() {
        val config = XrayConfigBuilder.build(
            profile(
                outboundJson = """
                    {"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"},
                     "streamSettings":{"security":"tls","network":"tcp"}}
                """.trimIndent()
            ),
            tunFd = 0,
            injectMode = InjectMode.PROXY_SNI
        )
        val outbound = outboundOf(config)
        assertEquals("127.0.0.1", outbound.getJSONObject("streamSettings").getString("sni"))
    }

    @Test
    fun `proxy mode keeps the outbound untouched when no SNI is required`() {
        val config = XrayConfigBuilder.build(
            profile(
                outboundJson = """
                    {"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"},
                     "streamSettings":{"security":"tls","network":"tcp"}}
                """.trimIndent()
            ),
            tunFd = 0,
            injectMode = InjectMode.PROXY
        )
        val outbound = outboundOf(config)
        assertEquals("tcp", outbound.getJSONObject("streamSettings").getString("network"))
        assertEquals("tls", outbound.getJSONObject("streamSettings").getString("security"))
    }

    @Test
    fun `dns tunnel inject mode is rejected by the xray engine`() {
        val error = runCatching {
            XrayConfigBuilder.build(
                profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
                tunFd = 0,
                injectMode = InjectMode.DNSTT
            )
        }.exceptionOrNull()
        assertTrue("expected a rejection for DNSTT, got $error", error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty(), error?.message.orEmpty().contains("DNSTT", ignoreCase = true))
    }

    @Test
    fun `unsupported transport network is rejected by the xray engine`() {
        val error = runCatching {
            XrayConfigBuilder.build(
                profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
                tunFd = 0,
                transport = "hysteria2"
            )
        }.exceptionOrNull()
        assertTrue("expected a rejection for a non-Xray transport, got $error", error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty(), error?.message.orEmpty().contains("Transport réseau", ignoreCase = true))
    }
}
