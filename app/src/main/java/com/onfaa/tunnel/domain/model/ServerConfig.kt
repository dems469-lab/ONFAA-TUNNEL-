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
