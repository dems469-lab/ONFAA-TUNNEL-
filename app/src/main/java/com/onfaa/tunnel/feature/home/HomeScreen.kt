package com.onfaa.tunnel.feature.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.core.xray.XrayBridge
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.ui.components.BrandHeader
import com.onfaa.tunnel.ui.components.Chevron
import com.onfaa.tunnel.ui.components.CircleIconButton
import com.onfaa.tunnel.ui.components.ConnectionOrb
import com.onfaa.tunnel.ui.components.DividerLine
import com.onfaa.tunnel.ui.components.GlassCard
import com.onfaa.tunnel.ui.components.GradientButton
import com.onfaa.tunnel.ui.components.InlineNotice
import com.onfaa.tunnel.ui.components.ProtocolBadge
import com.onfaa.tunnel.ui.components.SectionHeader
import com.onfaa.tunnel.ui.components.StatusBadge
import com.onfaa.tunnel.ui.components.TrafficChart
import com.onfaa.tunnel.ui.components.TrafficLegend
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.util.formatBytes
import com.onfaa.tunnel.ui.util.formatDuration
import com.onfaa.tunnel.ui.util.formatRate
import com.onfaa.tunnel.vpn.OnfaaTunnelVpnService
import com.onfaa.tunnel.vpn.TrafficStatsRepository
import com.onfaa.tunnel.vpn.TunnelState
import com.onfaa.tunnel.vpn.TunnelStatusRepository
import kotlinx.coroutines.delay

private const val ENGINE_MISSING_MESSAGE =
    "Cet APK ne contient pas le moteur libXray : aucune connexion n'est possible. " +
        "Installez l'artefact « onfaa-tunnel-apk » du workflow « libXray Android build " +
        "and real VPN E2E », pas l'artefact « onfaa-tunnel-debug-apk-no-engine » " +
        "du workflow Android CI."

