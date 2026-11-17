package com.onfaa.tunnel.core.xray

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigImporterValidationTest {

    @Test
    fun rejectsEmptyLink() = runBlocking {
        val error = ConfigImporter.importLink("   ").exceptionOrNull()
        assertTrue(error is ConfigException)
        assertTrue(error?.message.orEmpty().contains("vide", ignoreCase = true))
    }

    @Test
    fun rejectsUnsupportedScheme() = runBlocking {
        val error = ConfigImporter.importLink("hysteria2://secret@example.com:443").exceptionOrNull()
        assertTrue(error is ConfigException)
        assertTrue(error?.message.orEmpty().contains("non pris en charge"))
    }

    @Test
    fun rejectsLegacyBase64VmessWithoutUuid() = runBlocking {
        // {"v":"2","add":"example.com"} — decodes, then the standard structure
        // check reports the missing credential before the engine is touched.
        val error = ConfigImporter.importLink("vmess://eyJ2IjoiMiIsImFkZCI6ImV4YW1wbGUuY29tIn0=")
            .exceptionOrNull()
        assertTrue(error is ConfigException)
        assertTrue(error?.message.orEmpty().contains("UUID/identifiant"))
    }
}
