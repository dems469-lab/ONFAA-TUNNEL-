package com.onfaa.tunnel.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrafficSample(
    val epochMs: Long,
    val downBytesPerSec: Long,
    val upBytesPerSec: Long
)

/**
 * Real traffic counters for the running session.
 *
 * Source: android.net.TrafficStats for the application UID. The Xray proxy
 * sockets live in this process, so the deltas measure the bytes handled by the
 * engine during the session. They are not a kernel-level TUN counter; this is
 * documented in DESIGN_SPEC.md. Nothing is simulated: if the platform returns
 * UNSUPPORTED, the UI shows "indisponible".
 */
data class TrafficSnapshot(
    val available: Boolean = true,
    val active: Boolean = false,
    val downBytes: Long = 0L,
    val upBytes: Long = 0L,
    val downBytesPerSec: Long = 0L,
    val upBytesPerSec: Long = 0L,
    val samples: List<TrafficSample> = emptyList(),
    val startedAtEpochMs: Long? = null
)

object TrafficStatsRepository {
    private const val MAX_SAMPLES = 60

    private val _snapshot = MutableStateFlow(TrafficSnapshot())
    val snapshot: StateFlow<TrafficSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    fun start(atEpochMs: Long) {
        _snapshot.value = TrafficSnapshot(
            available = true,
            active = true,
            startedAtEpochMs = atEpochMs
        )
    }

    @Synchronized
    fun markUnavailable() {
        _snapshot.value = _snapshot.value.copy(available = false, active = false)
    }

    @Synchronized
    fun update(
        downBytes: Long,
        upBytes: Long,
        downBytesPerSec: Long,
        upBytesPerSec: Long,
        sampleEpochMs: Long
    ) {
        val current = _snapshot.value
        val samples = (current.samples + TrafficSample(
            epochMs = sampleEpochMs,
            downBytesPerSec = downBytesPerSec.coerceAtLeast(0L),
            upBytesPerSec = upBytesPerSec.coerceAtLeast(0L)
        )).takeLast(MAX_SAMPLES)
        _snapshot.value = current.copy(
            available = true,
            active = true,
            downBytes = downBytes.coerceAtLeast(0L),
            upBytes = upBytes.coerceAtLeast(0L),
            downBytesPerSec = downBytesPerSec.coerceAtLeast(0L),
            upBytesPerSec = upBytesPerSec.coerceAtLeast(0L),
            samples = samples
        )
    }

    @Synchronized
    fun stop() {
        _snapshot.value = _snapshot.value.copy(
            active = false,
            downBytesPerSec = 0L,
            upBytesPerSec = 0L
        )
    }

    @Synchronized
    fun clear() {
        _snapshot.value = TrafficSnapshot()
    }
}
