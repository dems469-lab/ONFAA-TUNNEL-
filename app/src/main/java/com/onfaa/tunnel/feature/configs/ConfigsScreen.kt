package com.onfaa.tunnel.feature.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.domain.model.TunnelProfile
import kotlinx.coroutines.launch

@Composable
fun ConfigsScreen() {
    val scope = rememberCoroutineScope()
    val profiles by TunnelConfigRepository.profiles.collectAsStateWithLifecycle()
    val selectedId by TunnelConfigRepository.selectedId.collectAsStateWithLifecycle()

    var link by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Configurations", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Import réel par libXray (vless:// ou vmess://), validé par le moteur Xray avant enregistrement. " +
                "Les profils restent en mémoire uniquement : aucun identifiant n'est écrit en clair sur disque.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = link,
            onValueChange = {
                link = it
                error = null
                message = null
            },
            label = { Text("Lien VLESS ou VMess") },
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (importing) return@Button
                importing = true
                error = null
                message = null
                scope.launch {
                    val result = TunnelConfigRepository.import(link)
                    importing = false
                    result.onSuccess { profile ->
                        message = "Configuration ${profile.protocol.name} validée par le moteur."
                        link = ""
                    }.onFailure { failure ->
                        error = failure.message ?: "Import impossible"
                    }
                }
            },
            enabled = link.isNotBlank() && !importing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (importing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("  Validation par Xray…")
            } else {
                Text("Importer et valider")
            }
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Profils importés", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (profiles.isEmpty()) {
            Text(
                text = "Aucun profil. Collez un lien partagé pour l'importer.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        selected = profile.id == selectedId,
                        onSelect = { TunnelConfigRepository.select(profile.id) },
                        onDelete = { TunnelConfigRepository.remove(profile.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: TunnelProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${profile.protocol.name} — ${profile.endpoint} — sécurité ${profile.security}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onDelete) {
                Text("Supprimer")
            }
        }
    }
}
