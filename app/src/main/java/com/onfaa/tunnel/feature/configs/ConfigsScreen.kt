@file:OptIn(ExperimentalFoundationApi::class)

package com.onfaa.tunnel.feature.configs

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.data.config.LatencyRepository
import com.onfaa.tunnel.data.config.LatencyResult
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.ui.components.Chevron
import com.onfaa.tunnel.ui.components.GlassCard
import com.onfaa.tunnel.ui.components.GradientButton
import com.onfaa.tunnel.ui.components.HeaderActionButton
import com.onfaa.tunnel.ui.components.InlineNotice
import com.onfaa.tunnel.ui.components.OnfaaChip
import com.onfaa.tunnel.ui.components.ProtocolBadge
import com.onfaa.tunnel.ui.components.ScreenHeader
import com.onfaa.tunnel.ui.components.StatusBadge
import com.onfaa.tunnel.ui.components.TonalButton
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.util.formatLatency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ConfigFilter(val label: String) {
    ALL("Tous"),
    FAVORITES("Favoris"),
    VLESS("VLESS"),
    VMESS("VMess")
}

private enum class ConfigSort(val label: String) {
    RECENT("Import récent"),
    NAME("Nom (A–Z)"),
    LATENCY("Latence"),
    FAVORITES_FIRST("Favoris d'abord")
}

