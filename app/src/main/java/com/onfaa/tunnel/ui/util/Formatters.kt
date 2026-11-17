package com.onfaa.tunnel.ui.util

import java.util.Locale
import kotlin.math.abs

fun formatBytes(bytes: Long): String {
    if (bytes < 0L) return "—"
    if (bytes < 1024L) return "$bytes o"
    val units = arrayOf("Ko", "Mo", "Go", "To")
    var value = bytes.toDouble()
    var index = -1
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (value >= 100.0) {
        String.format(Locale.FRANCE, "%.0f %s", value, units[index])
    } else {
        String.format(Locale.FRANCE, "%.1f %s", value, units[index])
    }
}

fun formatRate(bytesPerSecond: Long): String =
    if (bytesPerSecond < 0L) "—" else "${formatBytes(bytesPerSecond)}/s"

fun formatDuration(durationMs: Long): String {
    val safe = abs(durationMs)
    val totalSeconds = safe / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatLatency(millis: Long?): String =
    if (millis == null) "Non testé" else "$millis ms"
