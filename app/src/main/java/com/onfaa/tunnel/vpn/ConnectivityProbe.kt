package com.onfaa.tunnel.vpn

import com.onfaa.tunnel.core.xray.XrayErrorSanitizer
import java.net.HttpURLConnection
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
}
