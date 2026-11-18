package com.onfaa.tunnel.data.subscription

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import org.json.JSONArray
import org.json.JSONObject

data class SubscriptionSource(
    val url: String,
    val name: String,
    val lastUpdatedEpochMs: Long,
    val lastProfileCount: Int
)

/**
 * Persisted list of subscription URLs (DataStore, no Room migration needed).
 * The JSON payload is small and read/written atomically; entries are keyed by
 * their URL so re-adding a URL updates it instead of duplicating it.
 */
class SubscriptionStore(private val dataStore: DataStore<Preferences>) {

    val sources: kotlinx.coroutines.flow.Flow<List<SubscriptionSource>> =
        dataStore.data.map { prefs ->
            decode(prefs[SOURCES_KEY])
        }

    suspend fun upsert(source: SubscriptionSource) {
        dataStore.edit { prefs ->
            val current = decode(prefs[SOURCES_KEY])
                .filterNot { it.url.equals(source.url, ignoreCase = true) }
            val updated = listOf(source) + current
            prefs[SOURCES_KEY] = encode(updated.take(MAX_SOURCES))
        }
    }

    suspend fun remove(url: String) {
        dataStore.edit { prefs ->
            val updated = decode(prefs[SOURCES_KEY])
                .filterNot { it.url.equals(url, ignoreCase = true) }
            if (updated.isEmpty()) {
                prefs.remove(SOURCES_KEY)
            } else {
                prefs[SOURCES_KEY] = encode(updated)
            }
        }
    }

    private fun encode(sources: List<SubscriptionSource>): String {
        val array = JSONArray()
        sources.forEach { source ->
            array.put(
                JSONObject()
                    .put(KEY_URL, source.url)
                    .put(KEY_NAME, source.name)
                    .put(KEY_UPDATED, source.lastUpdatedEpochMs)
                    .put(KEY_COUNT, source.lastProfileCount)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<SubscriptionSource> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val url = item.optString(KEY_URL).takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                SubscriptionSource(
                    url = url,
                    name = item.optString(KEY_NAME).ifBlank { url },
                    lastUpdatedEpochMs = item.optLong(KEY_UPDATED),
                    lastProfileCount = item.optInt(KEY_COUNT)
                )
            }
        } catch (failure: Throwable) {
            emptyList()
        }
    }

    private companion object {
        val SOURCES_KEY = stringPreferencesKey("subscription_sources")
        const val KEY_URL = "url"
        const val KEY_NAME = "name"
        const val KEY_UPDATED = "updated"
        const val KEY_COUNT = "count"
        const val MAX_SOURCES = 20
    }
}
