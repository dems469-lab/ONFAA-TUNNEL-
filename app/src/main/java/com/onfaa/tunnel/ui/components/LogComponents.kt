package com.onfaa.tunnel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.vpn.LogSeverity
import com.onfaa.tunnel.vpn.TunnelEvent

enum class LogFilter(val label: String) {
    ALL("Tous"),
    INFO("Info"),
    WARNING("Avertissement"),
    ERROR("Erreur")
}

fun TunnelEvent.matches(filter: LogFilter): Boolean = when (filter) {
    LogFilter.ALL -> true
    LogFilter.INFO -> severity == LogSeverity.INFO || severity == LogSeverity.SUCCESS
    LogFilter.WARNING -> severity == LogSeverity.WARNING
    LogFilter.ERROR -> severity == LogSeverity.ERROR
}

@Composable
fun LogEventRow(
    event: TunnelEvent,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    val accent = when (event.severity) {
        LogSeverity.INFO -> palette.secondary
        LogSeverity.WARNING -> palette.warning
        LogSeverity.ERROR -> palette.danger
        LogSeverity.SUCCESS -> palette.success
    }
    val rowBackground = when (event.severity) {
        LogSeverity.SUCCESS -> palette.success.copy(alpha = 0.08f)
        LogSeverity.ERROR -> palette.danger.copy(alpha = 0.07f)
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, CircleShape)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = event.timeLabel(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = palette.textPrimary,
            modifier = Modifier.width(58.dp)
        )
        SeverityIcon(severity = event.severity)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = event.message,
            style = MaterialTheme.typography.bodySmall,
            color = if (event.severity == LogSeverity.SUCCESS) palette.success else palette.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SeverityIcon(
    severity: LogSeverity,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    val (tint, icon) = when (severity) {
        LogSeverity.INFO -> palette.secondary to Icons.Filled.Info
        LogSeverity.WARNING -> palette.warning to Icons.Filled.Warning
        LogSeverity.ERROR -> palette.danger to Icons.Filled.Error
        LogSeverity.SUCCESS -> palette.success to Icons.Filled.CheckCircle
    }
    Box(
        modifier = modifier
            .size(20.dp)
            .background(tint.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
    }
}

enum class CheckState { OK, WARNING, FAILED, UNKNOWN }

data class DiagnosticCheck(
    val label: String,
    val value: String,
    val state: CheckState
)

@Composable
fun DiagnosticCard(
    title: String,
    subtitle: String,
    checks: List<DiagnosticCheck>,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    val hasFailure = checks.any { it.state == CheckState.FAILED }
    // Les contrôles non testés (UNKNOWN) restent neutres et ne forcent pas l'alerte.
    val hasWarning = checks.any { it.state == CheckState.WARNING }
    val accent = when {
        hasFailure -> palette.danger
        hasWarning -> palette.warning
        else -> palette.success
    }
    val badge = when {
        hasFailure -> "À CORRIGER"
        hasWarning -> "À VÉRIFIER"
        else -> "OK"
    }
    val badgeIcon = when {
        hasFailure -> Icons.Filled.Close
        hasWarning -> Icons.Filled.Warning
        else -> Icons.Filled.Check
    }

    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(palette.surfaceElevated.copy(alpha = 0.9f), CircleShape)
                    .border(1.dp, palette.border.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = palette.textPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Diagnostic automatique",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(text = badge, color = accent, icon = badgeIcon, filled = true)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = if (accent.luminance() > 0.5f) Color(0xFF062012) else Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = accent
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        checks.forEach { check ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val checkColor = when (check.state) {
                    CheckState.OK -> palette.success
                    CheckState.WARNING -> palette.warning
                    CheckState.FAILED -> palette.danger
                    CheckState.UNKNOWN -> palette.textMuted
                }
                Icon(
                    imageVector = when (check.state) {
                        CheckState.OK -> Icons.Filled.CheckCircle
                        CheckState.WARNING -> Icons.Filled.Warning
                        CheckState.FAILED -> Icons.Filled.Close
                        CheckState.UNKNOWN -> Icons.Filled.HelpOutline
                    },
                    contentDescription = null,
                    tint = checkColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = check.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = check.value,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = checkColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LogActionGrid(
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onFilter: () -> Unit,
    onExport: () -> Unit,
    onReport: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(
                icon = Icons.Filled.ContentCopy,
                label = "Copier",
                onClick = onCopy,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                icon = Icons.Filled.Delete,
                label = "Effacer",
                onClick = onClear,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                icon = Icons.Filled.FilterAlt,
                label = "Filtrer",
                onClick = onFilter,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(
                icon = Icons.Filled.Download,
                label = "Exporter",
                onClick = onExport,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                icon = Icons.Filled.BarChart,
                label = "Rapport",
                onClick = onReport,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                icon = Icons.Filled.Search,
                label = "Rechercher",
                onClick = onSearch,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
