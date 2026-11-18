package com.onfaa.tunnel.domain.model

/**
 * User preferences persisted with DataStore.
 *
 * [themeKey] and [themeMode] drive the real application theme. Engine-bound
 * fields (DNS, routing, MTU, mux, TLS fragmentation, ad blocker, hotspot,
 * log level, reconnection) are mapped to real Xray-core options by
 * [com.onfaa.tunnel.core.xray.EngineSettings] and applied by the VPN service
 * at the next connection. [gamingMode] and [backgroundBehavior] have no real
 * engine equivalent and stay persisted-only (documented in the UI).
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
