package com.onfaa.tunnel.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.domain.model.UserPreferences
import com.onfaa.tunnel.onfaaAppContainer
import com.onfaa.tunnel.ui.components.DividerLine
import com.onfaa.tunnel.ui.components.GlassCard
import com.onfaa.tunnel.ui.components.HeaderActionButton
import com.onfaa.tunnel.ui.components.OnfaaLogo
import com.onfaa.tunnel.ui.components.ScreenTitle
import com.onfaa.tunnel.ui.components.SettingRow
import com.onfaa.tunnel.ui.components.SettingSwitchRow
import com.onfaa.tunnel.ui.components.SettingsGroup
import com.onfaa.tunnel.ui.components.SettingsNotice
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.theme.ThemeMode
import com.onfaa.tunnel.ui.theme.ThemePaletteId
import kotlinx.coroutines.launch

private data class SettingsSearchEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val palette = OnfaaTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = context.onfaaAppContainer.userPreferencesRepository
    val prefs by repository.preferences.collectAsStateWithLifecycle(
        initialValue = UserPreferences()
    )
    var query by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    val themeLabel = ThemePaletteId.fromKey(prefs.themeKey).label
    val modeLabel = ThemeMode.fromKey(prefs.themeMode).label

    val searchEntries = remember {
        listOf(
            SettingsSearchEntry("Connexion", "Vitesse, reconnexion, timeout", Icons.Filled.Shield, SettingsRoutes.CONNECTION),
            SettingsSearchEntry("Anti-censorship", "TLS, SNI, fragmentation", Icons.Filled.Security, SettingsRoutes.ANTI_CENSORSHIP),
            SettingsSearchEntry("Advanced Modes", "Ad blocker, gaming", Icons.Filled.Bolt, SettingsRoutes.ADVANCED_MODES),
            SettingsSearchEntry("DNS", "DoH, serveur DNS", Icons.Filled.Dns, SettingsRoutes.DNS),
            SettingsSearchEntry("Routage", "Applications et réseaux", Icons.Filled.Route, SettingsRoutes.ROUTING),
            SettingsSearchEntry("Moteur réseau", "MTU, mux, gVisor", Icons.Filled.Memory, SettingsRoutes.NETWORK_ENGINE),
            SettingsSearchEntry("Partage hotspot", "Proxy pour le partage", Icons.Filled.WifiTethering, SettingsRoutes.HOTSPOT),
            SettingsSearchEntry("Apparence", "Thèmes et mode d'affichage", Icons.Filled.Palette, SettingsRoutes.APPEARANCE),
            SettingsSearchEntry("Automation", "Deep links, connexion auto", Icons.Filled.Link, SettingsRoutes.AUTOMATION),
            SettingsSearchEntry("Diagnostics", "Niveau de log, état réel", Icons.Filled.Article, SettingsRoutes.DIAGNOSTICS),
            SettingsSearchEntry("À propos", "Version et moteur Xray", Icons.Filled.Info, SettingsRoutes.ABOUT)
        )
    }
    val results = if (query.isBlank()) {
        emptyList()
    } else {
        searchEntries.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OnfaaDimens.screenHorizontal)
            .padding(top = OnfaaDimens.screenTop, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Maquette : retour nu (flèche simple, sans cercle).
            IconButton(onClick = onBack, modifier = Modifier.size(46.dp)) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            OnfaaLogo(size = 30.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ONFAA",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = " TUNNEL",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary
                )
            }
            // Maquette : bouton loupe carré à droite de l'en-tête de marque.
            HeaderActionButton(
                icon = Icons.Filled.Search,
                contentDescription = "Rechercher dans les paramètres",
                onClick = { searchFocus.requestFocus() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        ScreenTitle(
            title = "Paramètres",
            subtitle = "Connexion, réseau et interface"
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocus),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Rechercher dans les paramètres…",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = palette.textMuted
                )
            },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Effacer la recherche",
                            tint = palette.textMuted
                        )
                    }
                }
            } else {
                null
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.primary.copy(alpha = 0.8f),
                unfocusedBorderColor = palette.border.copy(alpha = 0.55f),
                focusedContainerColor = palette.surfaceElevated.copy(alpha = 0.55f),
                unfocusedContainerColor = palette.surfaceElevated.copy(alpha = 0.45f),
                cursorColor = palette.primary,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary
            )
        )

        if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            if (results.isEmpty()) {
                GlassCard {
                    Text(
                        text = "Aucun réglage ne correspond à « $query ».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            } else {
                GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    results.forEachIndexed { index, entry ->
                        SettingRow(
                            icon = entry.icon,
                            title = entry.title,
                            subtitle = entry.subtitle,
                            onClick = { onNavigate(entry.route) }
                        )
                        if (index != results.lastIndex) DividerLine(inset = 14.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (query.isBlank()) {
            SettingsGroup(title = "Connexion", icon = Icons.Filled.Shield) {
                SettingSwitchRow(
                    icon = Icons.Filled.Bolt,
                    title = "Afficher la vitesse dans notification",
                    checked = prefs.showSpeedInNotification,
                    onCheckedChange = { scope.launch { repository.setShowSpeedInNotification(it) } }
                )
                DividerLine(inset = 14.dp)
                SettingSwitchRow(
                    icon = Icons.Filled.Shield,
                    title = "Reconnexion automatique",
                    checked = prefs.autoReconnect,
                    onCheckedChange = { scope.launch { repository.setAutoReconnect(it) } }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Bolt,
                    title = "Timeout de connexion",
                    value = "${prefs.connectionTimeoutSec} s",
                    onClick = { onNavigate(SettingsRoutes.CONNECTION) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Bolt,
                    title = "Délai de reconnexion",
                    value = "${prefs.reconnectDelaySec} s",
                    onClick = { onNavigate(SettingsRoutes.CONNECTION) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Shield,
                    title = "Comportement en arrière-plan",
                    subtitle = backgroundBehaviorLabel(prefs.backgroundBehavior),
                    onClick = { onNavigate(SettingsRoutes.CONNECTION) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            SettingsGroup(title = "Anti-censorship", icon = Icons.Filled.Security) {
                SettingRow(
                    icon = Icons.Filled.Security,
                    title = "Paramètres TLS & SNI",
                    subtitle = if (prefs.sniEnabled) "Optimisés" else "Désactivés",
                    iconTint = palette.secondary,
                    onClick = { onNavigate(SettingsRoutes.ANTI_CENSORSHIP) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Security,
                    title = "Fragmentation TLS",
                    subtitle = if (prefs.tlsFragment) "Activée" else "Désactivée",
                    iconTint = palette.secondary,
                    onClick = { onNavigate(SettingsRoutes.ANTI_CENSORSHIP) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            SettingsGroup(title = "Advanced Modes", icon = Icons.Filled.Bolt) {
                SettingSwitchRow(
                    icon = Icons.Filled.Bolt,
                    title = "Ad blocker",
                    subtitle = "Bloque les publicités et traqueurs",
                    iconTint = palette.tertiary,
                    checked = prefs.adBlock,
                    onCheckedChange = { scope.launch { repository.setAdBlock(it) } }
                )
                DividerLine(inset = 14.dp)
                SettingSwitchRow(
                    icon = Icons.Filled.Bolt,
                    title = "Gaming mode",
                    subtitle = "Priorité aux jeux",
                    iconTint = palette.tertiary,
                    checked = prefs.gamingMode,
                    onCheckedChange = { scope.launch { repository.setGamingMode(it) } }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Bolt,
                    title = "Réglages avancés",
                    subtitle = "MTU, mux, moteur réseau",
                    iconTint = palette.tertiary,
                    onClick = { onNavigate(SettingsRoutes.ADVANCED_MODES) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            SettingsGroup(title = "Réseau", icon = Icons.Filled.Dns) {
                SettingRow(
                    icon = Icons.Filled.Dns,
                    title = "DNS",
                    subtitle = if (prefs.dnsOverHttps) {
                        "DoH (HTTPS) · ${prefs.dnsServer}"
                    } else {
                        "DNS système · ${prefs.dnsServer}"
                    },
                    iconTint = palette.success,
                    onClick = { onNavigate(SettingsRoutes.DNS) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Route,
                    title = "Routage",
                    subtitle = routingLabel(prefs.routingMode),
                    iconTint = palette.success,
                    onClick = { onNavigate(SettingsRoutes.ROUTING) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Memory,
                    title = "Moteur réseau",
                    subtitle = "MTU ${prefs.mtu} · Mux ${if (prefs.muxEnabled) "on" else "off"}",
                    iconTint = palette.success,
                    onClick = { onNavigate(SettingsRoutes.NETWORK_ENGINE) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.WifiTethering,
                    title = "Partage hotspot",
                    subtitle = if (prefs.hotspotSharing) "Activé" else "Désactivé",
                    iconTint = palette.success,
                    onClick = { onNavigate(SettingsRoutes.HOTSPOT) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            SettingsGroup(title = "Interface", icon = Icons.Filled.Palette) {
                SettingRow(
                    icon = Icons.Filled.Palette,
                    title = "Apparence",
                    subtitle = "$themeLabel · mode $modeLabel",
                    iconTint = palette.primary,
                    onClick = { onNavigate(SettingsRoutes.APPEARANCE) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Article,
                    title = "Journaux",
                    subtitle = "Événements réels et diagnostic",
                    iconTint = palette.primary,
                    onClick = { onNavigate("logs") }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            SettingsGroup(title = "Plus", icon = Icons.Filled.Info) {
                SettingRow(
                    icon = Icons.Filled.Link,
                    title = "Automation",
                    subtitle = "Deep links et connexion automatique",
                    iconTint = palette.textSecondary,
                    onClick = { onNavigate(SettingsRoutes.AUTOMATION) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Article,
                    title = "Diagnostics",
                    subtitle = "Niveau de log : ${prefs.logLevel}",
                    iconTint = palette.textSecondary,
                    onClick = { onNavigate(SettingsRoutes.DIAGNOSTICS) }
                )
                DividerLine(inset = 14.dp)
                SettingRow(
                    icon = Icons.Filled.Info,
                    title = "À propos & noter",
                    subtitle = "Version, moteur Xray, sécurité",
                    iconTint = palette.textSecondary,
                    onClick = { onNavigate(SettingsRoutes.ABOUT) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsNotice(
                text = "Les réglages d'interface sont actifs et mémorisés. Les options moteur " +
                    "(fragmentation, blocage, gaming, hotspot, routage) seront raccordées au tunnel " +
                    "dans une phase ultérieure : aucun effet n'est simulé."
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun backgroundBehaviorLabel(value: String): String = when (value) {
    "optimized" -> "Optimisé"
    "stop" -> "Arrêt en arrière-plan"
    else -> "Toujours actif"
}

private fun routingLabel(value: String): String = when (value) {
    "bypass_private" -> "Contourner les réseaux privés"
    "proxy_only" -> "Proxy uniquement"
    else -> "Toutes les applications"
}
