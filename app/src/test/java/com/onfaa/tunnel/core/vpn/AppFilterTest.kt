package com.onfaa.tunnel.core.vpn

import com.onfaa.tunnel.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {

    @Test
    fun `defaults are inactive`() {
        val settings = AppFilter.from(UserPreferences())
        assertEquals(AppFilter.MODE_OFF, settings.mode)
        assertTrue(settings.packages.isEmpty())
        assertFalse(settings.isActive)
    }

    @Test
    fun `unknown mode falls back to off`() {
        val settings = AppFilter.from(
            UserPreferences(appFilterMode = "magic", appFilterPackages = "com.a")
        )
        assertEquals(AppFilter.MODE_OFF, settings.mode)
        assertFalse(settings.isActive)
    }

    @Test
    fun `parses newline and comma separated lists`() {
        val packages = AppFilter.parsePackages("com.a\ncom.b, com.c\n\n")
        assertEquals(listOf("com.a", "com.b", "com.c"), packages)
    }

    @Test
    fun `deduplicates keeping first occurrence`() {
        val packages = AppFilter.parsePackages("com.a, com.b, com.a")
        assertEquals(listOf("com.a", "com.b"), packages)
    }

    @Test
    fun `invalid entries are dropped not fatal`() {
        val packages = AppFilter.parsePackages("com.ok, not valid, , com.ok2")
        assertEquals(listOf("com.ok", "com.ok2"), packages)
    }

    @Test
    fun `package grammar accepts platform characters`() {
        assertTrue(AppFilter.isValidPackageName("com.example.app"))
        assertTrue(AppFilter.isValidPackageName("com.example.app_2\$Sub"))
        assertFalse(AppFilter.isValidPackageName(""))
        assertFalse(AppFilter.isValidPackageName("com example"))
        assertFalse(AppFilter.isValidPackageName("com/ex ample"))
        assertFalse(AppFilter.isValidPackageName("a".repeat(256)))
    }

    @Test
    fun `list is capped at the documented maximum`() {
        val raw = (1..300).joinToString("\n") { "com.app$it" }
        val packages = AppFilter.parsePackages(raw)
        assertEquals(AppFilter.MAX_PACKAGES, packages.size)
    }

    @Test
    fun `allow mode with packages is active and round-trips`() {
        val packages = listOf("com.a", "com.b")
        val serialized = AppFilter.serializePackages(packages)
        val settings = AppFilter.from(
            UserPreferences(appFilterMode = AppFilter.MODE_ALLOW, appFilterPackages = serialized)
        )
        assertTrue(settings.isActive)
        assertEquals(packages, settings.packages)
    }
}
