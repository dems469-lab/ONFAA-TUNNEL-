package com.onfaa.tunnel.vpn

import android.content.Context
import com.onfaa.tunnel.core.xray.XrayErrorSanitizer
import com.onfaa.tunnel.domain.model.TunnelProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface TunnelState {
    /** No tunnel, no engine. */
    data object Idle : TunnelState

    /** The system VPN consent dialog is on screen; nothing is started yet. */
    data object AwaitingPermission : TunnelState

    /** Consent granted: validating the profile and building the TUN. */
    data object Preparing : TunnelState

    /** TUN established, Xray starting. */
    data object Connecting : TunnelState

    /** Engine running, real traffic proof in progress. */
    data object Verifying : TunnelState

    data class Connected(
        val profileName: String,
        val protocol: TunnelProtocol,
        val sinceEpochMs: Long
    ) : TunnelState

    /** Connectivity lost; the tunnel is not proven again until a probe succeeds. */
    data class Reconnecting(val attempt: Int, val profileName: String) : TunnelState

    data object Disconnecting : TunnelState
    data class Error(val message: String) : TunnelState
}

enum class LogSeverity { INFO, WARNING, ERROR, SUCCESS }

data class TunnelEvent(
    val epochMs: Long,
    val message: String,
    val severity: LogSeverity = LogSeverity.INFO
) {
    fun timeLabel(): String =
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(epochMs))

    val isError: Boolean get() = severity == LogSeverity.ERROR
    val isWarning: Boolean get() = severity == LogSeverity.WARNING
    val isSuccess: Boolean get() = severity == LogSeverity.SUCCESS
}

/**
 * Real lifecycle state published by the service, plus the bounded diagnostic
 * log. Nothing writes Connected before runXray succeeded and a real
 * connectivity probe passed.
 *
 * The log is mirrored to a rotated file ([LogFileStore]) so a crash or a
 * process restart keeps the last events; the UI only keeps a bounded tail.
 */
object TunnelStatusRepository {
    const val MAX_EVENTS = 500

    private val _state = MutableStateFlow<TunnelState>(TunnelState.Idle)
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val _events = MutableStateFlow<List<TunnelEvent>>(emptyList())
    val events: StateFlow<List<TunnelEvent>> = _events.asStateFlow()

    @Volatile
    private var logStore: LogFileStore? = null

    /** Called once from Application.onCreate; safe to call twice. */
    @Synchronized
    fun attachLogFile(context: Context) {
        if (logStore != null) return
        val store = LogFileStore(context)
        logStore = store
        val persisted = store.tail()
        if (persisted.isNotEmpty()) {
            _events.value = (persisted + _events.value)
                .distinctBy { it.epochMs to it.message }
                .takeLast(MAX_EVENTS)
        }
    }

    @Synchronized
    fun set(newState: TunnelState) {
        _state.value = newState
    }

    @Synchronized
    fun log(message: String, severity: LogSeverity? = null) {
        val safe = XrayErrorSanitizer.sanitize(message)
        val event = TunnelEvent(
            epochMs = System.currentTimeMillis(),
            message = safe,
            severity = severity ?: classify(safe)
        )
        _events.value = (_events.value + event).takeLast(MAX_EVENTS)
        logStore?.append(event)
    }

    @Synchronized
    fun error(message: String) {
        val safe = XrayErrorSanitizer.sanitize(message)
        _state.value = TunnelState.Error(safe)
        log(safe, LogSeverity.ERROR)
    }

    @Synchronized
    fun clearEvents() {
        _events.value = emptyList()
        logStore?.clear()
    }

    /** Sanitized plain-text export (clipboard or user-chosen file). */
    fun exportText(appVersion: String): String =
        LogFormatter.export(_events.value, appVersion = appVersion)

    /**
     * Severity is inferred from the real message produced by the service; no
     * event is fabricated. Explicit severity can be passed by callers.
     */
    private fun classify(message: String): LogSeverity {
        val text = message.lowercase(Locale.ROOT)
        return when {
            listOf(
                "erreur", "échec", "echec", "echou", "échou", "impossible",
                "refus", "invalide", "non vérifiée", "non verifiee",
                "indisponible", "révoqué", "revoque"
            ).any { text.contains(it) } -> LogSeverity.ERROR

            listOf("report", "avertissement", "warn", "retard", "perdu").any { text.contains(it) } ->
                LogSeverity.WARNING

            listOf(
                "valid", "démarr", "demarr", "connecté", "connecte", "vérifiée",
                "verifiee", "réussi", "reussi", "succès", "succes", "créée",
                "creee", "actif", "rétabli", "retabli"
            ).any { text.contains(it) } -> LogSeverity.SUCCESS

            else -> LogSeverity.INFO
        }
    }
}
