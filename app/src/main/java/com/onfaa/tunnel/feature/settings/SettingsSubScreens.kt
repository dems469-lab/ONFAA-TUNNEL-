package com.onfaa.tunnel.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.BuildConfig
import com.onfaa.tunnel.core.xray.XrayBridge
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.data.preferences.UserPreferencesRepository
import com.onfaa.tunnel.domain.model.UserPreferences
import com.onfaa.tunnel.onfaaAppContainer
import com.onfaa.tunnel.ui.components.DividerLine
import com.onfaa.tunnel.ui.components.GlassCard
import com.onfaa.tunnel.ui.components.ScreenHeader
import com.onfaa.tunnel.ui.components.SettingRow
import com.onfaa.tunnel.ui.components.SettingSwitchRow
import com.onfaa.tunnel.ui.components.SettingsGroup
import com.onfaa.tunnel.ui.components.SettingsNotice
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.vpn.TunnelState
import com.onfaa.tunnel.vpn.TunnelStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ChoiceOption(
    val value: String,
    val label: String,
    val description: String? = null
)

private class SettingsController(
    val prefs: UserPreferences,
    val repository: UserPreferencesRepository,
    val scope: CoroutineScope
)

@Composable
private fun rememberSettingsController(): SettingsController {
    val context = LocalContext.current
    val repository = context.onfaaAppContainer.userPreferencesRepository
    val prefs by repository.preferences.collectAsStateWithLifecycle(
        initialValue = UserPreferences()
    )
    val scope = rememberCoroutineScope()
    return remember(repository, prefs, scope) {
        SettingsController(prefs, repository, scope)
    }
}

