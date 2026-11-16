package com.onfaa.tunnel.core.xray

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
    const val TUN_INTERFACE_NAME = "onfaa0"
    const val DEFAULT_MTU = 1500
    const val DEFAULT_RESOLVER_ENDPOINT = "1.1.1.1:53"
    val DEFAULT_DNS_ENDPOINTS = listOf("1.1.1.1:53", "8.8.8.8:53")

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

    fun build(
        profile: TunnelProfile,
        tunFd: Int,
        mtu: Int = DEFAULT_MTU,
        dnsEndpoints: List<String> = DEFAULT_DNS_ENDPOINTS
    ): String {
        require(profile.protocol == TunnelProtocol.VLESS || profile.protocol == TunnelProtocol.VMESS) {
            "Only VLESS and VMess are supported"
        }
        require(dnsEndpoints.isNotEmpty()) { "At least one DNS endpoint is required" }

        val outbound = JSONObject(profile.outboundJson)
        outbound.put("tag", PROXY_OUTBOUND_TAG)

        val config = JSONObject()
        config.put("log", JSONObject().put("loglevel", "warning"))
        config.put("env", JSONObject().put("xray.tun.fd", tunFd.toString()))

        config.put("dns", JSONObject()
            .put("tag", DNS_QUERY_TAG)
            .put("queryStrategy", "UseIP")
            .put("servers", JSONArray().apply {
                dnsEndpoints.forEach { endpoint ->
                    put(dnsServerObject(endpoint))
                }
            }))

        config.put("inbounds", JSONArray().put(JSONObject()
            .put("tag", TUN_INBOUND_TAG)
            .put("protocol", "tun")
            .put("settings", JSONObject()
                .put("name", TUN_INTERFACE_NAME)
                .put("mtu", mtu))))

        config.put("outbounds", JSONArray()
            .put(outbound)
            .put(JSONObject()
                .put("tag", DNS_OUTBOUND_TAG)
                .put("protocol", "dns")
                .put("settings", JSONObject()))
            .put(JSONObject()
                .put("tag", DIRECT_OUTBOUND_TAG)
                .put("protocol", "freedom")
                .put("settings", JSONObject())))

        config.put("routing", JSONObject()
            .put("domainStrategy", "AsIs")
            .put("rules", JSONArray()
                .put(JSONObject()
                    .put("type", "field")
                    .put("inboundTag", JSONArray().put(TUN_INBOUND_TAG))
                    .put("port", "53")
                    .put("outboundTag", DNS_OUTBOUND_TAG))
                .put(JSONObject()
                    .put("type", "field")
                    .put("inboundTag", JSONArray().put(DNS_QUERY_TAG))
                    .put("outboundTag", DIRECT_OUTBOUND_TAG))
                .put(JSONObject()
                    .put("type", "field")
                    .put("ip", JSONArray(privateNetworks))
                    .put("outboundTag", DIRECT_OUTBOUND_TAG))))

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
