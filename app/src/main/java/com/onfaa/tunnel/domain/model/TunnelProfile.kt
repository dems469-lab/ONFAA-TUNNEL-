package com.onfaa.tunnel.domain.model

/**
 * A validated VLESS/VMess outbound produced by libXray's real share-link
 * converter. [outboundJson] contains the Xray outbound object (and therefore
 * the server credential): it is never logged or displayed.
 */
data class TunnelProfile(
    val id: String,
    val displayName: String,
    val protocol: TunnelProtocol,
    val endpoint: String,
    val security: String,
    val outboundJson: String
)
