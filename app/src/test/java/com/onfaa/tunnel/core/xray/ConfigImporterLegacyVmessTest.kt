package com.onfaa.tunnel.core.xray

import java.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The historic vmess://base64(JSON) share format is decoded by the importer
 * itself and rebuilt as a single vmess outbound. On a plain JVM test run the
 * native engine is unavailable, so a payload that passes every structure and
 * credential check always ends on the "engine unavailable" error: reaching
 * that message proves the whole decode-and-rebuild pipeline worked. Corrupted
 * or incomplete payloads must be reported with a precise message instead.
 */
class ConfigImporterLegacyVmessTest {

    private fun encodedLink(json: String, urlSafe: Boolean = false): String {
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        return "vmess://" + if (urlSafe) {
            encoded.replace('+', '-').replace('/', '_').trimEnd('=')
        } else {
            encoded
        }
    }

    private fun errorOf(link: String): String = runBlocking {
        val error = ConfigImporter.importLink(link).exceptionOrNull()
        assertTrue("expected a ConfigException, got $error", error is ConfigException)
        error?.message.orEmpty()
    }

    private val fullJson = """
        {"v":"2","ps":"ONFAA Paris","add":"v.example.net","port":"443",
         "id":"b831381d-6324-4d53-ad4f-8cda48b30811","aid":"0","scy":"auto",
         "net":"ws","host":"v.example.net","path":"/ws","tls":"tls","sni":"v.example.net"}
    """.trimIndent().replace("\n", "")

    @Test
    fun validLegacyVmessPassesStructureChecksAndReachesEngine() {
        val message = errorOf(encodedLink(fullJson))
        assertTrue(message, message.contains("libXray indisponible"))
    }

    @Test
    fun urlSafeAlphabetWithoutPaddingDecodesToo() {
        val message = errorOf(encodedLink(fullJson, urlSafe = true))
        assertTrue(message, message.contains("libXray indisponible"))
    }

    @Test
    fun reportsMissingUuid() {
        val json = """{"v":"2","add":"v.example.net","port":"443"}"""
        val message = errorOf(encodedLink(json))
        assertTrue(message, message.contains("UUID/identifiant"))
    }

    @Test
    fun reportsInvalidPort() {
        val json = """{"v":"2","add":"v.example.net","port":"70000","id":"b831381d-6324-4d53-ad4f-8cda48b30811"}"""
        val message = errorOf(encodedLink(json))
        assertTrue(message, message.contains("Port du serveur invalide"))
    }

    @Test
    fun reportsMissingAddress() {
        val json = """{"v":"2","port":"443","id":"b831381d-6324-4d53-ad4f-8cda48b30811"}"""
        val message = errorOf(encodedLink(json))
        assertTrue(message, message.contains("Adresse du serveur manquante"))
    }

    @Test
    fun reportsCorruptedBase64() {
        val message = errorOf("vmess://@@@ceci-n-est-pas-du-base64@@@")
        assertTrue(message, message.contains("base64", ignoreCase = true))
    }

    @Test
    fun reportsNonJsonPayload() {
        val encoded = Base64.getEncoder().encodeToString("céci n'est pas du json".toByteArray())
        val message = errorOf("vmess://$encoded")
        assertTrue(message, message.contains("JSON", ignoreCase = true))
    }
}