@Composable
fun ConfigsScreen(onBack: () -> Unit) {
    val palette = OnfaaTheme.colors
    val scope = rememberCoroutineScope()
    val profiles by TunnelConfigRepository.profiles.collectAsStateWithLifecycle()
    val selectedId by TunnelConfigRepository.selectedId.collectAsStateWithLifecycle()
    val favoriteIds by TunnelConfigRepository.favoriteIds.collectAsStateWithLifecycle()
    val latencyResults by LatencyRepository.results.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ConfigFilter.ALL) }
    var sort by remember { mutableStateOf(ConfigSort.RECENT) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }
    var detailProfile by remember { mutableStateOf<TunnelProfile?>(null) }
    var deleteCandidate by remember { mutableStateOf<TunnelProfile?>(null) }
    var renameTarget by remember { mutableStateOf<TunnelProfile?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var testingAll by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val visible = profiles
        .filter { profile ->
            val matchesFilter = when (filter) {
                ConfigFilter.ALL -> true
                ConfigFilter.FAVORITES -> profile.id in favoriteIds
                ConfigFilter.VLESS -> profile.protocol.name == "VLESS"
                ConfigFilter.VMESS -> profile.protocol.name == "VMESS"
            }
            val matchesQuery = query.isBlank() ||
                profile.displayName.contains(query, ignoreCase = true) ||
                profile.endpoint.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        .let { list ->
            when (sort) {
                ConfigSort.RECENT -> list.reversed()
                ConfigSort.NAME -> list.sortedBy { it.displayName.lowercase() }
                ConfigSort.LATENCY -> list.sortedWith(
                    compareBy<TunnelProfile>(
                        { latencyResults[it.id]?.millis == null },
                        { latencyResults[it.id]?.millis ?: Long.MAX_VALUE }
                    )
                )
                ConfigSort.FAVORITES_FIRST -> list.sortedWith(
                    compareByDescending<TunnelProfile> { it.id in favoriteIds }
                        .thenBy { it.displayName.lowercase() }
                )
            }
        }

    fun runLatencyTests(onFinished: (String?) -> Unit = {}) {
        if (testingAll || profiles.isEmpty()) return
        testingAll = true
        notice = null
        error = null
        scope.launch {
            LatencyRepository.testAll(profiles)
            testingAll = false
            onFinished(LatencyRepository.fastestId())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OnfaaDimens.screenHorizontal)
            .padding(top = OnfaaDimens.screenTop, bottom = 28.dp)
    ) {
        ScreenHeader(
            title = "Configs",
            onBack = onBack,
            actions = {
                Box {
                    HeaderActionButton(
                        icon = Icons.Filled.SwapVert,
                        contentDescription = "Trier",
                        onClick = { sortMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false }
                    ) {
                        ConfigSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                leadingIcon = {
                                    if (sort == option) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = palette.primary
                                        )
                                    }
                                },
                                onClick = {
                                    sort = option
                                    sortMenuOpen = false
                                }
                            )
                        }
                    }
                }
                // Maquette : bouton d'ajout bleu plein avec « + » blanc.
                HeaderActionButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Importer une configuration",
                    onClick = { importOpen = true },
                    filled = true
                )
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Rechercher une configuration…",
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OnfaaChip(
                label = ConfigFilter.ALL.label,
                selected = filter == ConfigFilter.ALL,
                onClick = { filter = ConfigFilter.ALL }
            )
            OnfaaChip(
                label = ConfigFilter.FAVORITES.label,
                selected = filter == ConfigFilter.FAVORITES,
                onClick = { filter = ConfigFilter.FAVORITES },
                leadingIcon = Icons.Filled.Star
            )
            OnfaaChip(
                label = ConfigFilter.VLESS.label,
                selected = filter == ConfigFilter.VLESS,
                onClick = { filter = ConfigFilter.VLESS }
            )
            OnfaaChip(
                label = ConfigFilter.VMESS.label,
                selected = filter == ConfigFilter.VMESS,
                onClick = { filter = ConfigFilter.VMESS }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConfigActionButton(
                icon = Icons.Filled.Shield,
                label = if (testingAll) "Test en cours…" else "Tester toutes\nles configs",
                onClick = {
                    runLatencyTests { fastest ->
                        notice = if (fastest == null) {
                            "Aucun serveur joignable pendant le test."
                        } else {
                            "Test terminé. Le serveur le plus rapide est prêt à être sélectionné."
                        }
                    }
                },
                enabled = profiles.isNotEmpty() && !testingAll,
                modifier = Modifier.weight(1f)
            )
            ConfigActionButton(
                icon = Icons.Filled.Bolt,
                label = "Sélectionner\nle plus rapide",
                onClick = {
                    runLatencyTests { fastest ->
                        if (fastest != null) {
                            TunnelConfigRepository.select(fastest)
                            notice = "Le serveur le plus rapide a été sélectionné."
                        } else {
                            error = "Aucun serveur joignable : impossible de sélectionner le plus rapide."
                        }
                    }
                },
                enabled = profiles.isNotEmpty() && !testingAll,
                modifier = Modifier.weight(1f)
            )
        }

        if (testingAll) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = palette.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connexions TCP réelles vers les serveurs…",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }

        notice?.let {
            Spacer(modifier = Modifier.height(10.dp))
            InlineNotice(title = "Information", message = it, color = palette.primary)
        }
        error?.let {
            Spacer(modifier = Modifier.height(10.dp))
            InlineNotice(title = "Action impossible", message = it, color = palette.danger)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isEmpty()) {
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aucune configuration importée",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Importez un lien VLESS/VMess ou un fichier texte. " +
                            "Le moteur Xray valide la configuration avant de l'ajouter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "Importer maintenant",
                        icon = Icons.Filled.Add,
                        onClick = { importOpen = true }
                    )
                }
            }
        } else if (visible.isEmpty()) {
            GlassCard {
                Text(
                    text = "Aucune configuration ne correspond à la recherche ou au filtre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visible.forEach { profile ->
                    ServerCard(
                        profile = profile,
                        selected = profile.id == selectedId,
                        favorite = profile.id in favoriteIds,
                        latency = latencyResults[profile.id],
                        onClick = { detailProfile = profile },
                        onLongClick = { detailProfile = profile },
                        onSwipeLeft = { detailProfile = profile },
                        onToggleFavorite = { TunnelConfigRepository.toggleFavorite(profile.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = null,
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Glissez vers la gauche pour plus d'options, appui long pour le menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }

    if (importOpen) {
        ImportConfigDialog(
            onDismiss = { importOpen = false },
            onImported = { imported ->
                importOpen = false
                notice = "Configuration ${imported.protocol.name} validée et ajoutée."
                error = null
            }
        )
    }

    detailProfile?.let { profile ->
        ServerDetailDialog(
            profile = profile,
            selected = profile.id == selectedId,
            favorite = profile.id in favoriteIds,
            latency = latencyResults[profile.id],
            onDismiss = { detailProfile = null },
            onSelect = {
                TunnelConfigRepository.select(profile.id)
                detailProfile = null
            },
            onToggleFavorite = { TunnelConfigRepository.toggleFavorite(profile.id) },
            onTest = {
                scope.launch {
                    val result = LatencyRepository.test(profile)
                    notice = if (result.isSuccess) {
                        "Latence TCP mesurée : ${formatLatency(result.millis)}."
                    } else {
                        "Serveur injoignable."
                    }
                }
            },
            onDelete = {
                detailProfile = null
                deleteCandidate = profile
            },
            onRename = {
                detailProfile = null
                renameTarget = profile
                renameValue = profile.displayName
                renameError = null
            }
        )
    }

    deleteCandidate?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Supprimer la configuration ?") },
            text = {
                Text(
                    "« ${profile.displayName} » sera retiré de la liste et du stockage local. " +
                        "Cette action est définitive."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        TunnelConfigRepository.remove(profile.id)
                        deleteCandidate = null
                    }
                ) {
                    Text("Supprimer", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Annuler")
                }
            }
        )
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
                            color = palette.danger,
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
                                    notice = "Profil renommé en « $newName »."
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
private fun ConfigActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = OnfaaTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(
                palette.surfaceElevated.copy(alpha = if (enabled) 0.94f else 0.45f),
                shape
            )
            .border(1.dp, palette.border.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    palette.primary.copy(alpha = if (enabled) 0.14f else 0.06f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primary.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textPrimary.copy(alpha = if (enabled) 1f else 0.45f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ServerCard(
    profile: TunnelProfile,
    selected: Boolean,
    favorite: Boolean,
    latency: LatencyResult?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeLeft: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = OnfaaTheme.colors
    var dragTotal by remember(profile.id) { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(OnfaaDimens.cardRadius)
    val borderColor = if (selected) palette.primary.copy(alpha = 0.75f) else palette.border.copy(alpha = 0.55f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        palette.surfaceElevated.copy(alpha = 0.92f),
                        palette.surface
                    )
                ),
                shape = shape
            )
            .border(if (selected) 1.5.dp else 1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .pointerInput(profile.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        dragTotal += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (dragTotal < -56f) onSwipeLeft()
                    }
                )
            }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProtocolBadge(protocol = profile.protocol, compact = true)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.width(7.dp))
                        StatusBadge(
                            text = "Sélectionnée",
                            color = palette.primary,
                            icon = Icons.Filled.CheckCircle,
                            filled = true
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.endpoint,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                LatencyLine(latency)
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (favorite) "Retirer des favoris" else "Ajouter aux favoris",
                    tint = if (favorite) palette.warning else palette.textMuted
                )
            }
            Chevron()
        }
    }
}

@Composable
private fun LatencyLine(latency: LatencyResult?) {
    val palette = OnfaaTheme.colors
    val color = when {
        latency == null -> palette.textMuted
        latency.isSuccess -> palette.success
        else -> palette.danger
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = when {
                latency == null -> "Latence : non testée"
                latency.isSuccess -> "Latence : ${formatLatency(latency.millis)}"
                else -> "Latence : serveur injoignable"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun ServerDetailDialog(
    profile: TunnelProfile,
    selected: Boolean,
    favorite: Boolean,
    latency: LatencyResult?,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    val palette = OnfaaTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProtocolBadge(protocol = profile.protocol)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer",
                        tint = palette.textMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            DetailLine("Point de terminaison", profile.endpoint, monospace = true)
            DetailLine("Sécurité", profile.security)
            DetailLine(
                "Latence TCP",
                when {
                    latency == null -> "Non testée"
                    latency.isSuccess -> formatLatency(latency.millis)
                    else -> "Serveur injoignable"
                }
            )
            DetailLine("État", if (selected) "Sélectionné" else "Non sélectionné")
            Spacer(modifier = Modifier.height(14.dp))
            GradientButton(
                text = if (selected) "Serveur sélectionné" else "Sélectionner ce serveur",
                icon = if (selected) Icons.Filled.CheckCircle else Icons.Filled.Check,
                onClick = onSelect,
                enabled = !selected
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalButton(
                    text = if (favorite) "Favori" else "Ajouter aux favoris",
                    icon = if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    onClick = onToggleFavorite,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                TonalButton(
                    text = "Tester",
                    icon = Icons.Filled.Shield,
                    onClick = onTest,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TonalButton(
                text = "Renommer",
                icon = Icons.Filled.Edit,
                onClick = onRename,
                compact = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TonalButton(
                text = "Supprimer la configuration",
                icon = Icons.Filled.Delete,
                onClick = onDelete,
                compact = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Le détail ne révèle jamais l'UUID, les clés ou le JSON interne.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, monospace: Boolean = false) {
    val palette = OnfaaTheme.colors
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = palette.textPrimary
        )
    }
}

@Composable
private fun ImportConfigDialog(
    onDismiss: () -> Unit,
    onImported: (TunnelProfile) -> Unit
) {
    val palette = OnfaaTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var qrScanning by remember { mutableStateOf(false) }

    fun import(text: String) {
        if (text.isBlank() || importing) return
        importing = true
        error = null
        scope.launch {
            val result = TunnelConfigRepository.import(text)
            importing = false
            result.onSuccess { profile ->
                onImported(profile)
            }.onFailure { failure ->
                error = failure.message ?: "Import impossible"
            }
        }
    }

    fun pasteFromClipboard() {
        if (importing) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.let { clip ->
            (0 until clip.itemCount).firstNotNullOfOrNull { index ->
                clip.getItemAt(index)?.coerceToText(context)
            }
        }?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            error = "Le presse-papiers est vide ou ne contient pas de texte."
            return
        }
        // Direct import: the clipboard content is validated immediately and
        // the field keeps the value so a failure can be corrected in place.
        link = text
        import(text)
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            error = null
            val extracted = withContext(Dispatchers.IO) {
                readTextFile(context, uri)
            }
            importing = false
            if (extracted.isNullOrBlank()) {
                error = "Le fichier est vide ou illisible."
            } else {
                fileName = uri.lastPathSegment
                link = extracted.trim()
                import(extracted)
            }
        }
    }

    Dialog(onDismissRequest = { if (!importing) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Importer une configuration",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, enabled = !importing) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer",
                        tint = palette.textMuted
                    )
                }
            }
            Text(
                text = "Collez un lien VLESS, VMess, Trojan, Shadowsocks ou SOCKS5, " +
                    "scannez un QR code ou choisissez un fichier. La configuration est " +
                    "validée par le moteur Xray avant l'ajout.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = link,
                onValueChange = {
                    link = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                label = { Text("Lien ou configuration Xray") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border,
                    cursorColor = palette.primary,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )
            )
            fileName?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Fichier : $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted
                )
            }
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.danger
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            GradientButton(
                text = if (importing) "Validation par Xray…" else "Importer et valider",
                icon = Icons.Filled.Add,
                loading = importing,
                enabled = link.isNotBlank() && !importing,
                onClick = { import(link) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalButton(
                    text = "Coller",
                    icon = Icons.Filled.ContentPaste,
                    onClick = { pasteFromClipboard() },
                    enabled = !importing,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                TonalButton(
                    text = "Fichier",
                    icon = Icons.Filled.FolderOpen,
                    onClick = {
                        filePicker.launch(
                            arrayOf("text/*", "application/json", "application/octet-stream")
                        )
                    },
                    enabled = !importing,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
                TonalButton(
                    text = "QR",
                    icon = Icons.Filled.QrCode2,
                    onClick = { qrScanning = true },
                    enabled = !importing,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Les profils sont conservés localement (Room) ; les identifiants sont " +
                    "chiffrés (AES-GCM, Keystore) et jamais affichés ni journalisés.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }
    }

    if (qrScanning) {
        QrScanDialog(
            onResult = { text ->
                qrScanning = false
                fileName = null
                link = text
                import(text)
            },
            onDismiss = { qrScanning = false }
        )
    }
}

private fun readTextFile(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val bytes = stream.readBytes()
        if (bytes.size > 64 * 1024) return@use null
        bytes.toString(Charsets.UTF_8)
    }
}.getOrNull()
