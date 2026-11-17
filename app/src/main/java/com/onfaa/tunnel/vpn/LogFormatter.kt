package com.onfaa.tunnel.vpn

import com.onfaa.tunnel.core.xray.XrayErrorSanitizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure formatting layer for the diagnostic log: one file line per event and a
 * human-readable export. Sensitive values are stripped again at export time
 * (defence in depth), even though every event is already sanitized on write.
 */
object LogFormatter {
    private const val FIELD_SEPARATOR = '|'

    fun fileLine(event: TunnelEvent): String {
        val safe = event.message.replace('\n', ' ').replace('\r', ' ')
        return "${event.epochMs}$FIELD_SEPARATOR${event.severity.name}$FIELD_SEPARATOR$safe"
    }

    fun parseFileLine(line: String): TunnelEvent? {
        val parts = line.split(FIELD_SEPARATOR, limit = 3)
        if (parts.size != 3) return null
        val epochMs = parts[0].toLongOrNull() ?: return null
        val severity = runCatching { LogSeverity.valueOf(parts[1]) }.getOrNull() ?: return null
        val message = parts[2].trim()
        if (message.isEmpty()) return null
        return TunnelEvent(epochMs = epochMs, message = message, severity = severity)
    }

    fun export(
        events: List<TunnelEvent>,
        appVersion: String,
        generatedAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val builder = StringBuilder()
        builder.append("# ONFAA TUNNEL — journaux de diagnostic").append('\n')
        builder.append("# version: ").append(appVersion).append('\n')
        builder.append("# exporté le: ").append(stamp.format(Date(generatedAtEpochMs))).append('\n')
        builder.append("# événements: ").append(events.size).append('\n')
        events.forEach { event ->
            builder.append(stamp.format(Date(event.epochMs)))
                .append(" | ")
                .append(event.severity.name)
                .append(" | ")
                .append(XrayErrorSanitizer.sanitize(event.message))
                .append('\n')
        }
        return builder.toString()
    }
}
