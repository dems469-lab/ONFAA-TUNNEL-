package com.onfaa.tunnel.data.config

import com.onfaa.tunnel.core.xray.ConfigImporter
import com.onfaa.tunnel.domain.model.TunnelProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 3 keeps imported profiles in memory only: no credential is written in
 * clear text to disk. Encrypted persistence is deferred to the persistence
 * phase and is documented as a limitation.
 */
object TunnelConfigRepository {
    private val _profiles = MutableStateFlow<List<TunnelProfile>>(emptyList())
    val profiles: StateFlow<List<TunnelProfile>> = _profiles.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    fun selectedProfile(): TunnelProfile? {
        val id = _selectedId.value ?: return null
        return _profiles.value.firstOrNull { it.id == id }
    }

    suspend fun import(rawLink: String): Result<TunnelProfile> =
        ConfigImporter.importLink(rawLink).onSuccess { profile ->
            val current = _profiles.value
            _profiles.value = if (current.any { it.id == profile.id }) current else current + profile
            if (_selectedId.value == null) _selectedId.value = profile.id
        }

    fun select(id: String) {
        if (_profiles.value.any { it.id == id }) _selectedId.value = id
    }

    fun remove(id: String) {
        _profiles.value = _profiles.value.filterNot { it.id == id }
        if (_selectedId.value == id) {
            _selectedId.value = _profiles.value.firstOrNull()?.id
        }
    }
}
