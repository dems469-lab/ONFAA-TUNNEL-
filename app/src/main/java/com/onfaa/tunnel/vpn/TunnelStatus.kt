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

data class TunnelEvent(
    val epochMs: Long,
    val message: String
) {
    fun timeLabel(): String =
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(epochMs))
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
    fun log(message: String) {
        val safe = XrayErrorSanitizer.sanitize(message)
        _events.value = (_events.value + TunnelEvent(System.currentTimeMillis(), safe)).takeLast(200)
    }

    @Synchronized
    fun error(message: String) {
        val safe = XrayErrorSanitizer.sanitize(message)
        _state.value = TunnelState.Error(safe)
        log(safe)
    }

    fun clearEvents() {
        _events.value = emptyList()
    }
}
