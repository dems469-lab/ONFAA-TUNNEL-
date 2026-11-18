package com.onfaa.tunnel.core.xray

import com.onfaa.tunnel.domain.model.UserPreferences

/**
 * Engine-facing settings mapped from the persisted user preferences.
 *
 * Every field here is genuinely applied to the Xray configuration built by
 * [XrayConfigBuilder]; nothing is simulated. Options without a real Xray-core
 * equivalent (gaming mode, background behavior) never enter this object.
 */
data class EngineSettings(
    val mtu: Int = XrayConfigBuilder.DEFAULT_MTU,
    val logLevel: String = XrayConfigBuilder.DEFAULT_LOG_LEVEL,
    val routingMode: String = ROUTING_ALL,
    val dnsEndpoints: List<String> = XrayConfigBuilder.DEFAULT_DNS_ENDPOINTS,
    val muxEnabled: Boolean = false,
    val tlsFragment: Boolean = false,
    val adBlock: Boolean = false,
    val hotspotSharing: Boolean = false
) {
    init {
        require(mtu in MIN_MTU..MAX_MTU) { "MTU non supporté par le moteur : $mtu" }
        require(logLevel in SUPPORTED_LOG_LEVELS) { "Niveau de log non supporté : $logLevel" }
        require(routingMode in SUPPORTED_ROUTING_MODES) { "Mode de routage non supporté : $routingMode" }
        require(dnsEndpoints.isNotEmpty()) { "Au moins un serveur DNS est requis" }
    }

    /** True when the private-networks direct rule must be emitted. */
    val bypassPrivateNetworks: Boolean
        get() = routingMode != ROUTING_PROXY_ONLY

    /**
     * Plain host:port resolver handed to libXray's setDNS. DoH endpoints are
     * URL-shaped and only valid inside the "dns" config, so the bootstrap
     * resolver always uses the UDP form of the chosen server.
     */
    fun bootstrapResolverEndpoint(): String {
        val first = dnsEndpoints.first()
        val host = first.substringAfter("://", first).substringBefore('/')
        return host.let { if (it.contains(':')) it else "$it:53" }
    }

    companion object {
        const val ROUTING_ALL = "all"
        const val ROUTING_BYPASS_PRIVATE = "bypass_private"
        const val ROUTING_PROXY_ONLY = "proxy_only"

        val SUPPORTED_ROUTING_MODES = setOf(ROUTING_ALL, ROUTING_BYPASS_PRIVATE, ROUTING_PROXY_ONLY)
        val SUPPORTED_LOG_LEVELS = setOf("error", "warning", "info", "debug")

        const val MIN_MTU = 1280
        const val MAX_MTU = 9000

        private val LOG_LEVEL_ORDER = listOf("error", "warning", "info", "debug")

        /**
         * Maps the persisted user preferences to engine settings. DNS comes
         * first from the chosen server (DoH or plain UDP); [logLevelFloor]
         * guarantees the level needed by the build type (e.g. "info" in
         * debuggable builds for the E2E access-log evidence) is never lowered.
         */
        fun from(
            preferences: UserPreferences,
            logLevelFloor: String = XrayConfigBuilder.DEFAULT_LOG_LEVEL
        ): EngineSettings {
            val dnsEndpoints = if (preferences.dnsOverHttps) {
                listOf(dohEndpoint(preferences.dnsServer))
            } else {
                listOf(plainDnsEndpoint(preferences.dnsServer))
            }
            val level = normalizedLogLevel(preferences.logLevel)
            return EngineSettings(
                mtu = preferences.mtu.coerceIn(MIN_MTU, MAX_MTU),
                logLevel = if (rank(level) >= rank(normalizedLogLevel(logLevelFloor))) level else logLevelFloor,
                routingMode = if (preferences.routingMode in SUPPORTED_ROUTING_MODES) {
                    preferences.routingMode
                } else {
                    ROUTING_ALL
                },
                dnsEndpoints = dnsEndpoints,
                muxEnabled = preferences.muxEnabled,
                tlsFragment = preferences.tlsFragment,
                adBlock = preferences.adBlock,
                hotspotSharing = preferences.hotspotSharing
            )
        }

        /** DoH URL form accepted by the Xray "dns" config servers. */
        fun dohEndpoint(server: String): String {
            val value = server.trim()
            return when {
                value.isEmpty() -> "https://1.1.1.1/dns-query"
                value.contains("://") -> value
                else -> "https://$value/dns-query"
            }
        }

        /** Plain host (default port 53 is applied by the engine builder). */
        fun plainDnsEndpoint(server: String): String {
            val value = server.trim()
            return if (value.contains("://")) value.substringAfter("://") else value
        }

        /** Falls back to the engine default for any unknown persisted level. */
        fun normalizedLogLevel(value: String): String =
            if (value in SUPPORTED_LOG_LEVELS) value else XrayConfigBuilder.DEFAULT_LOG_LEVEL

        private fun rank(level: String): Int = LOG_LEVEL_ORDER.indexOf(level).coerceAtLeast(0)
    }
}
