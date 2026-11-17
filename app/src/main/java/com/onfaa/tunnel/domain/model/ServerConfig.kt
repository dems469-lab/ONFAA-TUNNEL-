package com.onfaa.tunnel.domain.model

/**
 * Domain model for a user-provided server configuration.
 * Sensitive fields (UUID, passwords, keys, raw URIs) must never be logged.
 */
data class ServerConfig(
    val id: String,
    val name: String,
    val protocol: TunnelProtocol,
    val endpoint: String,
    val isFavorite: Boolean = false,
    val notes: String? = null
)

enum class TunnelProtocol(val label: String) {
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan"),
    SHADOWSOCKS("Shadowsocks"),
    SOCKS("SOCKS5")
}

/**
 * How the client injects traffic into the tunnel (SNI Tunnel parity).
 *
 * Only the modes executable by the official Xray-core engine are supported at
 * runtime; [DNSTT] is modelled for completeness and is rejected by
 * [com.onfaa.tunnel.core.xray.XrayConfigBuilder] with a clear message.
 */
enum class InjectMode(val label: String) {
    DIRECT("Direct"),
    DIRECT_SNI("Direct avec SNI"),
    PROXY("Proxy"),
    PROXY_SNI("Proxy avec SNI"),
    DNSTT("DNSTT");

    companion object {
        fun fromWireName(name: String?): InjectMode? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: entries.firstOrNull {
                it.label.equals(name, ignoreCase = true)
            }
    }
}
