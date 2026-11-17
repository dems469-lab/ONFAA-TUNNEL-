package com.onfaa.tunnel.feature.logs

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.BuildConfig
import com.onfaa.tunnel.core.xray.XrayBridge
import com.onfaa.tunnel.data.config.LatencyRepository
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.ui.components.CheckState
import com.onfaa.tunnel.ui.components.DiagnosticCard
import com.onfaa.tunnel.ui.components.DiagnosticCheck
import com.onfaa.tunnel.ui.components.DividerLine
import com.onfaa.tunnel.ui.components.GlassCard
import com.onfaa.tunnel.ui.components.HeaderActionButton
import com.onfaa.tunnel.ui.components.InlineNotice
import com.onfaa.tunnel.ui.components.LogActionGrid
import com.onfaa.tunnel.ui.components.LogEventRow
import com.onfaa.tunnel.ui.components.LogFilter
import com.onfaa.tunnel.ui.components.OnfaaChip
import com.onfaa.tunnel.ui.components.ScreenHeader
import com.onfaa.tunnel.ui.components.matches
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.util.formatLatency
import com.onfaa.tunnel.vpn.OnfaaTunnelVpnService
import com.onfaa.tunnel.vpn.TunnelState
import com.onfaa.tunnel.vpn.TunnelStatusRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val palette = OnfaaTheme.colors
    val events by TunnelStatusRepository.events.collectAsStateWithLifecycle()
    val state by TunnelStatusRepository.state.collectAsStateWithLifecycle()
    val profiles by TunnelConfigRepository.profiles.collectAsStateWithLifecycle()
    val selectedId by TunnelConfigRepository.selectedId.collectAsStateWithLifecycle()
    val latencyResults by LatencyRepository.results.collectAsStateWithLifecycle()
    val selected = profiles.firstOrNull { it.id == selectedId }
    val latency = selected?.let { latencyResults[it.id] }

    var filter by remember { mutableStateOf(LogFilter.ALL) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var engineAvailable by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        engineAvailable = withContext(Dispatchers.Default) {
            runCatching { XrayBridge.isAvailable() }.getOrDefault(false)
        }
    }

    val visibleEvents = events
        .asReversed()
        .filter { it.matches(filter) }
        .filter {
            query.isBlank() || it.message.contains(query, ignoreCase = true)
        }

    val checks = buildDiagnosticChecks(
        state = state,
        engineAvailable = engineAvailable,
        hasProfile = selected != null,
        protocolLabel = selected?.protocol?.name,
        latencySuccess = latency?.isSuccess,
        latencyMillis = latency?.millis
    )
    val hasFailure = checks.any { it.state == CheckState.FAILED }
    // Les contrôles non testés restent neutres : seul un vrai avertissement alerte.
    val hasWarning = checks.any { it.state == CheckState.WARNING }
    val diagnosticTitle = when {
        hasFailure -> "Connexion à corriger"
        hasWarning -> "Vérification recommandée"
        else -> "Connexion stable"
    }
    val diagnosticSubtitle = when {
        hasFailure -> "Au moins un contrôle réel a échoué."
        hasWarning -> "Certains contrôles réels demandent attention."
        else -> "Tout fonctionne correctement"
    }

    fun copyLogs() {
        if (visibleEvents.isEmpty()) {
            notice = "Aucun événement à copier avec les filtres actuels."
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Journaux ONFAA TUNNEL",
                visibleEvents.joinToString("\n") { "${it.timeLabel()} [${it.severity.name}] ${it.message}" }
            )
        )
        notice = "${visibleEvents.size} événement(s) copié(s)."
    }

    fun shareLogs() {
        // Exporte exactement ce que l'utilisateur voit (filtres + recherche appliqués).
        if (visibleEvents.isEmpty()) {
            notice = "Aucun événement à exporter avec les filtres actuels."
            return
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date())
        val text = buildString {
            appendLine("ONFAA TUNNEL — journaux")
            appendLine("Version : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Date : $timestamp")
            appendLine("Événements : ${visibleEvents.size}/${events.size}")
            appendLine()
            visibleEvents.forEach { event ->
                appendLine("${event.timeLabel()} [${event.severity.name}] ${event.message}")
            }
            appendLine()
            appendLine("Aucun UUID, clé ou lien complet n'est inclus.")
        }
        shareText(context, "Journaux ONFAA TUNNEL", text)
    }

    fun shareReport() {
        val report = buildReport(state, checks, selected?.protocol?.label, events, visibleEvents)
        shareText(context, "Rapport ONFAA TUNNEL", report)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OnfaaDimens.screenHorizontal)
            .padding(top = OnfaaDimens.screenTop, bottom = 28.dp)
    ) {
        ScreenHeader(
            title = "Journaux",
            subtitle = "Logs et diagnostics",
            onBack = onBack,
            actions = {
                HeaderActionButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = "Effacer les journaux",
                    onClick = { showClearDialog = true },
                    tint = palette.danger
                )
                HeaderActionButton(
                    icon = Icons.Filled.Share,
                    contentDescription = "Exporter les journaux",
                    onClick = { shareLogs() }
                )
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogFilter.entries.forEach { option ->
                // Icônes colorées par filtre, comme sur la maquette.
                val filterIcon = when (option) {
                    LogFilter.ALL -> Icons.Filled.Article
                    LogFilter.INFO -> Icons.Filled.Info
                    LogFilter.WARNING -> Icons.Filled.Warning
                    LogFilter.ERROR -> Icons.Filled.Error
                }
                val filterTint = when (option) {
                    LogFilter.ALL -> palette.primary
                    LogFilter.INFO -> palette.secondary
                    LogFilter.WARNING -> palette.warning
                    LogFilter.ERROR -> palette.danger
                }
                OnfaaChip(
                    label = option.label,
                    selected = filter == option,
                    onClick = { filter = option },
                    leadingIcon = filterIcon,
                    iconTint = if (filter == option) null else filterTint
                )
            }
        }

        if (searchOpen) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Rechercher dans les événements…",
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
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isNotBlank()) query = "" else searchOpen = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = if (query.isBlank()) "Fermer la recherche" else "Effacer",
                            tint = palette.textMuted
                        )
                    }
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
        }

        notice?.let {
            Spacer(modifier = Modifier.height(10.dp))
            InlineNotice(title = "Journaux", message = it, color = palette.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${visibleEvents.size} événement(s)",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
            Text(
                text = "Session en mémoire · max 200",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
            if (visibleEvents.isEmpty()) {
                Text(
                    text = if (events.isEmpty()) {
                        "Aucun événement pour cette session."
                    } else {
                        "Aucun événement ne correspond au filtre ou à la recherche."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                visibleEvents.forEachIndexed { index, event ->
                    LogEventRow(event = event)
                    if (index != visibleEvents.lastIndex) {
                        DividerLine(inset = 8.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        DiagnosticCard(
            title = diagnosticTitle,
            subtitle = diagnosticSubtitle,
            checks = checks
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = palette.warning,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = "Aucune détection de fuite DNS/IP n'est implémentée : ce contrôle reste " +
                    "explicitement « non testé » et n'est jamais annoncé comme une absence de fuite.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        LogActionGrid(
            onCopy = { copyLogs() },
            onClear = { showClearDialog = true },
            onFilter = {
                val next = LogFilter.entries[(filter.ordinal + 1) % LogFilter.entries.size]
                filter = next
                notice = "Filtre appliqué : ${next.label}."
            },
            onExport = { shareLogs() },
            onReport = { shareReport() },
            onSearch = {
                searchOpen = !searchOpen
                if (!searchOpen) query = ""
            }
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Les journaux sont produits par le service réel et assainis avant affichage. " +
                "Aucun UUID, clé ou lien complet n'est journalisé. Export via le partage Android " +
                "(texte) : aucun fichier temporaire n'est créé.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted
        )
        Spacer(modifier = Modifier.height(18.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Effacer les journaux ?") },
            text = {
                Text(
                    "Les ${events.size} événement(s) de cette session seront retirés de la mémoire. " +
                        "Les diagnostics et l'historique du tunnel ne sont pas modifiés."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        TunnelStatusRepository.clearEvents()
                        showClearDialog = false
                        notice = "Journaux effacés. Le prochain événement réel du service sera ajouté."
                    }
                ) {
                    Text("Effacer", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private fun buildDiagnosticChecks(
    state: TunnelState,
    engineAvailable: Boolean?,
    hasProfile: Boolean,
    protocolLabel: String?,
    latencySuccess: Boolean?,
    latencyMillis: Long?
): List<DiagnosticCheck> {
    val engineCheck = when (engineAvailable) {
        null -> DiagnosticCheck("Moteur libXray", "Vérification…", CheckState.UNKNOWN)
        true -> DiagnosticCheck("Moteur libXray", "Disponible", CheckState.OK)
        false -> DiagnosticCheck("Moteur libXray", "Indisponible", CheckState.FAILED)
    }
    val tunnelCheck = when (state) {
        is TunnelState.Connected -> DiagnosticCheck("Tunnel", "Actif", CheckState.OK)
        is TunnelState.Error -> DiagnosticCheck("Tunnel", "Erreur réelle", CheckState.FAILED)
        TunnelState.Connecting, TunnelState.Verifying, TunnelState.Preparing ->
            DiagnosticCheck("Tunnel", "En cours", CheckState.WARNING)
        is TunnelState.Reconnecting ->
            DiagnosticCheck("Tunnel", "Reconnexion (tentative ${state.attempt})", CheckState.WARNING)
        TunnelState.Disconnecting -> DiagnosticCheck("Tunnel", "Arrêt", CheckState.WARNING)
        TunnelState.AwaitingPermission -> DiagnosticCheck("Tunnel", "Autorisation demandée", CheckState.WARNING)
        TunnelState.Idle -> DiagnosticCheck("Tunnel", "Inactif", CheckState.WARNING)
    }
    val serverCheck = when {
        !hasProfile -> DiagnosticCheck("Serveur", "Aucun configuré", CheckState.WARNING)
        else -> DiagnosticCheck("Serveur", "Configuré (${protocolLabel ?: "—"})", CheckState.OK)
    }
    val latencyCheck = when {
        !hasProfile -> DiagnosticCheck("Latence TCP", "Non testée", CheckState.UNKNOWN)
        latencySuccess == true -> DiagnosticCheck(
            "Latence TCP",
            formatLatency(latencyMillis),
            CheckState.OK
        )
        latencySuccess == false -> DiagnosticCheck(
            "Latence TCP",
            "Injoignable",
            CheckState.FAILED
        )
        else -> DiagnosticCheck("Latence TCP", "Non testée", CheckState.UNKNOWN)
    }
    val dnsCheck = DiagnosticCheck(
        "Résolveur du tunnel",
        OnfaaTunnelVpnService.DEFAULT_DNS_SERVER,
        CheckState.OK
    )
    val leakCheck = DiagnosticCheck(
        "Fuites DNS/IP",
        "Non testées",
        CheckState.UNKNOWN
    )
    return listOf(engineCheck, tunnelCheck, serverCheck, latencyCheck, dnsCheck, leakCheck)
}

private fun buildReport(
    state: TunnelState,
    checks: List<DiagnosticCheck>,
    protocolLabel: String?,
    allEvents: List<com.onfaa.tunnel.vpn.TunnelEvent>,
    visibleEvents: List<com.onfaa.tunnel.vpn.TunnelEvent>
): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date())
    val stateLabel = when (state) {
        TunnelState.Idle -> "Inactif"
        TunnelState.AwaitingPermission -> "Autorisation demandée"
        TunnelState.Preparing -> "Préparation"
        TunnelState.Connecting -> "Connexion"
        TunnelState.Verifying -> "Vérification"
        is TunnelState.Connected -> "Connecté"
        is TunnelState.Reconnecting -> "Reconnexion (tentative ${state.attempt})"
        TunnelState.Disconnecting -> "Déconnexion"
        is TunnelState.Error -> "Erreur"
    }
    val diagnostics = checks.joinToString("\n") { check ->
        "  [${check.state.name}] ${check.label} : ${check.value}"
    }
    val logs = visibleEvents.joinToString("\n") { event ->
        "${event.timeLabel()} [${event.severity.name}] ${event.message}"
    }.ifBlank { "  (aucun événement visible)" }
    return buildString {
        appendLine("ONFAA TUNNEL — rapport de diagnostic")
        appendLine("Version : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Date : $timestamp")
        appendLine("État du tunnel : $stateLabel")
        appendLine("Serveur configuré : ${if (protocolLabel == null) "non" else "oui ($protocolLabel)"}")
        appendLine()
        appendLine("Contrôles réels :")
        appendLine(diagnostics)
        appendLine()
        appendLine("Journaux affichés (${visibleEvents.size}/${allEvents.size}) :")
        appendLine(logs)
        appendLine()
        appendLine("Note : la détection de fuite DNS/IP n'est pas implémentée ;")
        appendLine("elle n'est jamais annoncée comme « aucune fuite ».")
        appendLine("Aucun UUID, clé ou lien complet n'est inclus.")
    }
}

private fun shareText(context: Context, title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, title)
    if (context !is Activity) {
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
