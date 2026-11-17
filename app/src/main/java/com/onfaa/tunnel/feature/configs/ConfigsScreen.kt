package com.onfaa.tunnel.feature.configs

import android.content.ClipboardManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.domain.model.TunnelProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_IMPORT_BYTES = 256 * 1024

@Composable
fun ConfigsScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profiles by TunnelConfigRepository.profiles.collectAsStateWithLifecycle()
    val selectedId by TunnelConfigRepository.selectedId.collectAsStateWithLifecycle()
    val favorites by TunnelConfigRepository.favoriteIds.collectAsStateWithLifecycle()

    var link by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<TunnelProfile?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }

    fun runImport(raw: String) {
        if (importing) return
        importing = true
        error = null
        message = null
        scope.launch {
            val result = TunnelConfigRepository.importInput(
                raw = raw,
                name = profileName.trim().takeIf { it.isNotEmpty() }
            )
            importing = false
            result.onSuccess { profile ->
                message = "Profil « ${profile.displayName} » (${profile.protocol.name}) validé et enregistré"
                link = ""
                profileName = ""
            }.onFailure { failure ->
                error = failure.message ?: "Import impossible"
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        error = null
        message = null
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val buffer = ByteArray(MAX_IMPORT_BYTES + 1)
                        var read = 0
                        while (read < buffer.size) {
                            val count = stream.read(buffer, read, buffer.size - read)
                            if (count <= 0) break
                            read += count
                        }
                        if (read > MAX_IMPORT_BYTES) {
                            throw IllegalStateException("Fichier trop volumineux (256 Ko maximum)")
                        }
                        String(buffer, 0, read, Charsets.UTF_8)
                    } ?: throw IllegalStateException("Fichier illisible")
                }
            }
            importing = false
            content.onSuccess { runImport(it) }
                .onFailure { error = it.message ?: "Lecture du fichier impossible" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Configurations", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Import d'un lien vless:// ou vmess://, du presse-papiers, d'un fichier ou " +
                "d'une configuration Xray JSON. Chaque profil est validé par le moteur Xray, " +
                "chiffré (AES-GCM, clé Android Keystore) puis enregistré dans Room.",
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
            label = { Text("Lien VLESS / VMess ou JSON Xray") },
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = profileName,
            onValueChange = { profileName = it },
            label = { Text("Nom du profil (optionnel)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { runImport(link) },
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
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val text = clipboard?.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        ?.trim()
                    if (text.isNullOrEmpty()) {
                        error = "Presse-papiers vide"
                    } else {
                        runImport(text)
                    }
                },
                enabled = !importing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Presse-papiers")
            }
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                enabled = !importing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Fichier")
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
        Text(text = "Profils enregistrés", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (profiles.isEmpty()) {
            Text(
                text = "Aucun profil enregistré. Importez un lien ou un fichier pour commencer.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileRow(
                        profile = profile,
                        selected = profile.id == selectedId,
                        favorite = profile.id in favorites,
                        onSelect = { TunnelConfigRepository.select(profile.id) },
                        onFavorite = { TunnelConfigRepository.toggleFavorite(profile.id) },
                        onRename = {
                            renameTarget = profile
                            renameValue = profile.displayName
                            renameError = null
                        },
                        onDelete = { TunnelConfigRepository.remove(profile.id) }
                    )
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renommer le profil") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = {
                            renameValue = it
                            renameError = null
                        },
                        label = { Text("Nom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    renameError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameValue.trim()
                        if (newName.isEmpty()) {
                            renameError = "Le nom ne peut pas être vide"
                            return@TextButton
                        }
                        scope.launch {
                            TunnelConfigRepository.rename(target.id, newName)
                                .onSuccess {
                                    renameTarget = null
                                    message = "Profil renommé en « $newName »"
                                }
                                .onFailure {
                                    renameError = it.message ?: "Renommage impossible"
                                }
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: TunnelProfile,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onRename: () -> Unit,
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
                if (profile.incomplete) {
                    Text(
                        text = "Configuration incomplète : réimportez le lien ou le JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (favorite) "Retirer des favoris" else "Ajouter aux favoris"
                )
            }
            IconButton(onClick = onRename) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Renommer")
            }
            TextButton(onClick = onDelete) {
                Text("Supprimer")
            }
        }
    }
}