@Composable
private fun SettingsDetailScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OnfaaDimens.screenHorizontal)
            .padding(top = OnfaaDimens.screenTop, bottom = 28.dp)
    ) {
        ScreenHeader(title = title, subtitle = subtitle, onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        content()
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<ChoiceOption>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (ChoiceOption) -> Unit
) {
    val palette = OnfaaTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    val isSelected = option.value == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) palette.primary else palette.textPrimary
                            )
                            if (option.description != null) {
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun InfoCard(
    title: String,
    message: String,
    icon: ImageVector = Icons.Filled.Info
) {
    val palette = OnfaaTheme.colors
    GlassCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(palette.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
fun ConnectionSettingsScreen(onBack: () -> Unit) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    val palette = OnfaaTheme.colors
    var showTimeout by remember { mutableStateOf(false) }
    var showDelay by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = "Connexion",
        subtitle = "Comportement du tunnel",
        onBack = onBack
    ) {
        SettingsGroup(title = "Notifications", icon = Icons.Filled.Bolt) {
            SettingSwitchRow(
                icon = Icons.Filled.Bolt,
                title = "Afficher la vitesse dans notification",
                subtitle = "La vitesse réelle sera affichée lorsque la télémétrie sera raccordée",
                checked = prefs.showSpeedInNotification,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setShowSpeedInNotification(it) }
                }
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SettingsGroup(title = "Reconnexion", icon = Icons.Filled.Autorenew) {
            SettingSwitchRow(
                icon = Icons.Filled.Autorenew,
                title = "Reconnexion automatique",
                subtitle = "Réessayer lorsque le tunnel tombe",
                checked = prefs.autoReconnect,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setAutoReconnect(it) }
                }
            )
            DividerLine(inset = 14.dp)
            SettingRow(
                icon = Icons.Filled.Timer,
                title = "Timeout de connexion",
                value = "${prefs.connectionTimeoutSec} s",
                onClick = { showTimeout = true }
            )
            DividerLine(inset = 14.dp)
            SettingRow(
                icon = Icons.Filled.Autorenew,
                title = "Délai de reconnexion",
                value = "${prefs.reconnectDelaySec} s",
                onClick = { showDelay = true }
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SettingsGroup(title = "Arrière-plan", icon = Icons.Filled.Shield) {
            SettingRow(
                icon = Icons.Filled.Shield,
                title = "Comportement en arrière-plan",
                value = when (prefs.backgroundBehavior) {
                    "optimized" -> "Optimisé"
                    "stop" -> "Arrêt"
                    else -> "Toujours actif"
                },
                onClick = { showBackground = true }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsNotice(
            text = "Ces préférences sont enregistrées. Le service VPN actuel reste actif en " +
                "arrière-plan ; la reconnexion automatique et les timeouts seront appliqués au " +
                "cycle de vie dans une phase ultérieure."
        )
        Text(
            text = "Valeurs appliquées au service : non raccordées dans cette version.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    if (showTimeout) {
        ChoiceDialog(
            title = "Timeout de connexion",
            options = listOf(
                ChoiceOption("15", "15 secondes"),
                ChoiceOption("30", "30 secondes"),
                ChoiceOption("60", "60 secondes"),
                ChoiceOption("120", "120 secondes")
            ),
            selected = prefs.connectionTimeoutSec.toString(),
            onDismiss = { showTimeout = false },
            onSelect = {
                controller.scope.launch {
                    controller.repository.setConnectionTimeoutSec(it.value.toInt())
                }
                showTimeout = false
            }
        )
    }
    if (showDelay) {
        ChoiceDialog(
            title = "Délai de reconnexion",
            options = listOf(
                ChoiceOption("3", "3 secondes"),
                ChoiceOption("5", "5 secondes"),
                ChoiceOption("10", "10 secondes"),
                ChoiceOption("30", "30 secondes")
            ),
            selected = prefs.reconnectDelaySec.toString(),
            onDismiss = { showDelay = false },
            onSelect = {
                controller.scope.launch {
                    controller.repository.setReconnectDelaySec(it.value.toInt())
                }
                showDelay = false
            }
        )
    }
    if (showBackground) {
        ChoiceDialog(
            title = "Comportement en arrière-plan",
            options = listOf(
                ChoiceOption("always", "Toujours actif", "Le tunnel reste actif"),
                ChoiceOption("optimized", "Optimisé", "Économise la batterie"),
                ChoiceOption("stop", "Arrêt", "Ferme le tunnel en arrière-plan")
            ),
            selected = prefs.backgroundBehavior,
            onDismiss = { showBackground = false },
            onSelect = {
                controller.scope.launch {
                    controller.repository.setBackgroundBehavior(it.value)
                }
                showBackground = false
            }
        )
    }
}

@Composable
fun AntiCensorshipSettingsScreen(onBack: () -> Unit) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    SettingsDetailScaffold(
        title = "Anti-censorship",
        subtitle = "TLS, SNI et fragmentation",
        onBack = onBack
    ) {
        SettingsGroup(title = "TLS & SNI", icon = Icons.Filled.Security) {
            SettingSwitchRow(
                icon = Icons.Filled.Security,
                title = "Optimiser TLS & SNI",
                subtitle = "Ajuste le SNI présenté au serveur",
                checked = prefs.sniEnabled,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setSniEnabled(it) }
                }
            )
            DividerLine(inset = 14.dp)
            SettingSwitchRow(
                icon = Icons.Filled.Bolt,
                title = "Fragmentation TLS",
                subtitle = "Découpe la poignée de main TLS",
                checked = prefs.tlsFragment,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setTlsFragment(it) }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard(
            title = "Option d'interface",
            message = "La fragmentation TLS et les profils SNI ne modifient pas encore la " +
                "configuration Xray. Le moteur reste inchangé dans cette phase ; aucune " +
                "protection supplémentaire n'est annoncée à tort."
        )
    }
}

@Composable
fun AdvancedModesScreen(
    onBack: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenHotspot: () -> Unit
) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    val palette = OnfaaTheme.colors
    SettingsDetailScaffold(
        title = "Advanced Modes",
        subtitle = "Blocage, jeu et partage",
        onBack = onBack
    ) {
        SettingsGroup(title = "Protection", icon = Icons.Filled.Block) {
            SettingSwitchRow(
                icon = Icons.Filled.Block,
                title = "Ad blocker",
                subtitle = "Bloque les publicités et traqueurs",
                iconTint = palette.tertiary,
                checked = prefs.adBlock,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setAdBlock(it) }
                }
            )
            DividerLine(inset = 14.dp)
            SettingSwitchRow(
                icon = Icons.Filled.SportsEsports,
                title = "Gaming mode",
                subtitle = "Priorité aux jeux",
                iconTint = palette.tertiary,
                checked = prefs.gamingMode,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setGamingMode(it) }
                }
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SettingsGroup(title = "Moteur", icon = Icons.Filled.Memory) {
            SettingRow(
                icon = Icons.Filled.Memory,
                title = "Moteur réseau",
                subtitle = "MTU, mux, gVisor",
                onClick = onOpenNetwork
            )
            DividerLine(inset = 14.dp)
            SettingRow(
                icon = Icons.Filled.WifiTethering,
                title = "Partage hotspot",
                subtitle = if (prefs.hotspotSharing) "Activé" else "Désactivé",
                onClick = onOpenHotspot
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsNotice(
            text = "Le blocage publicitaire et le mode gaming sont des préférences d'interface. " +
                "Ils ne sont pas encore traduits en règles Xray ; aucun trafic n'est filtré par " +
                "ces options actuellement."
        )
    }
}

@Composable
fun DnsSettingsScreen(onBack: () -> Unit) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    var showServers by remember { mutableStateOf(false) }
    val palette = OnfaaTheme.colors
    SettingsDetailScaffold(
        title = "DNS",
        subtitle = "Résolution et confidentialité",
        onBack = onBack
    ) {
        SettingsGroup(title = "DNS over HTTPS", icon = Icons.Filled.Dns) {
            SettingSwitchRow(
                icon = Icons.Filled.Dns,
                title = "Activer DoH",
                subtitle = "Chiffre les requêtes DNS",
                iconTint = palette.success,
                checked = prefs.dnsOverHttps,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setDnsOverHttps(it) }
                }
            )
            DividerLine(inset = 14.dp)
            SettingRow(
                icon = Icons.Filled.Public,
                title = "Serveur DNS",
                value = prefs.dnsServer,
                iconTint = palette.success,
                onClick = { showServers = true }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        GlassCard {
            Text(
                text = "Résolveur configuré",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${prefs.dnsServer} · ${if (prefs.dnsOverHttps) "DoH" else "UDP/TCP"}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Le tunnel utilise actuellement le résolveur par défaut du service " +
                    "Xray (1.1.1.1:53). Le choix ci-dessus est mémorisé mais pas encore " +
                    "transmis au moteur.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsNotice(
            text = "Le DNS réellement utilisé par le tunnel reste celui du service. " +
                "Aucun résultat DNS simulé n'est affiché."
        )
    }
    if (showServers) {
        ChoiceDialog(
            title = "Serveur DNS",
            options = listOf(
                ChoiceOption("1.1.1.1", "Cloudflare", "1.1.1.1 · 1.0.0.1"),
                ChoiceOption("8.8.8.8", "Google", "8.8.8.8 · 8.8.4.4"),
                ChoiceOption("9.9.9.9", "Quad9", "9.9.9.9 · 149.112.112.112"),
                ChoiceOption("223.5.5.5", "AliDNS", "223.5.5.5 · 223.6.6.6")
            ),
            selected = prefs.dnsServer,
            onDismiss = { showServers = false },
            onSelect = {
                controller.scope.launch {
                    controller.repository.setDnsServer(it.value)
                    controller.repository.setDnsOverHttps(true)
                }
                showServers = false
            }
        )
    }
}

@Composable
fun RoutingSettingsScreen(onBack: () -> Unit) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    var showModes by remember { mutableStateOf(false) }
    val palette = OnfaaTheme.colors
    SettingsDetailScaffold(
        title = "Routage",
        subtitle = "Applications et réseaux",
        onBack = onBack
    ) {
        SettingsGroup(title = "Mode de routage", icon = Icons.Filled.Route) {
            SettingRow(
                icon = Icons.Filled.Route,
                title = "Mode",
                value = when (prefs.routingMode) {
                    "bypass_private" -> "Réseaux privés directs"
                    "proxy_only" -> "Proxy uniquement"
                    else -> "Toutes les applis"
                },
                iconTint = palette.success,
                onClick = { showModes = true }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        GlassCard {
            Text(
                text = "Routage du moteur",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Le moteur route déjà le DNS vers dns-out, les réseaux privés en direct " +
                    "et le reste via le proxy. Le mode choisi ici n'est pas encore appliqué.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard(
            title = "Pas de fausse application",
            message = "Aucune liste d'applications n'est affichée tant que le filtrage par " +
                "application n'est pas réellement implémenté dans le service.",
            icon = Icons.Filled.Info
        )
    }
    if (showModes) {
        ChoiceDialog(
            title = "Mode de routage",
            options = listOf(
                ChoiceOption("all", "Toutes les applications", "Tout le trafic passe par le tunnel"),
                ChoiceOption("bypass_private", "Réseaux privés directs", "LAN et loopback hors tunnel"),
                ChoiceOption("proxy_only", "Proxy uniquement", "Le tunnel ne capture que le proxy")
            ),
            selected = prefs.routingMode,
            onDismiss = { showModes = false },
            onSelect = {
                controller.scope.launch { controller.repository.setRoutingMode(it.value) }
                showModes = false
            }
        )
    }
}

@Composable
fun NetworkEngineScreen(onBack: () -> Unit) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    var showMtu by remember { mutableStateOf(false) }
    SettingsDetailScaffold(
        title = "Moteur réseau",
        subtitle = "MTU, mux et transport",
        onBack = onBack
    ) {
        SettingsGroup(title = "Interface", icon = Icons.Filled.Memory) {
            SettingRow(
                icon = Icons.Filled.Memory,
                title = "MTU",
                value = prefs.mtu.toString(),
                onClick = { showMtu = true }
            )
            DividerLine(inset = 14.dp)
            SettingSwitchRow(
                icon = Icons.Filled.Bolt,
                title = "Mux",
                subtitle = "Multiplexage des connexions",
                checked = prefs.muxEnabled,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setMuxEnabled(it) }
                }
            )
            DividerLine(inset = 14.dp)
            SettingRow(
                icon = Icons.Filled.Settings,
                title = "Moteur",
                value = "Xray-core",
                showChevron = false
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsNotice(
            text = "Le TUN Android et la configuration Xray utilisent actuellement MTU 1500 " +
                "et le mux par défaut. Les valeurs choisies ici sont mémorisées, mais le " +
                "service conserve ses réglages validés pour ne pas perturber les tests E2E."
        )
    }
    if (showMtu) {
        ChoiceDialog(
            title = "MTU",
            options = listOf(
                ChoiceOption("1280", "1280", "IPv6 minimal"),
                ChoiceOption("1420", "1420", "Réseaux mobiles"),
                ChoiceOption("1500", "1500", "Valeur actuelle du service"),
                ChoiceOption("9000", "9000", "Jumbo frames")
            ),
            selected = prefs.mtu.toString(),
            onDismiss = { showMtu = false },
            onSelect = {
                controller.scope.launch { controller.repository.setMtu(it.value.toInt()) }
                showMtu = false
            }
        )
    }
}

@Composable
fun HotspotSettingsScreen(onBack: () -> Unit) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    SettingsDetailScaffold(
        title = "Partage hotspot",
        subtitle = "Proxy pour le partage de connexion",
        onBack = onBack
    ) {
        SettingsGroup(title = "Partage", icon = Icons.Filled.WifiTethering) {
            SettingSwitchRow(
                icon = Icons.Filled.WifiTethering,
                title = "Activer le partage hotspot",
                subtitle = "Expose un proxy local aux appareils connectés",
                checked = prefs.hotspotSharing,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setHotspotSharing(it) }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard(
            title = "Non raccordé",
            message = "Le partage hotspot nécessite un proxy local et une configuration " +
                "système supplémentaires. L'interrupteur mémorise la préférence mais aucun " +
                "proxy n'est exposé actuellement.",
            icon = Icons.Filled.WifiTethering
        )
    }
}

@Composable
fun AutomationSettingsScreen(onBack: () -> Unit) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    SettingsDetailScaffold(
        title = "Automation",
        subtitle = "Liens profonds et démarrage",
        onBack = onBack
    ) {
        SettingsGroup(title = "Connexion", icon = Icons.Filled.Link) {
            SettingSwitchRow(
                icon = Icons.Filled.Link,
                title = "Connexion automatique",
                subtitle = "Se connecter au démarrage de l'application",
                checked = prefs.autoConnect,
                onCheckedChange = {
                    controller.scope.launch { controller.repository.setAutoConnect(it) }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard(
            title = "Deep links",
            message = "Les liens onfaa:// ne sont pas encore déclarés dans le manifeste. " +
                "Aucun lien profond fictif n'est proposé.",
            icon = Icons.Filled.Link
        )
    }
}

@Composable
fun DiagnosticsSettingsScreen(
    onBack: () -> Unit,
    onOpenLogs: () -> Unit
) {
    val controller = rememberSettingsController()
    val prefs = controller.prefs
    val state by TunnelStatusRepository.state.collectAsStateWithLifecycle()
    val events by TunnelStatusRepository.events.collectAsStateWithLifecycle()
    val selectedId by TunnelConfigRepository.selectedId.collectAsStateWithLifecycle()
    val profiles by TunnelConfigRepository.profiles.collectAsStateWithLifecycle()
    val selected = profiles.firstOrNull { it.id == selectedId }
    val palette = OnfaaTheme.colors
    var showLevels by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = "Diagnostics",
        subtitle = "État réel et niveau de log",
        onBack = onBack
    ) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = when (state) {
                        is TunnelState.Connected -> palette.success
                        is TunnelState.Error -> palette.danger
                        else -> palette.textMuted
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (state) {
                            TunnelState.Idle -> "Aucun tunnel actif"
                            TunnelState.AwaitingPermission -> "Autorisation VPN demandée"
                            TunnelState.Preparing -> "Préparation du tunnel"
                            TunnelState.Connecting -> "Connexion en cours"
                            TunnelState.Verifying -> "Vérification du tunnel"
                            is TunnelState.Connected -> "Tunnel connecté"
                            is TunnelState.Reconnecting ->
                                "Reconnexion (tentative ${state.attempt})"
                            TunnelState.Disconnecting -> "Déconnexion"
                            is TunnelState.Error -> "Erreur réelle publiée"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${events.size} événement(s) en mémoire · " +
                            (selected?.let { "${it.protocol.label} · ${it.endpoint}" } ?: "aucun serveur"),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        SettingsGroup(title = "Journalisation", icon = Icons.Filled.Article) {
            SettingRow(
                icon = Icons.Filled.Article,
                title = "Niveau de log",
                value = prefs.logLevel,
                onClick = { showLevels = true }
            )
            DividerLine(inset = 14.dp)
            SettingRow(
                icon = Icons.Filled.Article,
                title = "Ouvrir les journaux",
                subtitle = "Filtres, recherche et export",
                onClick = onOpenLogs
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard(
            title = "Portée du diagnostic",
            message = "Le diagnostic s'appuie sur l'état réel du service et des événements " +
                "assainis. Aucune détection de fuite DNS/IP n'est implémentée ; aucune " +
                "conclusion de ce type n'est affichée.",
            icon = Icons.Filled.Info
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Le niveau de log est mémorisé ; le service utilise le niveau compilé " +
                "(info en debug, warning en release).",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }

    if (showLevels) {
        ChoiceDialog(
            title = "Niveau de log",
            options = listOf(
                ChoiceOption("error", "error", "Erreurs uniquement"),
                ChoiceOption("warning", "warning", "Erreurs et avertissements"),
                ChoiceOption("info", "info", "Cycle de vie détaillé"),
                ChoiceOption("debug", "debug", "Très détaillé (données sensibles exclues)")
            ),
            selected = prefs.logLevel,
            onDismiss = { showLevels = false },
            onSelect = {
                controller.scope.launch { controller.repository.setLogLevel(it.value) }
                showLevels = false
            }
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val palette = OnfaaTheme.colors
    var engineVersion by remember { mutableStateOf<String?>(null) }
    var engineError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val result = withContext(Dispatchers.Default) {
            runCatching { XrayBridge.xrayVersion() }
        }
        loading = false
        result.onSuccess { response ->
            if (response.success) {
                engineVersion = response.data?.optString("version")?.takeIf { it.isNotBlank() }
                    ?: "Version non retournée"
            } else {
                engineError = response.error.ifBlank { "Moteur indisponible" }
            }
        }.onFailure { failure ->
            engineError = failure.message ?: "Moteur indisponible"
        }
    }

    SettingsDetailScaffold(
        title = "À propos",
        subtitle = "ONFAA TUNNEL",
        onBack = onBack
    ) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(palette.primary.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ONFAA TUNNEL",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Votre liberté, notre priorité",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            DividerLine()
            Spacer(modifier = Modifier.height(10.dp))
            AboutLine("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            AboutLine("Package", BuildConfig.APPLICATION_ID)
            AboutLine("Android", "API ${Build.VERSION.SDK_INT}")
            AboutLine("Moteur", "Xray-core via libXray")
            AboutLine(
                "Version moteur",
                when {
                    loading -> "Vérification…"
                    engineVersion != null -> engineVersion!!
                    engineError != null -> "Indisponible"
                    else -> "Non retournée"
                }
            )
        }
        if (loading) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = palette.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interrogation réelle du moteur…",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }
        engineError?.let {
            Spacer(modifier = Modifier.height(10.dp))
            SettingsNotice(text = "Moteur non interrogeable dans cette compilation : $it")
        }
        Spacer(modifier = Modifier.height(18.dp))
        SettingsGroup(title = "Sécurité", icon = Icons.Filled.Security) {
            AboutLine("Profils", "Mémoire uniquement")
            DividerLine(inset = 14.dp)
            AboutLine("Secrets", "Jamais journalisés")
            DividerLine(inset = 14.dp)
            AboutLine("Sauvegarde Android", "Désactivée")
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsNotice(
            text = "Les captures de référence et la charte complète sont documentées dans " +
                "docs/design/DESIGN_SPEC.md. Aucune fausse statistique, connexion ou " +
                "détection de fuite n'est affichée."
        )
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    val palette = OnfaaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
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
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
