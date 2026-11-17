package com.onfaa.tunnel.feature.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.domain.model.UserPreferences
import com.onfaa.tunnel.onfaaAppContainer
import com.onfaa.tunnel.ui.components.CurrentThemeCard
import com.onfaa.tunnel.ui.components.SettingsNotice
import com.onfaa.tunnel.ui.components.ScreenHeader
import com.onfaa.tunnel.ui.components.ThemeModeSelector
import com.onfaa.tunnel.ui.components.ThemePreviewCard
import com.onfaa.tunnel.ui.theme.OnfaaDimens
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.theme.ThemeMode
import com.onfaa.tunnel.ui.theme.ThemePaletteId
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val palette = OnfaaTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = context.onfaaAppContainer.userPreferencesRepository
    val prefs by repository.preferences.collectAsStateWithLifecycle(
        initialValue = UserPreferences()
    )
    val selectedPalette = ThemePaletteId.fromKey(prefs.themeKey)
    val selectedMode = ThemeMode.fromKey(prefs.themeMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OnfaaDimens.screenHorizontal)
            .padding(top = OnfaaDimens.screenTop, bottom = 28.dp)
    ) {
        ScreenHeader(
            title = "Apparence",
            onBack = onBack,
            actions = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mode d'affichage",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        ThemeModeSelector(
            selected = selectedMode,
            onSelect = { mode ->
                scope.launch { repository.setThemeMode(mode.key) }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        // Maquette : libellé simple en title case (pas de section majuscules soulignée).
        Text(
            text = "Thèmes disponibles",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gap = 8.dp
            val cardWidth = (maxWidth - gap * 2) / 3
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                ThemePaletteId.entries.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        rowItems.forEach { id ->
                            ThemePreviewCard(
                                id = id,
                                selected = id == selectedPalette,
                                onClick = {
                                    scope.launch { repository.setThemeKey(id.key) }
                                },
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.width(cardWidth))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        CurrentThemeCard(id = selectedPalette, mode = selectedMode)

        Spacer(modifier = Modifier.height(16.dp))
        SettingsNotice(
            text = "7 palettes et 3 modes donnent 21 combinaisons appliquées à toute " +
                "l'application : fonds, cartes, textes, navigation, boutons, états et graphiques. " +
                "« Dark / Light » est le mode d'affichage, pas une huitième palette."
        )
    }
}
