package com.onfaa.tunnel.data.config

import com.onfaa.tunnel.core.xray.XrayConfigBuilder
import com.onfaa.tunnel.domain.model.TunnelProfile
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class LatencyResult(
    val millis: Long? = null,
    val error: String? = null,
    val testedAtEpochMs: Long = System.currentTimeMillis()
) {
    val isSuccess: Boolean get() = millis != null
}

/**
 * Real TCP connect latency to the configured endpoint. This is a reachability
 * indicator, not an Xray handshake measurement. Nothing is simulated: an
 * unreachable server yields an error and no latency value.
 */
object LatencyRepository {
    private const val CONNECT_TIMEOUT_MS = 3_000
    private const val PARALLELISM = 4

    private val _results = MutableStateFlow<Map<String, LatencyResult>>(emptyMap())
    val results: StateFlow<Map<String, LatencyResult>> = _results.asStateFlow()

    suspend fun test(profile: TunnelProfile): LatencyResult = withContext(Dispatchers.IO) {
        val result = runCatching {
            val (host, port) = XrayConfigBuilder.splitEndpoint(profile.endpoint)
            val startedAt = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            }
            val millis = (System.nanoTime() - startedAt) / 1_000_000L
            LatencyResult(millis = millis)
        }.getOrElse {
            LatencyResult(error = "Injoignable")
        }
        _results.update { it + (profile.id to result) }
        result
    }

    suspend fun testAll(profiles: List<TunnelProfile>) {
        coroutineScope {
            profiles.chunked(PARALLELISM).forEach { batch ->
                batch.map { profile -> async { test(profile) } }.awaitAll()
            }
        }
    }

    fun fastestId(): String? =
        _results.value
            .filterValues { it.isSuccess }
            .minByOrNull { it.value.millis ?: Long.MAX_VALUE }
            ?.key

    fun clear() {
        _results.value = emptyMap()
    }
}
