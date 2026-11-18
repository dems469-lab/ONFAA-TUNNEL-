package com.onfaa.tunnel.core.xray

import com.onfaa.tunnel.domain.model.InjectMode
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.domain.model.TunnelProtocol
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the complete Xray JSON configuration consumed by libXray's runXray.
 *
 * Android TUN wiring follows the real Xray-core v26.11.9 contract:
 * the TUN descriptor is passed in the root "env" object as "xray.tun.fd"
 * (libXray removed SetTunFd). The VpnService owns addresses/routes/DNS; the
 * tun inbound only reads packets from the descriptor.
 */
object XrayConfigBuilder {
    const val TUN_INBOUND_TAG = "tun-in"
    const val PROXY_OUTBOUND_TAG = "proxy"
    const val DNS_OUTBOUND_TAG = "dns-out"
    const val DIRECT_OUTBOUND_TAG = "direct"
    const val DNS_QUERY_TAG = "dns-direct"
    const val FRAGMENT_OUTBOUND_TAG = "tls-fragment"
    const val BLOCKED_OUTBOUND_TAG = "blocked"
    const val HOTSPOT_INBOUND_TAG = "hotspot-socks"
    const val TUN_INTERFACE_NAME = "onfaa0"
    const val DEFAULT_MTU = 1500
    const val DEFAULT_RESOLVER_ENDPOINT = "1.1.1.1:53"
    const val DEFAULT_LOG_LEVEL = "warning"
    const val HOTSPOT_SOCKS_PORT = 10808
    val DEFAULT_DNS_ENDPOINTS = listOf("1.1.1.1:53", "8.8.8.8:53")

    /**
     * Embedded, deliberately conservative ad/tracker domain list. A full
     * geosite database is not shipped, so blocking stays honest and bounded
     * to well-known advertising and tracking hosts.
     */
    val AD_DOMAINS = listOf(
        "domain:doubleclick.net",
        "domain:googlesyndication.com",
        "domain:googleadservices.com",
        "domain:google-analytics.com",
        "domain:adservice.google.com",
        "domain:adnxs.com",
        "domain:adsrvr.org",
        "domain:criteo.com",
        "domain:outbrain.com",
        "domain:taboola.com",
        "domain:pubmatic.com",
        "domain:rubiconproject.com",
        "domain:scorecardresearch.com",
        "domain:quantserve.com",
        "domain:moatads.com",
        "domain:amplitude.com",
        "domain:appsflyer.com",
        "domain:branch.io",
        "domain:applovin.com",
        "domain:adcolony.com",
        "domain:vungle.com",
        "domain:inmobi.com"
    )

    /** Transport networks executable by the official Xray-core engine. */
    val SUPPORTED_TRANSPORTS = setOf("tcp", "ws", "grpc")

    /** Inject modes executable by the official Xray-core engine. */
    val SUPPORTED_INJECT_MODES = setOf(
        InjectMode.DIRECT,
        InjectMode.DIRECT_SNI,
        InjectMode.PROXY,
        InjectMode.PROXY_SNI
    )

    private val privateNetworks = listOf(
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "127.0.0.0/8",
        "169.254.0.0/16",
        "::1/128",
        "fc00::/7",
        "fe80::/10"
    )

    private val SUPPORTED_PROTOCOLS = setOf(
        TunnelProtocol.VLESS,
        TunnelProtocol.VMESS,
        TunnelProtocol.TROJAN,
        TunnelProtocol.SHADOWSOCKS,
        TunnelProtocol.SOCKS
    )

