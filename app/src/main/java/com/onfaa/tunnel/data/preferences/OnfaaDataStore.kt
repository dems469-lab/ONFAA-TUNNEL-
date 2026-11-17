package com.onfaa.tunnel.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore instance shared by the app (profiles selection, theme). */
val Context.onfaaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onfaa_preferences"
)
