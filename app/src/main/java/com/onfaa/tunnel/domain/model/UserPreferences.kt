package com.onfaa.tunnel.domain.model

/**
 * User preferences persisted with DataStore.
 *
 * [themeKey] and [themeMode] drive the real application theme. The remaining
 * fields back the settings screens; each one is persisted, but engine-bound
 * options are not yet applied to the Xray configuration (documented in
 * docs/design/DESIGN_SPEC.md and surfaced in the UI).
 */
data class UserPreferences(
    val themeKey: String = "onfaa",
    val themeMode: String = "system",
    val selectedConfigId: String? = null,
    val autoConnect: Boolean = false,
    val showSpeedInNotification: Boolean = true,
    val autoReconnect: Boolean = true,
    val connectionTimeoutSec: Int = 30,
    val reconnectDelaySec: Int = 5,
    val backgroundBehavior: String = "always",
    val tlsFragment: Boolean = false,
    val sniEnabled: Boolean = true,
    val adBlock: Boolean = false,
    val gamingMode: Boolean = false,
    val dnsOverHttps: Boolean = true,
    val dnsServer: String = "1.1.1.1",
    val routingMode: String = "all",
    val mtu: Int = 1500,
    val muxEnabled: Boolean = false,
    val hotspotSharing: Boolean = false,
    val logLevel: String = "warning"
)