    fun build(
        profile: TunnelProfile,
        tunFd: Int,
        mtu: Int = DEFAULT_MTU,
        dnsEndpoints: List<String> = DEFAULT_DNS_ENDPOINTS,
        logLevel: String = DEFAULT_LOG_LEVEL,
        sni: String? = null,
        injectMode: InjectMode? = null,
        transport: String? = null,
        routingMode: String = EngineSettings.ROUTING_ALL,
        muxEnabled: Boolean = false,
        tlsFragment: Boolean = false,
        adBlock: Boolean = false,
        hotspotSharing: Boolean = false,
        dnsViaProxy: Boolean = false
    ): String {
        require(profile.protocol in SUPPORTED_PROTOCOLS) {
            "Unsupported protocol: ${profile.protocol}"
        }
        require(dnsEndpoints.isNotEmpty()) { "At least one DNS endpoint is required" }
        require(injectMode == null || injectMode in SUPPORTED_INJECT_MODES) {
            "Mode d'injection non supporté par le moteur Xray : ${injectMode?.label}"
        }
        require(transport == null || transport in SUPPORTED_TRANSPORTS) {
            "Transport réseau non supporté par le moteur Xray : $transport"
        }
        require(routingMode in EngineSettings.SUPPORTED_ROUTING_MODES) {
            "Mode de routage non supporté par le moteur Xray : $routingMode"
        }
        require(mtu in EngineSettings.MIN_MTU..EngineSettings.MAX_MTU) {
            "MTU non supporté par le moteur Xray : $mtu"
        }

        val outbound = JSONObject(profile.outboundJson)
        outbound.put("tag", PROXY_OUTBOUND_TAG)

        val existingStreamSettings = outbound.optJSONObject("streamSettings")
        val streamSettings = existingStreamSettings ?: JSONObject()
        var changedStreamSettings = false
        if (injectMode == InjectMode.DIRECT_SNI || injectMode == InjectMode.PROXY_SNI) {
            // Xray falls back to the server address as SNI when TLS is used
            // without an explicit serverName, so mirror that fallback here.
            val effectiveSni = sni
                ?: streamSettings.optString("sni").takeIf { it.isNotBlank() }
                ?: profile.endpoint.substringBeforeLast(":").takeIf { it.isNotBlank() }
            if (effectiveSni == null) {
                throw IllegalArgumentException("Le mode ${injectMode.label} requiert un SNI")
            }
            if (streamSettings.optString("sni") != effectiveSni) {
                streamSettings.put("sni", effectiveSni)
                changedStreamSettings = true
            }
        }
        if (transport != null) {
            if (streamSettings.optString("network") != transport) {
                streamSettings.put("network", transport)
                changedStreamSettings = true
            }
        }
        if (muxEnabled && !streamSettings.has("mux")) {
            // Official Xray-core multiplexing on the proxy outbound; only set
            // when the profile does not already carry its own mux settings.
            streamSettings.put(
                "mux",
                JSONObject().put("enabled", true).put("concurrency", 8)
            )
            changedStreamSettings = true
        }
        if (tlsFragment) {
            // Official Xray-core fragmentation: the proxy outbound dials its
            // underlying TLS connection through a freedom outbound carrying
            // the fragment dialer settings (tlshello).
            outbound.put("dialerProxy", FRAGMENT_OUTBOUND_TAG)
        }
        if (changedStreamSettings || tlsFragment) {
            if (streamSettings.length() > 0) {
                outbound.put("streamSettings", streamSettings)
            }
        }

        val config = JSONObject()
        config.put("log", JSONObject().put("loglevel", logLevel))
        config.put("env", JSONObject().put("xray.tun.fd", tunFd.toString()))

        config.put("dns", JSONObject()
            .put("tag", DNS_QUERY_TAG)
            .put("queryStrategy", "UseIP")
            .put("servers", JSONArray().apply {
                dnsEndpoints.forEach { endpoint ->
                    put(dnsServerObject(endpoint))
                }
            }))

        val inbounds = JSONArray().put(JSONObject()
            .put("tag", TUN_INBOUND_TAG)
            .put("protocol", "tun")
            .put("settings", JSONObject()
                .put("name", TUN_INTERFACE_NAME)
                .put("mtu", mtu)))
        if (hotspotSharing) {
            // Real local SOCKS proxy reachable from LAN/hotspot clients so the
            // tethered devices can route through the same engine.
            inbounds.put(JSONObject()
                .put("tag", HOTSPOT_INBOUND_TAG)
                .put("listen", "0.0.0.0")
                .put("port", HOTSPOT_SOCKS_PORT)
                .put("protocol", "socks")
                .put("settings", JSONObject()
                    .put("auth", "noauth")
                    .put("udp", true)))
        }
        config.put("inbounds", inbounds)

        val outbounds = JSONArray()
            .put(outbound)
            .put(JSONObject()
                .put("tag", DNS_OUTBOUND_TAG)
                .put("protocol", "dns")
                .put("settings", JSONObject()))
            .put(JSONObject()
                .put("tag", DIRECT_OUTBOUND_TAG)
                .put("protocol", "freedom")
                .put("settings", JSONObject()))
        if (tlsFragment) {
            outbounds.put(JSONObject()
                .put("tag", FRAGMENT_OUTBOUND_TAG)
                .put("protocol", "freedom")
                .put("settings", JSONObject().put("fragment", JSONObject()
                    .put("packets", "tlshello")
                    .put("length", "100-200")
                    .put("interval", "10-20"))))
        }
        if (adBlock) {
            outbounds.put(JSONObject()
                .put("tag", BLOCKED_OUTBOUND_TAG)
                .put("protocol", "blackhole")
                .put("settings", JSONObject()))
        }
        config.put("outbounds", outbounds)

        val rules = JSONArray()
        if (adBlock) {
            rules.put(JSONObject()
                .put("type", "field")
                .put("domain", JSONArray(AD_DOMAINS))
                .put("outboundTag", BLOCKED_OUTBOUND_TAG))
        }
        rules.put(JSONObject()
            .put("type", "field")
            .put("inboundTag", JSONArray().put(TUN_INBOUND_TAG))
            .put("port", "53")
            .put("outboundTag", DNS_OUTBOUND_TAG))
        // Queries issued by the engine's own DNS module carry the DNS tag as
        // inbound tag, so this rule decides where resolution really happens.
        // Sent direct (freedom) by default; with [dnsViaProxy] they travel
        // through the tunnel like the user traffic, which is the only way DNS
        // survives networks that block or hijack public resolvers.
        rules.put(JSONObject()
            .put("type", "field")
            .put("inboundTag", JSONArray().put(DNS_QUERY_TAG))
            .put("outboundTag", if (dnsViaProxy) PROXY_OUTBOUND_TAG else DIRECT_OUTBOUND_TAG))
        if (routingMode != EngineSettings.ROUTING_PROXY_ONLY) {
            rules.put(JSONObject()
                .put("type", "field")
                .put("ip", JSONArray(privateNetworks))
                .put("outboundTag", DIRECT_OUTBOUND_TAG))
        }
        config.put("routing", JSONObject()
            .put("domainStrategy", "AsIs")
            .put("rules", rules))

        return config.toString()
    }

