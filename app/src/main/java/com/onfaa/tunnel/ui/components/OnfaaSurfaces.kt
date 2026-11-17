package com.onfaa.tunnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme

@Composable
fun OnfaaBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = OnfaaTheme.colors
    val glowAlpha = if (palette.isDark) 0.22f else 0.18f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.backgroundTop, palette.backgroundBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.glowPrimary.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.02f),
                    radius = size.width * 0.95f
                ),
                radius = size.width * 0.95f,
                center = Offset(size.width * 0.92f, size.height * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.glowSecondary.copy(alpha = glowAlpha * 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.05f, size.height * 0.78f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.05f, size.height * 0.78f)
            )
        }
        content()
    }
}

@Composable
fun OnfaaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val palette = OnfaaTheme.colors
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.02f)
            lineTo(w * 0.94f, h * 0.18f)
            lineTo(w * 0.87f, h * 0.60f)
            quadraticBezierTo(w * 0.79f, h * 0.84f, w * 0.50f, h * 0.98f)
            quadraticBezierTo(w * 0.21f, h * 0.84f, w * 0.13f, h * 0.60f)
            lineTo(w * 0.06f, h * 0.18f)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.linearGradient(
                listOf(palette.primary, palette.secondary),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
        val inner = Path().apply {
            moveTo(w * 0.24f, h * 0.25f)
            lineTo(w * 0.76f, h * 0.25f)
            lineTo(w * 0.76f, h * 0.55f)
            quadraticBezierTo(w * 0.72f, h * 0.78f, w * 0.50f, h * 0.88f)
            quadraticBezierTo(w * 0.28f, h * 0.78f, w * 0.24f, h * 0.55f)
            close()
        }
        drawPath(
            path = inner,
            brush = Brush.verticalGradient(
                listOf(
                    palette.backgroundTop.copy(alpha = 0.10f),
                    palette.backgroundTop.copy(alpha = 0.70f)
                )
            )
        )
        val bolt = Path().apply {
            moveTo(w * 0.55f, h * 0.30f)
            lineTo(w * 0.36f, h * 0.55f)
            lineTo(w * 0.49f, h * 0.55f)
            lineTo(w * 0.43f, h * 0.73f)
            lineTo(w * 0.66f, h * 0.46f)
            lineTo(w * 0.52f, h * 0.46f)
            close()
        }
        drawPath(path = bolt, color = palette.backgroundTop.copy(alpha = 0.85f))
        drawPath(
            path = shield,
            color = palette.primary.copy(alpha = 0.85f),
            style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(OnfaaDimens.cardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(OnfaaDimens.cardRadius)
    val base = Modifier
        .fillMaxWidth()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.surfaceElevated.copy(alpha = if (palette.isDark) 0.92f else 0.98f),
                    palette.surface
                )
            ),
            shape = shape
        )
        .border(
            width = 1.dp,
            color = palette.border.copy(alpha = 0.55f),
            shape = shape
        )
    Column(
        modifier = modifier
            .then(if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
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
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = palette.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(palette.primary.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    enabled: Boolean = true
) {
    val palette = OnfaaTheme.colors
    Box(
        modifier = modifier
            .size(OnfaaDimens.iconButton)
            .background(
                color = palette.surfaceElevated.copy(alpha = if (palette.isDark) 0.75f else 0.9f),
                shape = CircleShape
            )
            .border(1.dp, palette.border.copy(alpha = 0.5f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = (tint ?: palette.textPrimary).copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = OnfaaDimens.topBarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
                maxLines = 1
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/** Bouton d'en-tête carré arrondi, comme sur les maquettes. `filled` reproduit
 * le bouton d'ajout bleu plein de l'écran Configs. */
@Composable
fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    filled: Boolean = false,
    enabled: Boolean = true
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(46.dp)
            .background(
                color = if (filled) {
                    palette.secondary
                } else {
                    palette.surfaceElevated.copy(alpha = if (palette.isDark) 0.75f else 0.9f)
                },
                shape = shape
            )
            .border(
                width = 1.dp,
                color = if (filled) Color.Transparent else palette.border.copy(alpha = 0.5f),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) {
                Color.White
            } else {
                (tint ?: palette.textPrimary).copy(alpha = if (enabled) 1f else 0.4f)
            },
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnfaaLogo(size = 34.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ONFAA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = " TUNNEL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.primary
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing
        )
    }
}

@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val palette = OnfaaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = palette.textPrimary
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
fun Chevron(
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = tint ?: OnfaaTheme.colors.textMuted,
        modifier = modifier.size(20.dp)
    )
}

@Composable
fun CenteredEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
