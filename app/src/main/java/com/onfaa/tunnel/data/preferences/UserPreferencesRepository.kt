package com.onfaa.tunnel.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.onfaa.tunnel.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeKey = prefs[THEME_KEY] ?: "system",
            selectedConfigId = prefs[SELECTED_CONFIG_ID],
            autoConnect = prefs[AUTO_CONNECT] ?: false
        )
    }

    suspend fun setThemeKey(value: String) {
        dataStore.edit { it[THEME_KEY] = value }
    }

    suspend fun setSelectedConfigId(value: String?) {
        dataStore.edit {
            if (value == null) it.remove(SELECTED_CONFIG_ID) else it[SELECTED_CONFIG_ID] = value
        }
    }

    suspend fun setAutoConnect(value: Boolean) {
        dataStore.edit { it[AUTO_CONNECT] = value }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_key")
        val SELECTED_CONFIG_ID = stringPreferencesKey("selected_config_id")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
    }
}
