package com.onfaa.tunnel.vpn

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogFileStoreInstrumentedTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val logDirectory: File
        get() = File(context.filesDir, "logs")

    @Before
    fun clearLogs() {
        logDirectory.deleteRecursively()
    }

    @After
    fun removeLogs() {
        logDirectory.deleteRecursively()
    }

    @Test
    fun persistsEventsAndSurvivesRestart() {
        val store = LogFileStore(context)
        store.append(TunnelEvent(1_000L, "premier événement", LogSeverity.INFO))
        store.append(TunnelEvent(2_000L, "tunnel vérifié", LogSeverity.SUCCESS))
        store.shutdown()

        val reopened = LogFileStore(context)
        val tail = try {
            reopened.tail()
        } finally {
            reopened.shutdown()
        }

        assertEquals(2, tail.size)
        assertEquals("premier événement", tail.first().message)
        assertEquals(LogSeverity.SUCCESS, tail.last().severity)
    }

    @Test
    fun rotatesWhenTheFileReachesItsLimit() {
        val store = LogFileStore(context, maxBytes = 180L, maxTailLines = 100)
        val events = try {
            repeat(20) { index ->
                store.append(
                    TunnelEvent(
                        epochMs = 1_000L + index,
                        message = "événement $index avec un texte assez long pour dépasser la limite",
                        severity = LogSeverity.INFO
                    )
                )
            }
            store.tail()
        } finally {
            store.shutdown()
        }

        assertTrue(events.isNotEmpty())
        assertTrue(events.size <= 100)
        assertEquals("événement 19 avec un texte assez long pour dépasser la limite", events.last().message)
        assertTrue(File(logDirectory, "${LogFileStore.FILE_NAME}.1").exists())
    }

    @Test
    fun tailIsBoundedAndClearRemovesEverything() {
        val store = LogFileStore(context, maxBytes = 1_000_000L, maxTailLines = 5)
        try {
            repeat(30) { index ->
                store.append(TunnelEvent(2_000L + index, "ligne $index", LogSeverity.INFO))
            }
            val tail = store.tail()
            assertEquals(5, tail.size)
            assertEquals("ligne 29", tail.last().message)

            store.clear()
            assertTrue(store.tail().isEmpty())
        } finally {
            store.shutdown()
        }
    }
}
