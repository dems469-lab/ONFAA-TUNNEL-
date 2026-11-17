package com.onfaa.tunnel.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFormatterTest {

    @Test
    fun roundTripsAFileLine() {
        val event = TunnelEvent(
            epochMs = 1_700_000_000_000L,
            message = "Connexion demandée",
            severity = LogSeverity.INFO
        )
        assertEquals(event, LogFormatter.parseFileLine(LogFormatter.fileLine(event)))
    }

    @Test
    fun fileLineCollapsesNewlines() {
        val line = LogFormatter.fileLine(TunnelEvent(1L, "ligne a\nligne b", LogSeverity.WARNING))
        assertEquals("1|WARNING|ligne a ligne b", line)
    }

    @Test
    fun rejectsCorruptedLines() {
        assertNull(LogFormatter.parseFileLine("sans séparateur"))
        assertNull(LogFormatter.parseFileLine("123|NIVEAU_INCONNU|message"))
        assertNull(LogFormatter.parseFileLine("pas-un-nombre|INFO|message"))
        assertNull(LogFormatter.parseFileLine("123|INFO|   "))
    }

    @Test
    fun exportMasksSecretsAndAddsHeader() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        val events = listOf(
            TunnelEvent(
                epochMs = 1_700_000_000_000L,
                message = "Lien vless://$uuid@example.com:443 refusé",
                severity = LogSeverity.ERROR
            ),
            TunnelEvent(1_700_000_001_000L, "Connexion demandée", LogSeverity.INFO)
        )

        val export = LogFormatter.export(
            events = events,
            appVersion = "0.4.0-test",
            generatedAtEpochMs = 1_700_000_002_000L
        )

        assertTrue(export.contains("# version: 0.4.0-test"))
        assertTrue(export.contains("# événements: 2"))
        assertTrue(export.contains("Connexion demandée"))
        assertTrue(export.contains("[lien-masqué]"))
        assertFalse(export, export.contains(uuid))
        assertFalse(export, export.contains("example.com"))
    }
}
