package com.onfaa.tunnel.domain.model

/**
 * Domain model for a user-provided server configuration (VLESS/VMess planned).
 * Sensitive fields (UUID, keys, raw URIs) must never be logged.
 */
data class ServerConfig(
    val id: String,
    val name: String,
    val protocol: TunnelProtocol,
    val endpoint: String,
    val isFavorite: Boolean = false,
    val notes: String? = null
)

enum class TunnelProtocol {
    VLESS,
    VMESS
}
