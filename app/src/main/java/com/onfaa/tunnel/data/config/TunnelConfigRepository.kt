package com.onfaa.tunnel.data.config

import android.content.Context
import com.onfaa.tunnel.core.security.SecretCipher
import com.onfaa.tunnel.core.xray.ConfigException
import com.onfaa.tunnel.core.xray.ConfigImporter
import com.onfaa.tunnel.data.local.db.OnfaaDatabase
import com.onfaa.tunnel.data.local.db.ProfileDao
import com.onfaa.tunnel.data.local.db.ProfileEntity
import com.onfaa.tunnel.data.preferences.UserPreferencesRepository
import com.onfaa.tunnel.data.preferences.onfaaDataStore
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.domain.model.TunnelProtocol
import com.onfaa.tunnel.vpn.LogSeverity
import com.onfaa.tunnel.vpn.TunnelStatusRepository
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Room-backed profile store.
 *
 * Profiles survive process death and reboots: display fields live in Room, the
 * engine outbound (UUID/keys) is stored as an AES-GCM envelope produced by
 * [SecretCipher] with a key held by the Android Keystore. The selected profile
 * id is persisted in DataStore so the home screen comes back on the same
 * server.
 *
 * The API stays synchronous for reads (StateFlow mirrors) and for the legacy
 * call sites (select/remove/toggleFavorite) so the UI and the VpnService keep
 * working without waiting on disk: those calls update the in-memory state
 * immediately and persist in the background.
 */
object TunnelConfigRepository {
    private const val MAX_NAME_LENGTH = 48

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialized = AtomicBoolean(false)

    @Volatile
    private var dao: ProfileDao? = null

    @Volatile
    private var preferences: UserPreferencesRepository? = null

    /** Selection written by the user but not yet observed back from DataStore. */
    @Volatile
    private var pendingSelection: String? = null

    private val unreadableIds = HashSet<String>()

    private val _profiles = MutableStateFlow<List<TunnelProfile>>(emptyList())
    val profiles: StateFlow<List<TunnelProfile>> = _profiles.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _ready = MutableStateFlow(false)

    /** True once the first Room emission has been published. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val database = OnfaaDatabase.build(appContext)
        val store = database.profileDao()
        val userPreferences = UserPreferencesRepository(appContext.onfaaDataStore)
        dao = store
        preferences = userPreferences

        scope.launch {
            combine(
                store.observeAll(),
                userPreferences.preferences.map { it.selectedConfigId }.distinctUntilChanged()
            ) { rows, selection -> rows to selection }
                .collect { (rows, selection) -> publish(rows, selection) }
        }
    }

    /** Safe to call from the service: the Application normally did it first. */
    fun ensureInitialized(context: Context) = initialize(context)

    fun selectedProfile(): TunnelProfile? {
        val id = _selectedId.value ?: return null
        return _profiles.value.firstOrNull { it.id == id }
    }

    suspend fun import(rawLink: String): Result<TunnelProfile> = importInput(rawLink)

