package com.onfaa.tunnel.core.xray

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proof-of-concept tests against the real libXray AAR artifact.
 * These tests require the verified libXray AAR to be bundled in app/libs.
 * When not bundled, tests are skipped (reported as assumptions not met).
 */
@RunWith(AndroidJUnit4::class)
class XrayBridgeInstrumentedTest {

    private fun requireLib(): String {
        assumeTrue("libXray AAR not bundled (app/libs). Skipping real-artifact test.", XrayBridge.isAvailable())
        return XrayBridge.resolvedSignature() ?: "unknown"
    }

    @Test
    fun nativeLibraryAndInvokeApiAvailable() {
        requireLib()
        // Forces class initialization: gomobile's static initializer loads libgojni.so.
        assertTrue("native Go runtime must load", XrayBridge.loadNativeLibrary())
        val sig = XrayBridge.resolvedSignature()
        assertNotNull(sig)
        assertTrue(sig!!.contains("invoke", ignoreCase = true))
    }

    @Test
    fun xrayVersionReturnsValidJsonAndSuccess() = runBlocking {
        requireLib()
        val response = XrayBridge.xrayVersion()
        assertTrue("raw response should be JSON object: ${response.raw}", response.raw.trim().startsWith("{"))
        assertTrue("xrayVersion should succeed: ${response.error}", response.success)
        val version = response.data?.optString("version").orEmpty()
        assertTrue("version should not be empty", version.isNotBlank())
    }

    @Test
    fun invalidRequestIsHandledAsErrorResponse() = runBlocking {
        requireLib()
        // Missing method/apiVersion contract violation -> should return structured error, not crash.
        val response = XrayBridge.invokeParsed("{\"apiVersion\":3}")
        assertFalse("invalid request should not be successful", response.success)
        assertTrue("error message expected", response.error.isNotBlank())
        assertTrue(response.raw.contains("success"))
    }

    @Test
    fun unknownMethodReturnsStructuredError() = runBlocking {
        requireLib()
        val response = XrayBridge.invokeParsed(XrayBridge.buildRequest("notARealMethod"))
        assertFalse(response.success)
        assertTrue(response.error.isNotBlank())
    }

    @Test
    fun testXrayRejectsInvalidConfigWithoutCrash() = runBlocking {
        requireLib()
        val payload = JSONObject().put("xrayJson", "not-json")
        val response = XrayBridge.invoke("testXray", payload)
        // Should fail gracefully (success=false with error), never crash process.
        assertFalse("invalid config must not succeed", response.success)
        assertNotNull(response.raw)
        assertEquals(false, response.success)
    }
}
