package com.onfaa.tunnel.core.xray

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XrayErrorSanitizerTest {

    @Test
    fun masksUuid() {
        val sanitized = XrayErrorSanitizer.sanitize(
            "client 12345678-abcd-4abc-8abc-123456789abc rejected"
        )
        assertFalse(sanitized.contains("12345678-abcd"))
        assertTrue(sanitized.contains("[uuid-masqué]"))
    }

    @Test
    fun masksShareLink() {
        val sanitized = XrayErrorSanitizer.sanitize(
            "failed for vless://12345678-abcd-4abc-8abc-123456789abc@example.com:443?x=1"
        )
        assertFalse(sanitized.contains("vless://"))
        assertFalse(sanitized.contains("12345678-abcd"))
        assertTrue(sanitized.contains("[lien-masqué]"))
    }

    @Test
    fun keepsShortTechnicalMessage() {
        assertEquals("connection refused", XrayErrorSanitizer.sanitize("connection refused"))
    }

    @Test
    fun emptyMessageHasFallback() {
        assertEquals("Erreur inconnue", XrayErrorSanitizer.sanitize(null))
        assertEquals("Erreur inconnue", XrayErrorSanitizer.sanitize("   "))
    }

    @Test
    fun truncatesLongMessage() {
        val sanitized = XrayErrorSanitizer.sanitize("x".repeat(500))
        assertTrue(sanitized.length <= 241)
        assertTrue(sanitized.endsWith("…"))
    }
}
