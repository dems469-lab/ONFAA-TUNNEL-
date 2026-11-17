package com.onfaa.tunnel.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Classification of probe failures. A DNS-only failure must never be treated
 * as a dead tunnel: the IP fallback decides whether the engine forwards
 * traffic. Plain JVM test — no Android framework involved.
 */
class ConnectivityProbeTest {

    @Test
    fun classifiesUnknownHostAsDnsFailure() {
        val result = ConnectivityProbe.Result(
            false,
            "UnknownHostException: Unable to resolve host \"www.gstatic.com\": " +
                "No address associated with hostname"
        )
        assertTrue(ConnectivityProbe.isDnsFailure(result))
    }

    @Test
    fun successfulProbeIsNeverADnsFailure() {
        assertFalse(ConnectivityProbe.isDnsFailure(ConnectivityProbe.Result(true, "HTTP 204")))
    }

    @Test
    fun transportFailureIsNotADnsFailure() {
        val result = ConnectivityProbe.Result(false, "HTTP 502 — Bad Gateway")
        assertFalse(ConnectivityProbe.isDnsFailure(result))
    }
}
