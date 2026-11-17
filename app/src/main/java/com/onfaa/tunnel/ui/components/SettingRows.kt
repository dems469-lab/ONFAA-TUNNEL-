package com.onfaa.tunnel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onfaa.tunnel.ui.theme.OnfaaTheme

@Composable
fun SettingsGroup(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = OnfaaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        // Maquette Paramètres (FR) : tuile d'icône cyan + titre en cyan, sans ligne.
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                SettingIcon(icon = icon, tint = palette.primary)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.primary
            )
        }
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            content()
        }
    }
}

@Composable
fun SettingIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(11.dp))
            .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color? = null,
    value: String? = null,
    valueColor: Color? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(
            icon = icon,
            tint = (iconTint ?: palette.primary).copy(alpha = if (enabled) 1f else 0.45f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary.copy(alpha = if (enabled) 1f else 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary.copy(alpha = if (enabled) 1f else 0.45f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (value != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = valueColor ?: palette.textSecondary,
                maxLines = 1
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(6.dp))
            Chevron()
        }
    }
}

@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color? = null,
    enabled: Boolean = true
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                enabled = enabled,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(
            icon = icon,
            tint = (iconTint ?: palette.primary).copy(alpha = if (enabled) 1f else 0.45f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary.copy(alpha = if (enabled) 1f else 0.5f)
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 2
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.primary,
                checkedBorderColor = palette.primary,
                uncheckedThumbColor = palette.textMuted,
                uncheckedTrackColor = palette.surfaceMuted,
                uncheckedBorderColor = palette.border,
                disabledCheckedTrackColor = palette.secondary.copy(alpha = 0.4f),
                disabledUncheckedTrackColor = palette.surfaceMuted.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingsNotice(
    text: String,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                palette.primary.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, palette.primary.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "i",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
    }
}

@Composable
fun InlineNotice(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val palette = OnfaaTheme.colors
    val tint = color ?: palette.warning
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tint, RoundedCornerShape(99.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = tint
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
    }
}
