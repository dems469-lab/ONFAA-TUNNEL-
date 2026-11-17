package com.onfaa.tunnel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onfaa.tunnel.domain.model.TunnelProtocol
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(OnfaaDimens.controlHeight)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        palette.primary.copy(alpha = alpha),
                        palette.secondary.copy(alpha = alpha)
                    )
                )
            )
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = palette.onPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.onPrimary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = palette.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val height = if (compact) 42.dp else 48.dp
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(palette.surfaceElevated.copy(alpha = if (enabled) 1f else 0.45f))
            .border(1.dp, palette.border.copy(alpha = 0.65f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color? = null
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(66.dp)
            .clip(shape)
            .background(palette.surfaceElevated.copy(alpha = if (enabled) 0.9f else 0.4f))
            .border(1.dp, palette.border.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent ?: palette.textPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OnfaaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color? = null
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(OnfaaDimens.pillRadius)
    Row(
        modifier = modifier
            .height(38.dp)
            .semantics { this.selected = selected }
            .clip(shape)
            .background(
                if (selected) palette.primary else palette.surfaceElevated.copy(alpha = 0.8f)
            )
            .border(
                1.dp,
                if (selected) palette.primary else palette.border.copy(alpha = 0.55f),
                shape
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint ?: (if (selected) palette.onPrimary else palette.textSecondary),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) palette.onPrimary else palette.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun ProtocolBadge(
    protocol: TunnelProtocol,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = OnfaaTheme.colors
    // Maquette : pastille pleine — bleu pour VLESS, bleu clair pour VMess.
    // Protocoles hors maquette : teintes sémantiques de la palette active.
    val base = when (protocol) {
        TunnelProtocol.VLESS -> palette.secondary
        TunnelProtocol.VMESS -> lerp(palette.secondary, Color.White, 0.35f)
        TunnelProtocol.TROJAN -> palette.danger
        TunnelProtocol.SHADOWSOCKS -> palette.tertiary
        TunnelProtocol.SOCKS -> palette.warning
    }
    val contentColor = if (base.luminance() > 0.5f) Color(0xFF0A1020) else Color.White
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(base)
            .padding(horizontal = if (compact) 7.dp else 9.dp, vertical = if (compact) 3.dp else 5.dp)
    ) {
        Text(
            text = if (compact) {
                when (protocol) {
                    TunnelProtocol.VLESS -> "VL"
                    TunnelProtocol.VMESS -> "VM"
                    TunnelProtocol.TROJAN -> "TR"
                    TunnelProtocol.SHADOWSOCKS -> "SS"
                    TunnelProtocol.SOCKS -> "S5"
                }
            } else {
                protocol.label
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false
) {
    val shape = RoundedCornerShape(OnfaaDimens.pillRadius)
    val contentColor = if (filled) {
        if (color.luminance() > 0.5f) Color(0xFF0A1020) else Color.White
    } else {
        color
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (filled) color else color.copy(alpha = 0.15f))
            .border(
                1.dp,
                if (filled) Color.Transparent else color.copy(alpha = 0.4f),
                shape
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor ?: palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    val palette = OnfaaTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .height(1.dp)
            .background(palette.border.copy(alpha = 0.45f))
    )
}
