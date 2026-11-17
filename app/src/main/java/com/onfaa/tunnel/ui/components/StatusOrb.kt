package com.onfaa.tunnel.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.vpn.TunnelState

@Composable
fun ConnectionOrb(
    state: TunnelState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = OnfaaTheme.colors
    val connected = state is TunnelState.Connected
    val busy = state is TunnelState.Connecting ||
        state is TunnelState.Preparing ||
        state is TunnelState.AwaitingPermission ||
        state is TunnelState.Verifying ||
        state is TunnelState.Reconnecting ||
        state is TunnelState.Disconnecting
    val error = state is TunnelState.Error

    val accent = when {
        error -> palette.danger
        connected -> palette.success
        busy -> palette.warning
        else -> palette.primary
    }
    val targetScale by animateFloatAsState(
        targetValue = when {
            busy -> 0.96f
            connected -> 1f
            else -> 0.98f
        },
        animationSpec = tween(420),
        label = "orbScale"
    )
    val transition = rememberInfiniteTransition(label = "orbPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbPulseValue"
    )

    Box(
        modifier = modifier
            .size(OnfaaDimens.orbSize)
            .graphicsLayer {
                scaleX = targetScale
                scaleY = targetScale
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(OnfaaDimens.orbSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            drawCircle(
                color = accent.copy(alpha = 0.16f),
                radius = baseRadius * 0.94f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            val pulseRadius = baseRadius * (0.72f + 0.24f * pulse)
            drawCircle(
                color = accent.copy(alpha = if (busy) 0.28f * (1f - pulse) else 0.08f),
                radius = pulseRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.34f),
                        accent.copy(alpha = 0.08f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = baseRadius * 0.72f,
                center = center
            )
            drawCircle(
                color = accent.copy(alpha = 0.55f),
                radius = baseRadius * 0.72f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.surfaceElevated,
                        palette.surface
                    ),
                    center = center,
                    radius = baseRadius * 0.58f
                ),
                radius = baseRadius * 0.58f,
                center = center
            )
            drawCircle(
                color = accent.copy(alpha = 0.65f),
                radius = baseRadius * 0.58f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    error -> Icons.Filled.Close
                    connected -> Icons.Filled.Lock
                    else -> Icons.Filled.PowerSettingsNew
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(46.dp)
            )
            Text(
                text = when (state) {
                    TunnelState.Idle -> "DÉCONNECTÉ"
                    TunnelState.AwaitingPermission -> "AUTORISATION"
                    TunnelState.Preparing -> "PRÉPARATION"
                    TunnelState.Connecting -> "CONNEXION"
                    TunnelState.Verifying -> "VÉRIFICATION"
                    is TunnelState.Connected -> "CONNECTÉ"
                    is TunnelState.Reconnecting -> "RECONNEXION"
                    TunnelState.Disconnecting -> "ARRÊT"
                    is TunnelState.Error -> "ERREUR"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = accent
            )
        }
    }
}
