package com.onfaa.tunnel.core.xray

import com.onfaa.tunnel.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the mapping between persisted user preferences and the
 * engine settings really applied to the Xray configuration.
 */
class EngineSettingsTest {

    @Test
    fun `defaults reproduce the validated engine behavior`() {
        val settings = EngineSettings.from(UserPreferences())
        assertEquals(XrayConfigBuilder.DEFAULT_MTU, settings.mtu)
        assertEquals("warning", settings.logLevel)
        assertEquals(EngineSettings.ROUTING_ALL, settings.routingMode)
        assertEquals(listOf("https://1.1.1.1/dns-query"), settings.dnsEndpoints)
        assertFalse(settings.muxEnabled)
        assertFalse(settings.tlsFragment)
        assertFalse(settings.adBlock)
        assertFalse(settings.hotspotSharing)
        assertTrue(settings.bypassPrivateNetworks)
    }

    @Test
    fun `doh preference produces a real DoH endpoint and a plain bootstrap resolver`() {
        val settings = EngineSettings.from(UserPreferences(dnsOverHttps = true, dnsServer = "9.9.9.9"))
        assertEquals(listOf("https://9.9.9.9/dns-query"), settings.dnsEndpoints)
        assertEquals("9.9.9.9:53", settings.bootstrapResolverEndpoint())
    }

    @Test
    fun `plain udp preference produces a host endpoint`() {
        val settings = EngineSettings.from(UserPreferences(dnsOverHttps = false, dnsServer = "8.8.8.8"))
        assertEquals(listOf("8.8.8.8"), settings.dnsEndpoints)
        assertEquals("8.8.8.8:53", settings.bootstrapResolverEndpoint())
    }

    @Test
    fun `log level floor is never lowered by the preference`() {
        val settings = EngineSettings.from(
            UserPreferences(logLevel = "error"),
            logLevelFloor = "info"
        )
        assertEquals("info", settings.logLevel)
        val honored = EngineSettings.from(
            UserPreferences(logLevel = "debug"),
            logLevelFloor = "warning"
        )
        assertEquals("debug", honored.logLevel)
    }

    @Test
    fun `unknown persisted values fall back to safe engine defaults`() {
        val settings = EngineSettings.from(
            UserPreferences(logLevel = "verbose", routingMode = "split", mtu = 99999)
        )
        assertEquals("warning", settings.logLevel)
        assertEquals(EngineSettings.ROUTING_ALL, settings.routingMode)
        assertEquals(EngineSettings.MAX_MTU, settings.mtu)
    }

    @Test
    fun `engine options are carried from the preferences`() {
        val settings = EngineSettings.from(
            UserPreferences(
                mtu = 1420,
                routingMode = EngineSettings.ROUTING_PROXY_ONLY,
                muxEnabled = true,
                tlsFragment = true,
                adBlock = true,
                hotspotSharing = true
            )
        )
        assertEquals(1420, settings.mtu)
        assertEquals(EngineSettings.ROUTING_PROXY_ONLY, settings.routingMode)
        assertFalse(settings.bypassPrivateNetworks)
        assertTrue(settings.muxEnabled)
        assertTrue(settings.tlsFragment)
        assertTrue(settings.adBlock)
        assertTrue(settings.hotspotSharing)
    }

    @Test
    fun `invalid settings are rejected at the boundary`() {
        assertTrue(
            runCatching { EngineSettings(mtu = 100) }.isFailure
        )
        assertTrue(
            runCatching { EngineSettings(logLevel = "trace") }.isFailure
        )
        assertTrue(
            runCatching { EngineSettings(routingMode = "split") }.isFailure
        )
        assertTrue(
            runCatching { EngineSettings(dnsEndpoints = emptyList()) }.isFailure
        )
    }
}
