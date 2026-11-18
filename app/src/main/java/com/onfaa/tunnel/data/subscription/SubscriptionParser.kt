package com.onfaa.tunnel.data.subscription

import java.util.Base64

/**
 * Pure parser for v2ray-style subscription payloads (no network, no engine).
 *
 * A subscription is either
 *  - a base64 blob (standard or URL-safe alphabet, optional padding) that
 *    decodes to a newline separated list of share links, or
 *  - a plain newline separated list of share links.
 *
 * Only link lines with a supported scheme are returned; blank lines and
 * unknown schemes are skipped instead of failing the whole import, and the
 * summary reports them so the UI can say exactly what was kept or dropped.
 */
object SubscriptionParser {

    data class Parsed(
        val links: List<String>,
        val skipped: Int
    )

    fun parse(raw: String): Parsed {
        val text = raw.trim().removePrefix("\uFEFF").trim()
        if (text.isEmpty()) return Parsed(emptyList(), 0)
        val body = decodeBase64OrText(text)
        val links = ArrayList<String>()
        var skipped = 0
        body.split('\n', '\r')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                if (isSupportedLink(line)) links.add(line) else skipped += 1
            }
        return Parsed(links, skipped)
    }

    /**
     * True when the line starts with a supported share scheme followed by "://".
     * A line may also carry a fragment/remark; validation of the link itself
     * stays with the ConfigImporter, exactly like a manual import.
     */
    fun isSupportedLink(line: String): Boolean {
        val scheme = line.substringBefore("://", "").lowercase()
        return scheme in SUPPORTED_SCHEMES
    }

    private fun decodeBase64OrText(text: String): String {
        // A plain link list always contains "://" so the raw text is returned
        // as-is; only a payload without that marker goes through base64.
        if (text.contains("://")) return text
        // Strict decoding: the MIME decoder silently drops characters that are
        // not in the base64 alphabet, which would turn a corrupted payload
        // into unreadable garbage instead of a readable failure. Whitespace
        // is removed first (some providers wrap base64), then the URL-safe
        // alphabet is normalized to the standard one and the padding is
        // restored before the strict decode.
        val compact = text.replace(Regex("\\s"), "")
        val normalized = compact.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val bytes = try {
            Base64.getDecoder().decode(padded)
        } catch (failure: IllegalArgumentException) {
            throw SubscriptionException(
                "Abonnement illisible : ni une liste de liens, ni un bloc base64 valide"
            )
        }
        val decodedText = String(bytes, Charsets.UTF_8)
        if (decodedText.isBlank()) {
            throw SubscriptionException("Abonnement décodé vide")
        }
        return decodedText
    }

    private val SUPPORTED_SCHEMES = setOf("vless", "vmess", "trojan", "ss", "socks")
}

class SubscriptionException(message: String) : Exception(message)
