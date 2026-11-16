package com.onfaa.tunnel.domain.model

/** Basic user preferences prepared for DataStore-backed storage. */
data class UserPreferences(
    val themeKey: String = "system",
    val selectedConfigId: String? = null,
    val autoConnect: Boolean = false
)
