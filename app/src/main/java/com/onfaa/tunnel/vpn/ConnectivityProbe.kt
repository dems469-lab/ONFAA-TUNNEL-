package com.onfaa.tunnel.vpn

import com.onfaa.tunnel.core.xray.XrayErrorSanitizer
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * Real connectivity check issued from the application process after the TUN
 * is established. The socket is intentionally not protected, so Android routes
 * it through the VPN; success proves the tunnel actually forwards traffic.
 */
object ConnectivityProbe {
    data class Result(val ok: Boolean, val detail: String)

    fun check(url: String, timeoutMs: Int = 12_000): Result = try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            requestMethod = "GET"
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
        Result(ok, XrayErrorSanitizer.sanitize(detail))
    } catch (t: Throwable) {
        Result(false, XrayErrorSanitizer.sanitize("${t.javaClass.simpleName}: ${t.message.orEmpty()}"))
    }

    /**
     * True when a failed probe failed at the name-resolution stage instead of
     * the transport stage. Hostname resolution runs through the freshly
     * established tunnel and warms up late on slow servers, so this shape of
     * failure says nothing yet about whether the tunnel forwards packets.
     */
    fun isDnsFailure(result: Result): Boolean =
        !result.ok && DNS_FAILURE_PATTERN.containsMatchIn(result.detail)

    /**
     * Raw TCP connect to well-known IP literals, still without socket
     * protection so the connections are routed through the tunnel. No DNS is
     * involved: a success proves the engine really forwards traffic even
     * while hostname resolution is not answering yet.
     */
    fun checkIpProbe(timeoutMs: Int = 6_000): Result {
        val candidates = listOf("1.1.1.1" to 443, "8.8.8.8" to 443, "9.9.9.9" to 53)
        var lastFailure = "aucune tentative"
        for ((host, port) in candidates) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
                return Result(
                    true,
                    "TCP $host:$port connecté via le tunnel (sans DNS)"
                )
            } catch (t: Throwable) {
                lastFailure = "$host:$port → ${t.javaClass.simpleName}: ${t.message.orEmpty()}"
            }
        }
        return Result(false, XrayErrorSanitizer.sanitize("aucune destination IP joignable — $lastFailure"))
    }

    private val DNS_FAILURE_PATTERN = Regex(
        pattern = "UnknownHostException|Unable to resolve host|No address associated|NXDOMAIN|dns",
        option = RegexOption.IGNORE_CASE
    )
}
