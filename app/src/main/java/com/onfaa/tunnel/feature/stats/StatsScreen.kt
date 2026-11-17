package com.onfaa.tunnel.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.vpn.TrafficStatsRepository
import com.onfaa.tunnel.vpn.TunnelState
import com.onfaa.tunnel.vpn.TunnelStatusRepository
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun StatsScreen() {
    val state by TunnelStatusRepository.state.collectAsStateWithLifecycle()
    val traffic by TrafficStatsRepository.snapshot.collectAsStateWithLifecycle()
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val sinceEpochMs = (state as? TunnelState.Connected)?.sinceEpochMs
    LaunchedEffect(sinceEpochMs) {
        while (sinceEpochMs != null) {
            nowEpochMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Statistiques", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Mesures réelles uniquement. Aucune valeur estimée n'est affichée.",
            style = MaterialTheme.typography.bodySmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricRow("État du tunnel", stateLabel(state))
                when (val current = state) {
                    is TunnelState.Connected -> {
                        MetricRow("Profil actif", current.profileName)
                        MetricRow("Protocole", current.protocol.name)
                        MetricRow("Durée de connexion", formatDuration(nowEpochMs - current.sinceEpochMs))
                    }

                    is TunnelState.Reconnecting -> {
                        MetricRow("Profil actif", current.profileName)
                        MetricRow("Reconnexion", "tentative ${current.attempt}")
                    }

                    is TunnelState.Error -> MetricRow("Dernière erreur", current.message)

                    else -> Unit
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Trafic de session",
                    style = MaterialTheme.typography.titleSmall
                )
                HorizontalDivider()
                if (!traffic.available) {
                    Text(
                        text = "Compteurs indisponibles sur cet appareil (TrafficStats UNSUPPORTED).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!traffic.active && traffic.startedAtEpochMs == null) {
                    Text(
                        text = "Aucune session mesurée. Les compteurs démarrent avec le tunnel.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    MetricRow("Données reçues", formatBytes(traffic.downBytes))
                    MetricRow("Données envoyées", formatBytes(traffic.upBytes))
                    MetricRow("Débit descendant", "${formatBytes(traffic.downBytesPerSec)}/s")
                    MetricRow("Débit montant", "${formatBytes(traffic.upBytesPerSec)}/s")
                    MetricRow("Échantillons", "${traffic.samples.size}")
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Source : android.net.TrafficStats pour l'UID Android de l'application " +
                        "(les sockets du moteur Xray tournent dans ce processus). Ce sont des " +
                        "compteurs UID, pas les compteurs noyau de l'interface TUN : ils incluent " +
                        "aussi le trafic local de l'application.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun stateLabel(state: TunnelState): String = when (state) {
    TunnelState.Idle -> "Déconnecté"
    TunnelState.AwaitingPermission -> "Autorisation demandée"
    TunnelState.Preparing -> "Préparation"
    TunnelState.Connecting -> "Connexion en cours"
    TunnelState.Verifying -> "Vérification de la connectivité"
    is TunnelState.Connected -> "Connecté"
    is TunnelState.Reconnecting -> "Reconnexion"
    TunnelState.Disconnecting -> "Déconnexion en cours"
    is TunnelState.Error -> "Erreur"
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes o"
    val units = listOf("Ko", "Mo", "Go", "To")
    var value = bytes.toDouble() / 1_024.0
    var index = 0
    while (value >= 1_024.0 && index < units.lastIndex) {
        value /= 1_024.0
        index++
    }
    return String.format(Locale.US, "%.1f %s", value, units[index])
}
