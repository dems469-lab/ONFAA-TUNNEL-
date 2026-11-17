package com.onfaa.tunnel.feature.logs

import android.content.ClipboardManager
import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.BuildConfig
import com.onfaa.tunnel.vpn.LogSeverity
import com.onfaa.tunnel.vpn.TunnelEvent
import com.onfaa.tunnel.vpn.TunnelStatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LogFilter(val label: String) {
    ALL("Tous"),
    INFO("Info"),
    SUCCESS("Succès"),
    WARNING("Avertissement"),
    ERROR("Erreur")
}

@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val events by TunnelStatusRepository.events.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf(LogFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val visible = events
        .filter { event ->
            when (filter) {
                LogFilter.ALL -> true
                LogFilter.INFO -> event.severity == LogSeverity.INFO
                LogFilter.SUCCESS -> event.severity == LogSeverity.SUCCESS
                LogFilter.WARNING -> event.severity == LogSeverity.WARNING
                LogFilter.ERROR -> event.severity == LogSeverity.ERROR
            }
        }
        .filter { event -> query.isBlank() || event.message.contains(query, ignoreCase = true) }

    fun asText(): String = visible.joinToString("\n") { event ->
        "${event.timeLabel()}  ${event.severity.name}  ${event.message}"
    }

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val payload = TunnelStatusRepository.exportText(BuildConfig.VERSION_NAME)
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Destination inaccessible")
                }
            }
            status = saved.fold(
                onSuccess = { "Journaux exportés (données sensibles masquées)" },
                onFailure = { "Export impossible : ${it.message}" }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Journaux", style = MaterialTheme.typography.headlineSmall)
            Row {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("ONFAA TUNNEL", asText()))
                        status = "Journaux visibles copiés"
                    }
                ) {
                    Text("Copier")
                }
                TextButton(onClick = { exporter.launch("onfaa-journaux.txt") }) {
                    Text("Exporter")
                }
                TextButton(onClick = { confirmClear = true }) {
                    Text("Effacer")
                }
            }
        }
        Text(
            text = "Événements réels du tunnel (${events.size}/${TunnelStatusRepository.MAX_EVENTS}). " +
                "Horodatage, niveau et état du moteur. Les UUID, liens et secrets sont masqués avant écriture et avant export.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LogFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Filtrer le texte") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        status?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (visible.isEmpty()) {
            Text(
                text = if (events.isEmpty()) {
                    "Aucun événement pour cette session."
                } else {
                    "Aucun événement ne correspond au filtre."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(visible.asReversed()) { event ->
                    LogRow(event)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Effacer les journaux ?") },
            text = { Text("Les événements en mémoire et le fichier de journal seront supprimés.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        TunnelStatusRepository.clearEvents()
                        confirmClear = false
                        status = "Journaux effacés"
                    }
                ) {
                    Text("Effacer")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun LogRow(event: TunnelEvent) {
    val color = when (event.severity) {
        LogSeverity.ERROR -> MaterialTheme.colorScheme.error
        LogSeverity.WARNING -> Color(0xFFB26A00)
        LogSeverity.SUCCESS -> Color(0xFF1B7F3B)
        LogSeverity.INFO -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = "${event.timeLabel()}  ${event.severity.name}  ${event.message}",
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}
