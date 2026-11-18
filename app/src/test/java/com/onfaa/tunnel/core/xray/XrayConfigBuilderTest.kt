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

    @Test
    fun `mux enabled writes real streamSettings mux on the proxy outbound`() {
        val config = XrayConfigBuilder.build(
            profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
            tunFd = 0,
            muxEnabled = true
        )
        val mux = outboundOf(config).getJSONObject("streamSettings").getJSONObject("mux")
        assertTrue(mux.getBoolean("enabled"))
        assertEquals(8, mux.getInt("concurrency"))
    }

    @Test
    fun `mux disabled keeps the outbound free of mux settings`() {
        val config = XrayConfigBuilder.build(
            profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
            tunFd = 0
        )
        val stream = outboundOf(config).optJSONObject("streamSettings")
        assertTrue("no streamSettings expected by default", stream == null || !stream.has("mux"))
    }

    @Test
    fun `tls fragment adds a freedom fragment outbound and dials the proxy through it`() {
        val config = XrayConfigBuilder.build(
            profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
            tunFd = 0,
            tlsFragment = true
        )
        val root = JSONObject(config)
        assertEquals(
            "trojan",
            outboundOf(config).optString("protocol")
        )
        assertEquals(XrayConfigBuilder.FRAGMENT_OUTBOUND_TAG, outboundOf(config).getString("dialerProxy"))
        val fragment = root.getJSONArray("outbounds").let { outbounds ->
            (0 until outbounds.length())
                .map { outbounds.getJSONObject(it) }
                .first { it.getString("tag") == XrayConfigBuilder.FRAGMENT_OUTBOUND_TAG }
        }
        val fragmentSettings = fragment.getJSONObject("settings").getJSONObject("fragment")
        assertEquals("tlshello", fragmentSettings.getString("packets"))
        assertTrue(fragmentSettings.has("length"))
        assertTrue(fragmentSettings.has("interval"))
    }

    @Test
    fun `ad block routes the embedded ad domains to a blackhole outbound`() {
        val config = XrayConfigBuilder.build(
            profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
            tunFd = 0,
            adBlock = true
        )
        val root = JSONObject(config)
        val outbounds = root.getJSONArray("outbounds")
        val blackhole = (0 until outbounds.length())
            .map { outbounds.getJSONObject(it) }
            .first { it.getString("tag") == XrayConfigBuilder.BLOCKED_OUTBOUND_TAG }
        assertEquals("blackhole", blackhole.getString("protocol"))
        val firstRule = root.getJSONObject("routing").getJSONArray("rules").getJSONObject(0)
        assertEquals(XrayConfigBuilder.BLOCKED_OUTBOUND_TAG, firstRule.getString("outboundTag"))
        assertTrue(firstRule.getJSONArray("domain").length() > 0)
    }

    @Test
    fun `proxy only routing drops the private networks direct rule`() {
        val allConfig = JSONObject(
            XrayConfigBuilder.build(
                profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
                tunFd = 0,
                routingMode = EngineSettings.ROUTING_ALL
            )
        )
        val proxyOnlyConfig = JSONObject(
            XrayConfigBuilder.build(
                profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
                tunFd = 0,
                routingMode = EngineSettings.ROUTING_PROXY_ONLY
            )
        )
        fun hasPrivateNetworksDirectRule(config: JSONObject): Boolean {
            val rules = config.getJSONObject("routing").getJSONArray("rules")
            return (0 until rules.length()).any { index ->
                val rule = rules.getJSONObject(index)
                rule.optString("outboundTag") == XrayConfigBuilder.DIRECT_OUTBOUND_TAG &&
                    rule.optJSONArray("ip")?.optString(0) == "10.0.0.0/8"
            }
        }
        assertTrue(hasPrivateNetworksDirectRule(allConfig))
        assertTrue(!hasPrivateNetworksDirectRule(proxyOnlyConfig))
        // DNS plumbing must survive in proxy-only mode.
        val rules = proxyOnlyConfig.getJSONObject("routing").getJSONArray("rules")
        assertTrue(
            (0 until rules.length()).any {
                rules.getJSONObject(it).optString("outboundTag") == XrayConfigBuilder.DNS_OUTBOUND_TAG
            }
        )
    }

    @Test
    fun `hotspot sharing exposes a real local socks inbound`() {
        val config = XrayConfigBuilder.build(
            profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
            tunFd = 0,
            hotspotSharing = true
        )
        val inbounds = JSONObject(config).getJSONArray("inbounds")
        val socks = (0 until inbounds.length())
            .map { inbounds.getJSONObject(it) }
            .first { it.getString("tag") == XrayConfigBuilder.HOTSPOT_INBOUND_TAG }
        assertEquals("socks", socks.getString("protocol"))
        assertEquals(XrayConfigBuilder.HOTSPOT_SOCKS_PORT, socks.getInt("port"))
        assertEquals("0.0.0.0", socks.getString("listen"))
    }

    @Test
    fun `out of range mtu is rejected by the engine boundary`() {
        val error = runCatching {
            XrayConfigBuilder.build(
                profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
                tunFd = 0,
                mtu = 100
            )
        }.exceptionOrNull()
        assertTrue("expected a rejection for MTU 100, got $error", error is IllegalArgumentException)
    }

    @Test
    fun `unsupported routing mode is rejected by the engine boundary`() {
        val error = runCatching {
            XrayConfigBuilder.build(
                profile(outboundJson = """{"protocol":"trojan","settings":{"address":"127.0.0.1","port":18445,"password":"pw"}}"""),
                tunFd = 0,
                routingMode = "split"
            )
        }.exceptionOrNull()
        assertTrue("expected a rejection for routing mode split, got $error", error is IllegalArgumentException)
    }
}
