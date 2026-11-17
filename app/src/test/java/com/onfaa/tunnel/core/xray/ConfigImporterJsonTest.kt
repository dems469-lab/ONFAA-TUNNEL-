package com.onfaa.tunnel.core.xray

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Input validation of the JSON import path. Every case below is rejected
 * before the native engine is touched, so it runs as a plain JVM test and
 * proves that no invalid configuration is accepted silently.
 */
class ConfigImporterJsonTest {

    private fun rejectionOf(raw: String): String = runBlocking {
        val error = ConfigImporter.importText(raw).exceptionOrNull()
        assertTrue("expected a ConfigException, got $error", error is ConfigException)
        error?.message.orEmpty()
    }

    @Test
    fun rejectsMalformedJson() {
        val message = rejectionOf("""{"outbounds": [""")
        assertTrue(message, message.contains("JSON invalide", ignoreCase = true))
    }

    @Test
    fun rejectsEmptyInput() {
        val message = rejectionOf("   ")
        assertTrue(message, message.contains("vide", ignoreCase = true))
    }

    @Test
    fun rejectsJsonWithoutVlessOrVmessOutbound() {
        val message = rejectionOf("""{"outbounds":[{"protocol":"freedom","tag":"direct"}]}""")
        assertTrue(message, message.contains("Aucun serveur VLESS/VMess"))
    }

    @Test
    fun rejectsUnsupportedProtocolAsSingleOutbound() {
        val message = rejectionOf("""{"protocol":"trojan","settings":{"address":"a.example","port":443}}""")
        assertTrue(message, message.contains("Aucun serveur VLESS/VMess"))
    }

    @Test
    fun rejectsSeveralSupportedOutbounds() {
        val message = rejectionOf(
            """
            {"outbounds":[
              {"protocol":"vless","settings":{"address":"a.example","port":443,"id":"a"}},
              {"protocol":"vmess","settings":{"address":"b.example","port":443,"id":"b"}}
            ]}
            """.trimIndent()
        )
        assertTrue(message, message.contains("2 sorties"))
    }

    @Test
    fun rejectsMissingCredential() {
        val message = rejectionOf("""{"protocol":"vless","settings":{"address":"a.example","port":443}}""")
        assertTrue(message, message.contains("identifiant", ignoreCase = true))
    }

    @Test
    fun rejectsInvalidPort() {
        val message = rejectionOf("""{"protocol":"vless","settings":{"address":"a.example","port":70000,"id":"u"}}""")
        assertTrue(message, message.contains("Port du serveur invalide"))
    }

    @Test
    fun rejectsMissingAddress() {
        val message = rejectionOf("""{"protocol":"vmess","settings":{"port":443,"id":"u"}}""")
        assertTrue(message, message.contains("Adresse du serveur manquante"))
    }
}
