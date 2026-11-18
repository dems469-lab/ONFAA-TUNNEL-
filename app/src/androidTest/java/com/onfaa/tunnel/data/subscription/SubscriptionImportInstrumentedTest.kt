package com.onfaa.tunnel.data.subscription

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onfaa.tunnel.core.xray.XrayBridge
import com.onfaa.tunnel.onfaaAppContainer
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real subscription import end-to-end on the Android runtime: a local HTTP
 * server serves a base64 v2ray-style payload, the fetch + parse + engine
 * validation pipeline runs for real, and a refused HTTP status produces a
 * readable failure. libXray availability is required for the import path.
 *
 * The real application SubscriptionStore (the app's own DataStore file) is
 * reused; test-runner data is ephemeral on CI and no other consumer reads
 * these entries, so the writes are side-effect free for the tests.
 */
@RunWith(AndroidJUnit4::class)
class SubscriptionImportInstrumentedTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val store get() = context.onfaaAppContainer.subscriptionStore

    private var server: ServerSocket? = null

    @Before
    fun setUp() {
        server?.close()
        server = null
        runBlocking { runCatching { store.remove("http://127.0.0.1/cleanup") } }
    }

    private fun serve(body: String, status: String = "200 OK") {
        val socket = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        server = socket
        val payload = body.toByteArray(Charsets.UTF_8)
        val thread = Thread {
            runCatching {
                while (!socket.isClosed) {
                    val client = socket.accept() ?: break
                    handle(client, status, payload)
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    private fun handle(socket: Socket, status: String, payload: ByteArray) {
        runCatching {
            socket.use { client ->
                val reader = client.getInputStream().bufferedReader()
                while (reader.readLine()?.isNotBlank() == true) {
                    // Drain the request head.
                }
                client.getOutputStream().apply {
                    write(
                        ("HTTP/1.1 $status\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: ${payload.size}\r\n" +
                            "Connection: close\r\n\r\n").toByteArray(Charsets.UTF_8)
                    )
                    write(payload)
                    flush()
                }
            }
        }
    }

    private fun base64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun requireLib() {
        assumeTrue(
            "libXray AAR not bundled (app/libs). Skipping real-artifact test.",
            XrayBridge.isAvailable()
        )
    }

    @Test
    fun importsBase64SubscriptionWithRealEngineValidation() = runBlocking {
        requireLib()
        val links = listOf(
            "trojan://onfaa-trojan-pass@example.com:443?security=tls&sni=example.com&type=tcp#Sub%20Trojan",
            "ss://${Base64.getUrlEncoder().withoutPadding().encodeToString(
                "aes-256-gcm:onfaa-ss-pass".toByteArray()
            )}@example.com:8388#Sub%20SS",
            "definitely-not-a-link"
        ).joinToString("\n")
        serve(base64(links))
        val report = SubscriptionImporter.import(
            url = "http://127.0.0.1:${server!!.localPort}/sub",
            name = "CI Abonnement",
            store = store
        )
        assertEquals(2, report.imported)
        assertEquals(0, report.failed)
        assertEquals(1, report.skippedLines)
        val sources = store.sources.first()
        assertEquals(1, sources.size)
        assertEquals("CI Abonnement", sources[0].name)
        assertEquals(2, sources[0].lastProfileCount)
        store.remove(sources[0].url)
    }

    @Test
    fun plainSubscriptionListIsAccepted() = runBlocking {
        requireLib()
        serve("trojan://onfaa-trojan-pass@example.com:443?security=tls&sni=example.com&type=tcp#Sub%20Trojan\n")
        val report = SubscriptionImporter.import(
            url = "http://127.0.0.1:${server!!.localPort}/sub",
            store = store
        )
        assertEquals(1, report.imported)
        assertEquals(0, report.failed)
        store.remove("http://127.0.0.1:${server!!.localPort}/sub")
    }

    @Test
    fun httpErrorProducesReadableFailure() = runBlocking {
        serve("not found body", status = "404 Not Found")
        try {
            SubscriptionImporter.import("http://127.0.0.1:${server!!.localPort}/sub", store = store)
            fail("HTTP 404 must fail the import")
        } catch (failure: SubscriptionException) {
            assertTrue(failure.message.orEmpty().contains("404"))
        }
    }

    @Test
    fun subscriptionWithoutUsableLinksIsRejected() = runBlocking {
        requireLib()
        serve("only junk lines\nno schemes here")
        assertThrows(SubscriptionException::class.java) {
            runBlocking {
                SubscriptionImporter.import("http://127.0.0.1:${server!!.localPort}/sub", store = store)
            }
        }
        Unit
    }
}
