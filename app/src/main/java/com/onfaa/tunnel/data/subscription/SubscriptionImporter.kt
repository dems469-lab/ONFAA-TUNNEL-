package com.onfaa.tunnel.data.subscription

import com.onfaa.tunnel.core.xray.ConfigImporter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SubscriptionImportReport(
    val url: String,
    val imported: Int,
    val failed: Int,
    val skippedLines: Int,
    val messages: List<String>
) {
    val isFullySuccessful: Boolean
        get() = failed == 0 && imported > 0
}

/**
 * Real subscription fetch and import.
 *
 * The payload is downloaded with HttpURLConnection (bounded time and size),
 * parsed by [SubscriptionParser], then every link goes through the exact same
 * [ConfigImporter] path as a manual import (structure checks + real engine
 * validation). Import never invents profiles: links that the engine refuses
 * are counted as failures with a sanitized message, nothing is silently
 * accepted.
 */
object SubscriptionImporter {

    const val MAX_BODY_BYTES = 2_000_000L
    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 20_000

    suspend fun fetchPayload(url: String): String = withContext(Dispatchers.IO) {
        val parsedUrl = try {
            URL(url)
        } catch (failure: Throwable) {
            throw SubscriptionException("URL d'abonnement invalide")
        }
        if (parsedUrl.protocol != "http" && parsedUrl.protocol != "https") {
            throw SubscriptionException("Protocole d'abonnement non pris en charge (http/https uniquement)")
        }
        var connection: HttpURLConnection? = null
        try {
            connection = (parsedUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ONFAA-TUNNEL/0.7")
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw SubscriptionException(
                    "Le serveur d'abonnement a répondu HTTP $status"
                )
            }
            val declared = connection.contentLengthLong
            if (declared > MAX_BODY_BYTES) {
                throw SubscriptionException("Abonnement trop volumineux ($declared octets)")
            }
            val body = connection.inputStream.use { stream ->
                val buffer = StringBuilder()
                val chunk = ByteArray(8_192)
                var total = 0L
                while (true) {
                    val read = stream.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_BODY_BYTES) {
                        throw SubscriptionException("Abonnement trop volumineux (> $MAX_BODY_BYTES octets)")
                    }
                    buffer.append(String(chunk, 0, read, Charsets.UTF_8))
                }
                buffer.toString()
            }
            if (body.isBlank()) {
                throw SubscriptionException("Abonnement vide")
            }
            body
        } catch (failure: SubscriptionException) {
            throw failure
        } catch (failure: Throwable) {
            throw SubscriptionException(
                "Téléchargement de l'abonnement impossible : ${failure.javaClass.simpleName}"
            )
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun import(
        url: String,
        name: String? = null,
        store: SubscriptionStore
    ): SubscriptionImportReport {
        val payload = fetchPayload(url)
        val parsed = SubscriptionParser.parse(payload)
        if (parsed.links.isEmpty()) {
            throw SubscriptionException(
                "Aucun lien exploitable dans l'abonnement" +
                    if (parsed.skipped > 0) " (${parsed.skipped} ligne(s) ignorée(s))" else ""
            )
        }
        var imported = 0
        var failed = 0
        val messages = ArrayList<String>()
        parsed.links.forEachIndexed { index, link ->
            val label = "${name ?: "Abonnement"} #${index + 1}"
            ConfigImporter.importLink(link, name = label)
                .onSuccess { imported += 1 }
                .onFailure { failure ->
                    failed += 1
                    if (messages.size < 5) {
                        messages.add("Lien ${index + 1} refusé : ${failure.message ?: "erreur inconnue"}")
                    }
                }
        }
        if (imported == 0) {
            throw SubscriptionException(
                "Aucun profil importé (${failed} lien(s) refusé(s) par le moteur)"
            )
        }
        val report = SubscriptionImportReport(
            url = url,
            imported = imported,
            failed = failed,
            skippedLines = parsed.skipped,
            messages = messages
        )
        store.upsert(
            SubscriptionSource(
                url = url,
                name = name ?: url,
                lastUpdatedEpochMs = System.currentTimeMillis(),
                lastProfileCount = imported
            )
        )
        return report
    }
}
