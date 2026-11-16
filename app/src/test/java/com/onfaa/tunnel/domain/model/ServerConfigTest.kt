package com.onfaa.tunnel.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ServerConfigTest {

    @Test
    fun `server config stores provided values`() {
        val config = ServerConfig(
            id = "cfg-1",
            name = "Example",
            protocol = TunnelProtocol.VLESS,
            endpoint = "example.com:443",
            isFavorite = true,
            notes = "note"
        )

        assertEquals("cfg-1", config.id)
        assertEquals("Example", config.name)
        assertEquals(TunnelProtocol.VLESS, config.protocol)
        assertEquals("example.com:443", config.endpoint)
        assertEquals(true, config.isFavorite)
        assertEquals("note", config.notes)
    }

    @Test
    fun `user preferences default to safe non-connected values`() {
        val prefs = UserPreferences()
        assertEquals("system", prefs.themeKey)
        assertEquals(null, prefs.selectedConfigId)
        assertFalse(prefs.autoConnect)
    }
}