    /**
     * Xray accepts plain UDP endpoints ("1.1.1.1") with a separate port, or a
     * URL form such as "tcp://10.0.2.2:15353". In the URL form the port is part
     * of the address and must not be duplicated in the JSON "port" field.
     */
    private fun dnsServerObject(endpoint: String): JSONObject {
        val value = endpoint.trim()
        if (value.contains("://")) {
            return JSONObject().put("address", value)
        }
        val (address, port) = splitEndpoint(value)
        return JSONObject().put("address", address).put("port", port)
    }

    /** Splits "1.1.1.1:53", "8.8.8.8" or "[2001:db8::1]:53". */
    fun splitEndpoint(endpoint: String): Pair<String, Int> {
        val value = endpoint.trim()
        if (value.startsWith("[")) {
            val end = value.indexOf(']')
            if (end > 0) {
                val host = value.substring(1, end)
                val port = value.substring(end + 1).removePrefix(":").toIntOrNull() ?: 53
                return host to port
            }
        }
        val lastColon = value.lastIndexOf(':')
        if (lastColon > 0 && value.indexOf(':') == lastColon) {
            val host = value.substring(0, lastColon)
            val port = value.substring(lastColon + 1).toIntOrNull()
            if (port != null && port in 1..65535) return host to port
        }
        return value to 53
    }
}