@Composable
fun HomeScreen(
    onOpenConfigs: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val palette = OnfaaTheme.colors
    val state by TunnelStatusRepository.state.collectAsStateWithLifecycle()
    val traffic by TrafficStatsRepository.snapshot.collectAsStateWithLifecycle()
    val profiles by TunnelConfigRepository.profiles.collectAsStateWithLifecycle()
    val selectedId by TunnelConfigRepository.selectedId.collectAsStateWithLifecycle()
    val selected = profiles.firstOrNull { it.id == selectedId }
    // A build without the pinned libXray AAR can never open a tunnel. Detecting
    // it here explains the failure instead of letting each connect attempt end
    // with a generic engine error.
    val engineAvailable = remember { XrayBridge.isAvailable() }
    val connectedState = state as? TunnelState.Connected
    val errorState = state as? TunnelState.Error
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(connectedState?.sinceEpochMs) {
        if (connectedState == null) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startTunnel(context)
        } else {
            TunnelStatusRepository.error("Permission VPN refusée par l'utilisateur")
        }
    }

    fun requestConnect() {
        if (!engineAvailable) {
            TunnelStatusRepository.error(ENGINE_MISSING_MESSAGE)
            return
        }
        val profile = selected
        if (profile == null) {
            TunnelStatusRepository.error("Aucune configuration sélectionnée")
            return
        }
        if (profile.incomplete) {
            TunnelStatusRepository.error(
                "Configuration incomplète : réimportez le lien de « ${profile.displayName} »"
            )
            return
        }
        val consent = VpnService.prepare(context)
        if (consent != null) permissionLauncher.launch(consent) else startTunnel(context)
    }

    val busy = state is TunnelState.Connecting ||
        state is TunnelState.Preparing ||
        state is TunnelState.AwaitingPermission ||
        state is TunnelState.Verifying ||
        state is TunnelState.Reconnecting ||
        state is TunnelState.Disconnecting

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OnfaaDimens.screenHorizontal)
            .padding(top = OnfaaDimens.screenTop, bottom = 28.dp)
    ) {
        BrandHeader(
            subtitle = "Votre liberté, notre priorité",
            trailing = {
                CircleIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = "Paramètres",
                    onClick = onOpenSettings
                )
            }
        )

        Spacer(modifier = Modifier.height(26.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectionOrb(
                state = state,
                enabled = !busy,
                onClick = {
                    when (state) {
                        is TunnelState.Connected, is TunnelState.Disconnecting ->
                            disconnectTunnel(context)
                        else -> requestConnect()
                    }
                }
            )
            if (connectedState != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .background(palette.success.copy(alpha = 0.10f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = palette.success,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = formatDuration((nowMs - connectedState.sinceEpochMs).coerceAtLeast(0L)),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = palette.success
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!engineAvailable) {
            InlineNotice(
                title = "Moteur Xray absent de cette compilation",
                message = ENGINE_MISSING_MESSAGE,
                color = palette.danger
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (errorState != null) {
            InlineNotice(
                title = "Connexion interrompue",
                message = errorState.message,
                color = palette.danger
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (selected == null) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(palette.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aucun serveur sélectionné",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Importez une configuration (VLESS, VMess, Trojan, " +
                                "Shadowsocks, SOCKS5) pour commencer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Chevron()
                }
                Spacer(modifier = Modifier.height(12.dp))
                GradientButton(
                    text = "Importer une configuration",
                    icon = Icons.Filled.Add,
                    onClick = onOpenConfigs
                )
            }
        } else {
            SectionHeader(title = "Serveur actif", icon = Icons.Filled.Shield)
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(onClick = onOpenConfigs) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProtocolBadge(protocol = selected.protocol)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selected.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Chevron()
                }
            }
            if (selected.incomplete) {
                Spacer(modifier = Modifier.height(8.dp))
                InlineNotice(
                    title = "Profil incomplet",
                    message = "Ce profil a été migré sans identifiant : réimportez son lien " +
                        "dans Configs pour pouvoir vous connecter.",
                    color = palette.warning
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        SectionHeader(
            title = "Trafic de la session",
            icon = Icons.Filled.Bolt,
            trailing = {
                StatusBadge(
                    text = when {
                        !traffic.available -> "Indisponible"
                        traffic.active -> "En direct"
                        traffic.downBytes > 0 || traffic.upBytes > 0 -> "Dernière session"
                        else -> "En attente"
                    },
                    color = when {
                        !traffic.available -> palette.textMuted
                        traffic.active -> palette.success
                        else -> palette.textMuted
                    }
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrafficStat(
                    icon = Icons.Filled.Download,
                    label = "Téléchargé",
                    total = if (traffic.available) formatBytes(traffic.downBytes) else "—",
                    rate = if (traffic.available && traffic.active) {
                        formatRate(traffic.downBytesPerSec)
                    } else {
                        "—"
                    },
                    color = palette.primary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(54.dp)
                        .background(palette.border.copy(alpha = 0.45f))
                )
                TrafficStat(
                    icon = Icons.Filled.Upload,
                    label = "Envoyé",
                    total = if (traffic.available) formatBytes(traffic.upBytes) else "—",
                    rate = if (traffic.available && traffic.active) {
                        formatRate(traffic.upBytesPerSec)
                    } else {
                        "—"
                    },
                    color = palette.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DividerLine()
            Spacer(modifier = Modifier.height(10.dp))
            if (traffic.samples.size >= 2) {
                TrafficChart(samples = traffic.samples)
                Spacer(modifier = Modifier.height(6.dp))
                TrafficLegend(
                    downRate = traffic.downBytesPerSec,
                    upRate = traffic.upBytesPerSec
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(palette.surfaceMuted.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (traffic.available) {
                            "Le graphique apparaîtra pendant une connexion active."
                        } else {
                            "Compteurs de trafic indisponibles sur cet appareil."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mesure réelle TrafficStats (UID application) : inclut le trafic du moteur Xray. " +
                    "Aucune valeur n'est simulée.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun TrafficStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    total: String,
    rate: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color.copy(alpha = 0.13f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
            Text(
                text = total,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = rate,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = color,
                maxLines = 1
            )
        }
    }
}

private fun startTunnel(context: Context) {
    val intent = Intent(context, OnfaaTunnelVpnService::class.java)
        .setAction(OnfaaTunnelVpnService.ACTION_CONNECT)
    ContextCompat.startForegroundService(context, intent)
}

private fun disconnectTunnel(context: Context) {
    context.startService(
        Intent(context, OnfaaTunnelVpnService::class.java)
            .setAction(OnfaaTunnelVpnService.ACTION_DISCONNECT)
    )
}
