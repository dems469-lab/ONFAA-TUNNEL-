package com.onfaa.tunnel.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.onfaa.tunnel.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val defaults = UserPreferences()
        UserPreferences(
            themeKey = prefs[THEME_KEY] ?: defaults.themeKey,
            themeMode = prefs[THEME_MODE] ?: defaults.themeMode,
            selectedConfigId = prefs[SELECTED_CONFIG_ID],
            autoConnect = prefs[AUTO_CONNECT] ?: defaults.autoConnect,
            showSpeedInNotification = prefs[SHOW_SPEED] ?: defaults.showSpeedInNotification,
            autoReconnect = prefs[AUTO_RECONNECT] ?: defaults.autoReconnect,
            connectionTimeoutSec = prefs[CONNECTION_TIMEOUT] ?: defaults.connectionTimeoutSec,
            reconnectDelaySec = prefs[RECONNECT_DELAY] ?: defaults.reconnectDelaySec,
            backgroundBehavior = prefs[BACKGROUND_BEHAVIOR] ?: defaults.backgroundBehavior,
            tlsFragment = prefs[TLS_FRAGMENT] ?: defaults.tlsFragment,
            sniEnabled = prefs[SNI_ENABLED] ?: defaults.sniEnabled,
            adBlock = prefs[AD_BLOCK] ?: defaults.adBlock,
            gamingMode = prefs[GAMING_MODE] ?: defaults.gamingMode,
            dnsOverHttps = prefs[DNS_OVER_HTTPS] ?: defaults.dnsOverHttps,
            dnsServer = prefs[DNS_SERVER] ?: defaults.dnsServer,
            routingMode = prefs[ROUTING_MODE] ?: defaults.routingMode,
            mtu = prefs[MTU] ?: defaults.mtu,
            muxEnabled = prefs[MUX_ENABLED] ?: defaults.muxEnabled,
            hotspotSharing = prefs[HOTSPOT] ?: defaults.hotspotSharing,
            logLevel = prefs[LOG_LEVEL] ?: defaults.logLevel
        )
    }

    suspend fun setThemeKey(value: String) = dataStore.edit { it[THEME_KEY] = value }

    suspend fun setThemeMode(value: String) = dataStore.edit { it[THEME_MODE] = value }

    suspend fun setSelectedConfigId(value: String?) {
        dataStore.edit {
            if (value == null) it.remove(SELECTED_CONFIG_ID) else it[SELECTED_CONFIG_ID] = value
        }
    }

    suspend fun setAutoConnect(value: Boolean) = dataStore.edit { it[AUTO_CONNECT] = value }

    suspend fun setShowSpeedInNotification(value: Boolean) =
        dataStore.edit { it[SHOW_SPEED] = value }

    suspend fun setAutoReconnect(value: Boolean) = dataStore.edit { it[AUTO_RECONNECT] = value }

    suspend fun setConnectionTimeoutSec(value: Int) =
        dataStore.edit { it[CONNECTION_TIMEOUT] = value.coerceIn(5, 120) }

    suspend fun setReconnectDelaySec(value: Int) =
        dataStore.edit { it[RECONNECT_DELAY] = value.coerceIn(1, 60) }

    suspend fun setBackgroundBehavior(value: String) =
        dataStore.edit { it[BACKGROUND_BEHAVIOR] = value }

    suspend fun setTlsFragment(value: Boolean) = dataStore.edit { it[TLS_FRAGMENT] = value }

    suspend fun setSniEnabled(value: Boolean) = dataStore.edit { it[SNI_ENABLED] = value }

    suspend fun setAdBlock(value: Boolean) = dataStore.edit { it[AD_BLOCK] = value }

    suspend fun setGamingMode(value: Boolean) = dataStore.edit { it[GAMING_MODE] = value }

    suspend fun setDnsOverHttps(value: Boolean) = dataStore.edit { it[DNS_OVER_HTTPS] = value }

    suspend fun setDnsServer(value: String) = dataStore.edit { it[DNS_SERVER] = value }

    suspend fun setRoutingMode(value: String) = dataStore.edit { it[ROUTING_MODE] = value }

    suspend fun setMtu(value: Int) = dataStore.edit { it[MTU] = value.coerceIn(1280, 9000) }

    suspend fun setMuxEnabled(value: Boolean) = dataStore.edit { it[MUX_ENABLED] = value }

    suspend fun setHotspotSharing(value: Boolean) = dataStore.edit { it[HOTSPOT] = value }

    suspend fun setLogLevel(value: String) = dataStore.edit { it[LOG_LEVEL] = value }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_key")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_CONFIG_ID = stringPreferencesKey("selected_config_id")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val SHOW_SPEED = booleanPreferencesKey("show_speed_notification")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val CONNECTION_TIMEOUT = intPreferencesKey("connection_timeout_sec")
        val RECONNECT_DELAY = intPreferencesKey("reconnect_delay_sec")
        val BACKGROUND_BEHAVIOR = stringPreferencesKey("background_behavior")
        val TLS_FRAGMENT = booleanPreferencesKey("tls_fragment")
        val SNI_ENABLED = booleanPreferencesKey("sni_enabled")
        val AD_BLOCK = booleanPreferencesKey("ad_block")
        val GAMING_MODE = booleanPreferencesKey("gaming_mode")
        val DNS_OVER_HTTPS = booleanPreferencesKey("dns_over_https")
        val DNS_SERVER = stringPreferencesKey("dns_server")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
        val MTU = intPreferencesKey("mtu")
        val MUX_ENABLED = booleanPreferencesKey("mux_enabled")
        val HOTSPOT = booleanPreferencesKey("hotspot_sharing")
        val LOG_LEVEL = stringPreferencesKey("log_level")
    }
}
