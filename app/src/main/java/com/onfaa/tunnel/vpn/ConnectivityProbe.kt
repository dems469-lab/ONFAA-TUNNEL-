package com.onfaa.tunnel.vpn

import com.onfaa.tunnel.core.xray.XrayErrorSanitizer
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Real connectivity check issued from the application process after the TUN
 * is established. The socket is intentionally not protected, so Android routes
 * it through the VPN; success proves the tunnel actually forwards traffic.
 *
 * Every check runs behind a hard wall-clock bound: a blocked DNS lookup can
 * outlive HttpURLConnection's connect/read timeouts (they do not cover name
 * resolution), which used to freeze the verification phase with no log line.
 * The blocking call now runs on a disposable daemon thread and the caller
 * gives up when the bound expires, reporting a readable timeout instead.
 */
object ConnectivityProbe {

    enum class FailureKind { NONE, DNS, TIMEOUT, TRANSPORT }

    data class Result(
        val ok: Boolean,
        val detail: String,
        val kind: FailureKind = FailureKind.NONE
    )

    /**
     * Neutral, plain-HTTP targets (port 80) used when no explicit probe URL is
     * supplied. HTTP avoids TLS-handshake failures on censored or captive
     * networks and every endpoint answers 200/204 with a tiny body. They are
     * tried in order, so one blocked endpoint does not decide the verdict.
     */
    val NEUTRAL_TARGETS: List<String> = listOf(
        "http://connectivitycheck.gstatic.com/generate_204",
        "http://cp.cloudflare.com/generate_204",
        "http://www.msftconnecttest.com/connecttest.txt"
    )

    fun check(url: String, timeoutMs: Int = 12_000): Result =
        withHardDeadline(timeoutMs.toLong() + HARD_GRACE_MS) { probeOnce(url, timeoutMs) }

    /**
     * Tries [targets] in order and returns the first success. When everything
     * fails, the last result is returned (its detail names the last target).
     */
    fun checkAny(targets: List<String>, timeoutMsPerTarget: Int = 8_000): Result {
        var last = Result(false, "aucune cible configurée", FailureKind.TRANSPORT)
        for (target in targets) {
            last = check(target, timeoutMsPerTarget)
            if (last.ok) return last
        }
        return last
    }

    private fun probeOnce(url: String, timeoutMs: Int): Result = try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ONFAA-TUNNEL")
            useCaches = false
        }
        val code = connection.responseCode
        val body = if (code in 200..399) {
            connection.inputStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        } else {
            ""
        }
        connection.disconnect()
        val ok = code in 200..399
        val detail = buildString {
            append("HTTP ").append(code)
            val trimmed = body.trim()
            if (trimmed.isNotEmpty()) append(" — ").append(trimmed.take(120))
        }
        Result(ok, XrayErrorSanitizer.sanitize(detail), if (ok) FailureKind.NONE else FailureKind.TRANSPORT)
    } catch (t: Throwable) {
        val raw = "${t.javaClass.simpleName}: ${t.message.orEmpty()}"
        Result(
            ok = false,
            detail = XrayErrorSanitizer.sanitize(raw),
            kind = classifyFailure(raw)
        )
    }

    /**
     * True when a failed probe failed at the name-resolution stage instead of
     * the transport stage. Hostname resolution runs through the freshly
     * established tunnel and warms up late on slow servers, so this shape of
     * failure says nothing yet about whether the tunnel forwards packets.
     */
    fun isDnsFailure(result: Result): Boolean = !result.ok && (
        result.kind == FailureKind.DNS ||
            classifyFailure(result.detail) == FailureKind.DNS
        )

    /** Pure, unit-tested classification of a raw failure description. */
    fun classifyFailure(raw: String): FailureKind = when {
        DNS_FAILURE_PATTERN.containsMatchIn(raw) -> FailureKind.DNS
        raw.contains("Timeout", ignoreCase = true) ||
            raw.contains("timed out", ignoreCase = true) -> FailureKind.TIMEOUT
        else -> FailureKind.TRANSPORT
    }

    /**
     * Raw TCP connect to well-known IP literals, still without socket
     * protection so the connections are routed through the tunnel. No DNS is
     * involved: a success proves the engine really forwards traffic even
     * while hostname resolution is not answering yet.
     */
    fun checkIpProbe(timeoutMs: Int = 6_000): Result = withHardDeadline(
        (timeoutMs * IP_PROBE_CANDIDATES.size).toLong() + HARD_GRACE_MS
    ) {
        var lastFailure = "aucune tentative"
        for ((host, port) in IP_PROBE_CANDIDATES) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
                return@withHardDeadline Result(
                    true,
                    "TCP $host:$port connecté via le tunnel (sans DNS)"
                )
            } catch (t: Throwable) {
                lastFailure = "$host:$port → ${t.javaClass.simpleName}: ${t.message.orEmpty()}"
            }
        }
        Result(
            false,
            XrayErrorSanitizer.sanitize("aucune destination IP joignable — $lastFailure"),
            FailureKind.TRANSPORT
        )
    }

    /**
     * Runs [block] and guarantees the caller is never blocked longer than
     * [boundMs], even when a blocking DNS lookup ignores socket timeouts.
     * The worker is a daemon thread: a stuck resolution cannot keep the
     * process alive.
     */
    private fun withHardDeadline(boundMs: Long, block: () -> Result): Result {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "onfaa-connectivity-probe").apply { isDaemon = true }
        }
        return try {
            val future = executor.submit(java.util.concurrent.Callable { block() })
            try {
                future.get(boundMs.coerceAtLeast(1_000L), TimeUnit.MILLISECONDS)
            } catch (timeout: TimeoutException) {
                future.cancel(true)
                Result(
                    false,
                    "délai dur de $boundMs ms dépassé (résolution DNS figée ?)",
                    FailureKind.TIMEOUT
                )
            } catch (failure: ExecutionException) {
                val cause = failure.cause ?: failure
                val raw = "${cause.javaClass.simpleName}: ${cause.message.orEmpty()}"
                Result(false, XrayErrorSanitizer.sanitize(raw), classifyFailure(raw))
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private const val HARD_GRACE_MS = 1_500L

    private val IP_PROBE_CANDIDATES = listOf("1.1.1.1" to 443, "8.8.8.8" to 443, "9.9.9.9" to 53)

    private val DNS_FAILURE_PATTERN = Regex(
        pattern = "UnknownHostException|Unable to resolve host|No address associated|NXDOMAIN|dns",
        option = RegexOption.IGNORE_CASE
    )
}
