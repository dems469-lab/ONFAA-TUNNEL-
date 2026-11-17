package com.onfaa.tunnel.vpn

import com.onfaa.tunnel.core.xray.XrayErrorSanitizer
import com.onfaa.tunnel.domain.model.TunnelProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface TunnelState {
    data object Idle : TunnelState
    data object Connecting : TunnelState
    data object Verifying : TunnelState
    data class Connected(
        val profileName: String,
        val protocol: TunnelProtocol,
        val sinceEpochMs: Long
    ) : TunnelState

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
 * Real lifecycle state published by the VpnService. Nothing writes Connected
 * before runXray succeeded and a real connectivity probe passed.
 */
object TunnelStatusRepository {
    private val _state = MutableStateFlow<TunnelState>(TunnelState.Idle)
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val _events = MutableStateFlow<List<TunnelEvent>>(emptyList())
    val events: StateFlow<List<TunnelEvent>> = _events.asStateFlow()

    @Synchronized
    fun set(newState: TunnelState) {
        _state.value = newState
    }

    @Synchronized
    fun log(message: String, severity: LogSeverity? = null) {
        val safe = XrayErrorSanitizer.sanitize(message)
        _events.value = (
            _events.value + TunnelEvent(
                epochMs = System.currentTimeMillis(),
                message = safe,
                severity = severity ?: classify(safe)
            )
        ).takeLast(200)
    }

    @Synchronized
    fun error(message: String) {
        val safe = XrayErrorSanitizer.sanitize(message)
        _state.value = TunnelState.Error(safe)
        log(safe, LogSeverity.ERROR)
    }

    fun clearEvents() {
        _events.value = emptyList()
    }

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

            listOf("report", "avertissement", "warn", "retard").any { text.contains(it) } ->
                LogSeverity.WARNING

            listOf(
                "valid", "démarr", "demarr", "connecté", "connecte", "vérifiée",
                "verifiee", "réussi", "reussi", "succès", "succes", "créée",
                "creee", "actif"
            ).any { text.contains(it) } -> LogSeverity.SUCCESS

            else -> LogSeverity.INFO
        }
    }
}
