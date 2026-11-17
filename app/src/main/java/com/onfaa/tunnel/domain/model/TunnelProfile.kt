package com.onfaa.tunnel.domain.model

/**
 * A validated Xray outbound (VLESS, VMess, Trojan, Shadowsocks or SOCKS5)
 * produced by libXray's real share-link converter or extracted from a
 * user-supplied Xray JSON configuration.
 * [outboundJson] contains the Xray outbound object (and therefore the server
 * credential): it is encrypted at rest, never logged and never displayed.
 *
 * [incomplete] marks a profile whose credential is unavailable (for example a
 * row migrated from the v1 schema, which never stored credentials). Such a
 * profile can be listed and deleted but never connected: the user is asked to
 * re-import the link instead of failing silently.
 */
data class TunnelProfile(
    val id: String,
    val displayName: String,
    val protocol: TunnelProtocol,
    val endpoint: String,
    val security: String,
    val outboundJson: String,
    val incomplete: Boolean = false
)
