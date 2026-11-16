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
        // Long words separated by spaces so the base64-like secret masker does
        // not replace the input before the 240-character truncation applies.
        val sanitized = XrayErrorSanitizer.sanitize("erreur " + "x ".repeat(500))
        assertTrue(sanitized.length <= 241)
        assertTrue(sanitized.endsWith("…"))
    }

    @Test
    fun masksLongBase64LikeSecretInsteadOfTruncating() {
        val sanitized = XrayErrorSanitizer.sanitize("key=" + "A".repeat(64))
        assertFalse(sanitized.contains("A".repeat(64)))
        assertTrue(sanitized.contains("[valeur-masquée]"))
    }
}