    /**
     * Imports a share link, a single outbound or a full Xray JSON config,
     * validates it with the real engine, then persists it encrypted.
     */
    suspend fun importInput(raw: String, name: String? = null): Result<TunnelProfile> = try {
        val imported = ConfigImporter.importText(raw, name).getOrThrow()
        Result.success(withContext(Dispatchers.IO) { persist(imported, name) })
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    suspend fun rename(id: String, newName: String): Result<Unit> = try {
        val cleaned = sanitizeName(newName)
            ?: throw ConfigException("Le nom du profil ne peut pas être vide")
        val store = storage()
        val updated = withContext(Dispatchers.IO) {
            store.rename(id, cleaned, System.currentTimeMillis())
        }
        if (updated == 0) throw ConfigException("Profil introuvable")
        _profiles.value = _profiles.value.map {
            if (it.id == id) it.copy(displayName = cleaned) else it
        }
        Result.success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    fun select(id: String) {
        if (_profiles.value.none { it.id == id }) return
        if (_selectedId.value == id) return
        _selectedId.value = id
        pendingSelection = id
        scope.launch { preferences?.setSelectedConfigId(id) }
    }

    fun remove(id: String) {
        val remaining = _profiles.value.filterNot { it.id == id }
        _profiles.value = remaining
        val nextSelection = if (_selectedId.value == id) remaining.firstOrNull()?.id else _selectedId.value
        _selectedId.value = nextSelection
        pendingSelection = nextSelection
        scope.launch {
            dao?.deleteById(id)
            preferences?.setSelectedConfigId(nextSelection)
        }
    }

    fun toggleFavorite(id: String) {
        if (_profiles.value.none { it.id == id }) return
        val favorite = id !in _favoriteIds.value
        _favoriteIds.value = if (favorite) _favoriteIds.value + id else _favoriteIds.value - id
        scope.launch { dao?.setFavorite(id, favorite, System.currentTimeMillis()) }
    }

    private suspend fun persist(profile: TunnelProfile, requestedName: String?): TunnelProfile {
        val store = storage()
        val cipher = try {
            SecretCipher.encrypt(profile.outboundJson)
        } catch (failure: SecretCipher.SecretUnavailable) {
            throw ConfigException(
                "Impossible de protéger la configuration sur cet appareil : " +
                    (failure.message ?: "Keystore indisponible")
            )
        }

        val existing = store.findById(profile.id)
        val now = System.currentTimeMillis()
        val name = sanitizeName(requestedName) ?: existing?.name ?: profile.displayName
        store.upsert(
            ProfileEntity(
                id = profile.id,
                name = name,
                protocol = profile.protocol.name,
                endpoint = profile.endpoint,
                security = profile.security,
                payloadCipher = cipher,
                isFavorite = existing?.isFavorite ?: false,
                notes = existing?.notes,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        val stored = profile.copy(displayName = name, incomplete = false)
        val current = _profiles.value
        _profiles.value = if (current.any { it.id == stored.id }) {
            current.map { if (it.id == stored.id) stored else it }
        } else {
            current + stored
        }
        if (_selectedId.value == null) {
            _selectedId.value = stored.id
            pendingSelection = stored.id
            preferences?.setSelectedConfigId(stored.id)
        }
        return stored
    }

    private fun publish(rows: List<ProfileEntity>, storedSelection: String?) {
        val profiles = rows.mapNotNull { row -> row.toProfile() }
        _profiles.value = profiles
        _favoriteIds.value = rows.filter { it.isFavorite }.map { it.id }.toSet()

        val desired = pendingSelection ?: storedSelection
        val selection = desired?.takeIf { id -> profiles.any { it.id == id } }
            ?: profiles.firstOrNull()?.id
        _selectedId.value = selection
        if (pendingSelection != null && pendingSelection == storedSelection) {
            pendingSelection = null
        }
        if (selection != null && selection != storedSelection) {
            scope.launch { preferences?.setSelectedConfigId(selection) }
        }
        _ready.value = true
    }

    private fun ProfileEntity.toProfile(): TunnelProfile? {
        val protocol = runCatching {
            TunnelProtocol.valueOf(protocol.uppercase(Locale.ROOT))
        }.getOrNull() ?: return null

        var payload = ""
        var incomplete = true
        if (payloadCipher.isNotBlank()) {
            payload = try {
                SecretCipher.decrypt(payloadCipher)
            } catch (_: Throwable) {
                if (unreadableIds.add(id)) {
                    TunnelStatusRepository.log(
                        "Profil « $name » illisible : clé de chiffrement absente ou modifiée, " +
                            "réimportez la configuration",
                        LogSeverity.ERROR
                    )
                }
                ""
            }
            incomplete = payload.isBlank()
        }

        return TunnelProfile(
            id = id,
            displayName = name,
            protocol = protocol,
            endpoint = endpoint,
            security = security,
            outboundJson = payload,
            incomplete = incomplete
        )
    }

    private fun storage(): ProfileDao =
        dao ?: throw ConfigException("Stockage local non initialisé")

    private fun sanitizeName(raw: String?): String? = raw
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.take(MAX_NAME_LENGTH)
        ?.takeIf { it.isNotEmpty() }
}
