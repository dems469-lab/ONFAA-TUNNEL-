package com.onfaa.tunnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.theme.ThemeMode
import com.onfaa.tunnel.ui.theme.ThemePaletteId

@Composable
fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val container = if (isSelected) palette.secondary else palette.surfaceElevated
            val content = if (isSelected) {
                if (palette.secondary.luminance() > 0.55f) Color(0xFF0A1020) else Color.White
            } else {
                palette.textSecondary
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .semantics { this.selected = isSelected }
                    .background(container.copy(alpha = if (isSelected) 1f else 0.8f), RoundedCornerShape(99.dp))
                    .border(
                        1.dp,
                        if (isSelected) container else palette.border.copy(alpha = 0.55f),
                        RoundedCornerShape(99.dp)
                    )
                    .clickable(role = Role.RadioButton) { onSelect(mode) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = modeIcon(mode),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = content,
                    maxLines = 1
                )
            }
        }
    }
}

private fun modeIcon(mode: ThemeMode): ImageVector = when (mode) {
    ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
}

/** Scène décorative dessinée par thème, inspirée des visuels des maquettes. */
@Composable
private fun ThemeArtwork(id: ThemePaletteId, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (id) {
            ThemePaletteId.ONFAA -> {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF051A33), Color(0xFF010812))),
                    size = size
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF19D8F5).copy(alpha = 0.50f), Color.Transparent),
                        center = Offset(w * 0.80f, h * 0.18f),
                        radius = w * 0.75f
                    ),
                    radius = w * 0.75f,
                    center = Offset(w * 0.80f, h * 0.18f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF2F80FF).copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.92f),
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = Offset(w * 0.12f, h * 0.92f)
                )
            }
            ThemePaletteId.AURORA -> {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF0B1035), Color(0xFF020410))),
                    size = size
                )
                listOf(
                    Offset(w * 0.18f, h * 0.18f),
                    Offset(w * 0.52f, h * 0.10f),
                    Offset(w * 0.80f, h * 0.22f),
                    Offset(w * 0.34f, h * 0.30f),
                    Offset(w * 0.68f, h * 0.38f)
                ).forEach { star ->
                    drawCircle(Color.White.copy(alpha = 0.65f), radius = 2.4f, center = star)
                }
                val ribbon1 = Path().apply {
                    moveTo(-w * 0.05f, h * 0.52f)
                    cubicTo(w * 0.25f, h * 0.30f, w * 0.45f, h * 0.66f, w * 0.72f, h * 0.40f)
                    quadraticBezierTo(w * 0.88f, h * 0.28f, w * 1.05f, h * 0.42f)
                }
                drawPath(
                    ribbon1,
                    Color(0xFF22D3EE).copy(alpha = 0.20f),
                    style = Stroke(width = w * 0.16f, cap = StrokeCap.Round)
                )
                drawPath(
                    ribbon1,
                    Color(0xFF22D3EE).copy(alpha = 0.85f),
                    style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
                )
                val ribbon2 = Path().apply {
                    moveTo(-w * 0.05f, h * 0.66f)
                    cubicTo(w * 0.30f, h * 0.48f, w * 0.52f, h * 0.78f, w * 0.78f, h * 0.58f)
                    quadraticBezierTo(w * 0.92f, h * 0.48f, w * 1.05f, h * 0.56f)
                }
                drawPath(
                    ribbon2,
                    Color(0xFF8B5CF6).copy(alpha = 0.18f),
                    style = Stroke(width = w * 0.14f, cap = StrokeCap.Round)
                )
                drawPath(
                    ribbon2,
                    Color(0xFF8B5CF6).copy(alpha = 0.75f),
                    style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
                )
            }
            ThemePaletteId.EMBER -> {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF2A0B08), Color(0xFF12040A))),
                    size = size
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFF9A4D).copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(w * 0.50f, h * 0.80f),
                        radius = w * 0.75f
                    ),
                    radius = w * 0.75f,
                    center = Offset(w * 0.50f, h * 0.80f)
                )
                val arc1 = Path().apply {
                    moveTo(w * 0.16f, h * 0.98f)
                    quadraticBezierTo(w * 0.30f, h * 0.42f, w * 0.48f, h * 0.18f)
                }
                drawPath(
                    arc1,
                    Color(0xFFF43F5E).copy(alpha = 0.50f),
                    style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
                )
                drawPath(
                    arc1,
                    Color(0xFFFF9A4D).copy(alpha = 0.95f),
                    style = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
                )
                val arc2 = Path().apply {
                    moveTo(w * 0.60f, h * 1.02f)
                    quadraticBezierTo(w * 0.62f, h * 0.50f, w * 0.86f, h * 0.30f)
                }
                drawPath(
                    arc2,
                    Color(0xFFFBBF24).copy(alpha = 0.85f),
                    style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
                )
            }
            ThemePaletteId.GRAPHITE -> {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF1C242F), Color(0xFF05070A))),
                    size = size
                )
                val facets = listOf(
                    floatArrayOf(0f, 0f, w * 0.55f, 0f, w * 0.28f, h * 0.48f),
                    floatArrayOf(w * 0.55f, 0f, w, h * 0.10f, w * 0.62f, h * 0.55f, w * 0.28f, h * 0.48f),
                    floatArrayOf(0f, 0f, w * 0.28f, h * 0.48f, 0f, h * 0.85f),
                    floatArrayOf(w * 0.28f, h * 0.48f, w * 0.62f, h * 0.55f, w * 0.45f, h)
                )
                facets.forEachIndexed { index, points ->
                    val facet = Path().apply {
                        moveTo(points[0], points[1])
                        var i = 2
                        while (i < points.size) {
                            lineTo(points[i], points[i + 1])
                            i += 2
                        }
                        close()
                    }
                    drawPath(facet, Color.White.copy(alpha = if (index % 2 == 0) 0.07f else 0.12f))
                    drawPath(
                        facet,
                        Color(0xFF94A3B8).copy(alpha = 0.35f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
            ThemePaletteId.ORCHID -> {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF2A0D40), Color(0xFF0F0418))),
                    size = size
                )
                val cx = w * 0.5f
                val cy = h * 0.44f
                for (petal in 0 until 5) {
                    rotate(degrees = 72f * petal + 12f, pivot = Offset(cx, cy)) {
                        drawOval(
                            color = Color(0xFFE879F9).copy(alpha = 0.42f),
                            topLeft = Offset(cx - w * 0.085f, cy - w * 0.40f),
                            size = Size(w * 0.17f, w * 0.40f)
                        )
                    }
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFFF0ABFC).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.16f
                    ),
                    radius = w * 0.16f,
                    center = Offset(cx, cy)
                )
            }
            ThemePaletteId.PAPER -> {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFFEAF3FF), Color(0xFFFDFEFF))),
                    size = size
                )
                val waves = listOf(
                    Triple(0.62f, Color(0xFF93C5FD).copy(alpha = 0.85f), 0.10f),
                    Triple(0.72f, Color(0xFF60A5FA).copy(alpha = 0.75f), -0.08f),
                    Triple(0.82f, Color(0xFF2563EB).copy(alpha = 0.80f), 0.06f)
                )
                waves.forEach { (base, color, bump) ->
                    val y = h * base
                    val wave = Path().apply {
                        moveTo(0f, y)
                        cubicTo(
                            w * 0.25f, y - h * (0.10f + bump),
                            w * 0.55f, y + h * bump,
                            w * 0.78f, y - h * 0.06f
                        )
                        quadraticBezierTo(w * 0.92f, y - h * 0.10f, w, y - h * 0.04f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(wave, color)
                }
            }
            ThemePaletteId.MATCHA -> {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF0D2C18), Color(0xFF040F08))),
                    size = size
                )
                val cx = w * 0.5f
                val cy = h * 0.42f
                listOf(-38f, 14f, -8f, 42f, 24f).forEachIndexed { index, angle ->
                    rotate(degrees = angle, pivot = Offset(cx, cy)) {
                        drawOval(
                            color = (if (index % 2 == 0) Color(0xFF4ADE80) else Color(0xFF86EFAC)).copy(alpha = 0.80f),
                            topLeft = Offset(cx - w * 0.075f, cy - w * 0.36f),
                            size = Size(w * 0.15f, w * 0.36f)
                        )
                    }
                }
                drawLine(
                    Color(0xFFBEF264).copy(alpha = 0.70f),
                    start = Offset(cx, cy + w * 0.05f),
                    end = Offset(cx, h * 0.95f),
                    strokeWidth = w * 0.03f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun ThemePreviewCard(
    id: ThemePaletteId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .aspectRatio(0.98f)
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.primary else palette.border.copy(alpha = 0.6f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
    ) {
        ThemeArtwork(id = id, modifier = Modifier.fillMaxSize())
        if (id == ThemePaletteId.ONFAA) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnfaaLogo(size = 34.dp)
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "ONFAA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))
                )
        )
        Text(
            text = id.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 9.dp)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(23.dp)
                    .background(palette.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Thème sélectionné",
                    tint = palette.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CurrentThemeCard(
    id: ThemePaletteId,
    mode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(palette.primary.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Maquette : icône pinceau pour le thème actuel.
                Icon(
                    imageVector = Icons.Filled.Brush,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thème actuel",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Text(
                    text = id.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary
                )
                Text(
                    text = "${id.tagline} · mode ${mode.label.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
