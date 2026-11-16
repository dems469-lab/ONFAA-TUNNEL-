package com.onfaa.tunnel.core.xray

/**
 * Prevents configuration secrets (UUIDs, share links, credentials) from
 * reaching logs, UI errors or CI output.
 */
object XrayErrorSanitizer {
    private val uuid = Regex(
        "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b"
    )
    private val anyUri = Regex("(?i)\\b[a-z][a-z0-9+.-]*://[^\\s\"']+")
    private val base64ish = Regex("\\b[A-Za-z0-9+/=]{48,}\\b")

    fun sanitize(message: String?): String {
        val raw = message?.trim().orEmpty()
        if (raw.isEmpty()) return "Erreur inconnue"
        val cleaned = raw
            .replace(uuid, "[uuid-masqué]")
            .replace(anyUri, "[lien-masqué]")
            .replace(base64ish, "[valeur-masquée]")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (cleaned.length > 240) cleaned.take(240) + "…" else cleaned
    }
}
