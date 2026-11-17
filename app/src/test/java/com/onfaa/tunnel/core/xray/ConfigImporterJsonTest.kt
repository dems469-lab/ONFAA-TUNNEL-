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
        assertTrue(message, message.contains("Aucun serveur compatible"))
    }

    @Test
    fun rejectsUnsupportedProtocolAsSingleOutbound() {
        val message = rejectionOf("""{"protocol":"hysteria2","settings":{"address":"a.example","port":443}}""")
        assertTrue(message, message.contains("Aucun serveur compatible"))
    }

    @Test
    fun rejectsTrojanWithoutPassword() {
        val message = rejectionOf("""{"protocol":"trojan","settings":{"address":"a.example","port":443}}""")
        assertTrue(message, message.contains("Mot de passe Trojan"))
    }

    @Test
    fun rejectsShadowsocksWithoutMethod() {
        val message = rejectionOf(
            """{"protocol":"shadowsocks","settings":{"address":"a.example","port":8388,"password":"secret"}}"""
        )
        assertTrue(message, message.contains("Méthode de chiffrement Shadowsocks"))
    }

    @Test
    fun rejectsShadowsocksWithoutPassword() {
        val message = rejectionOf(
            """{"protocol":"shadowsocks","settings":{"address":"a.example","port":8388,"method":"aes-256-gcm"}}"""
        )
        assertTrue(message, message.contains("Mot de passe Shadowsocks"))
    }

    @Test
    fun rejectsSeveralServersInOneOutbound() {
        val message = rejectionOf(
            """
            {"protocol":"shadowsocks","settings":{"servers":[
              {"address":"a.example","port":8388,"method":"aes-256-gcm","password":"secret"},
              {"address":"b.example","port":8388,"method":"aes-256-gcm","password":"secret"}
            ]}}
            """.trimIndent()
        )
        assertTrue(message, message.contains("servers"))
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
