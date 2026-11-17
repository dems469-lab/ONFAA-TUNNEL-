package com.onfaa.tunnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.util.formatRate
import com.onfaa.tunnel.vpn.TrafficSample
import kotlin.math.max

/**
 * Sparkline fed exclusively by real TrafficSample values. An empty list draws
 * a neutral baseline, never a decorative fake curve.
 */
@Composable
fun TrafficChart(
    samples: List<TrafficSample>,
    modifier: Modifier = Modifier,
    showUpload: Boolean = true
) {
    val palette = OnfaaTheme.colors
    val downColor = palette.primary
    val upColor = palette.secondary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
    ) {
        val chartHeight = size.height
        val chartWidth = size.width
        val gridColor = palette.border.copy(alpha = 0.32f)
        repeat(4) { index ->
            val y = chartHeight * (index + 1) / 5f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (samples.size < 2) return@Canvas

        val maxValue = max(
            samples.maxOfOrNull { it.downBytesPerSec } ?: 0L,
            if (showUpload) samples.maxOfOrNull { it.upBytesPerSec } ?: 0L else 0L
        ).coerceAtLeast(1L).toFloat()

        fun pointFor(index: Int, value: Long): Offset {
            val x = if (samples.size == 1) 0f else chartWidth * index / (samples.size - 1).toFloat()
            val normalized = (value.toFloat() / maxValue).coerceIn(0f, 1f)
            val y = chartHeight - 6.dp.toPx() - normalized * (chartHeight - 18.dp.toPx())
            return Offset(x, y)
        }

        fun buildPath(values: List<Long>): Path {
            val path = Path()
            values.forEachIndexed { index, value ->
                val point = pointFor(index, value)
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    val previous = pointFor(index - 1, values[index - 1])
                    val midX = (previous.x + point.x) / 2f
                    path.cubicTo(midX, previous.y, midX, point.y, point.x, point.y)
                }
            }
            return path
        }

        val downValues = samples.map { it.downBytesPerSec }
        val upValues = samples.map { it.upBytesPerSec }
        val downPath = buildPath(downValues)

        val area = Path().apply {
            addPath(downPath)
            lineTo(chartWidth, chartHeight)
            lineTo(0f, chartHeight)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(downColor.copy(alpha = 0.28f), Color.Transparent),
                startY = 0f,
                endY = chartHeight
            )
        )
        drawPath(
            path = downPath,
            color = downColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        if (showUpload) {
            drawPath(
                path = buildPath(upValues),
                color = upColor.copy(alpha = 0.9f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun TrafficLegend(
    downRate: Long,
    upRate: Long,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(color = palette.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Descendant ${formatRate(downRate)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(color = palette.secondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Montant ${formatRate(upRate)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(color, CircleShape)
    )
}
