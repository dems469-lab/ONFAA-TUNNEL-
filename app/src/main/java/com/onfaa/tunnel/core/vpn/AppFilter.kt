package com.onfaa.tunnel.core.vpn

import com.onfaa.tunnel.domain.model.UserPreferences

/**
 * Real per-application split tunneling mapping.
 *
 * The VPN service applies the parsed result with the platform
 * VpnService.Builder (addAllowedApplication / addDisallowedApplication),
 * so this mapping must stay pure and deterministic: it is unit-tested on
 * the JVM and mirrors what the service will really do at establish().
 */
object AppFilter {

    const val MODE_OFF = "off"
    const val MODE_ALLOW = "allow"
    const val MODE_BLOCK = "block"
    val SUPPORTED_MODES = setOf(MODE_OFF, MODE_ALLOW, MODE_BLOCK)

    /** Hard cap so a corrupted preference can never flood establish(). */
    const val MAX_PACKAGES = 200

    data class Settings(
        val mode: String,
        val packages: List<String>
    ) {
        val isActive: Boolean
            get() = mode != MODE_OFF && packages.isNotEmpty()
    }

    fun from(preferences: UserPreferences): Settings = Settings(
        mode = if (preferences.appFilterMode in SUPPORTED_MODES) {
            preferences.appFilterMode
        } else {
            MODE_OFF
        },
        packages = parsePackages(preferences.appFilterPackages)
    )

    /**
     * Accepts newline or comma separated package names. Entries are trimmed,
     * deduplicated (keeping the first occurrence), limited to [MAX_PACKAGES]
     * and rejected when empty or containing whitespace/invalid package
     * characters. Package names may contain letters, digits, underscores,
     * dots and dollar signs (the platform grammar).
     */
    fun parsePackages(raw: String): List<String> {
        val seen = LinkedHashSet<String>()
        raw.split('\n', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { entry ->
                if (seen.size >= MAX_PACKAGES) return seen.toList()
                if (!isValidPackageName(entry)) return@forEach
                seen.add(entry)
            }
        return seen.toList()
    }

    fun isValidPackageName(value: String): Boolean {
        if (value.isEmpty() || value.length > 255) return false
        value.forEach { char ->
            val valid = char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' ||
                char == '_' || char == '.' || char == '$'
            if (!valid) return false
        }
        return true
    }

    fun serializePackages(packages: List<String>): String = packages.joinToString("\n")
}
