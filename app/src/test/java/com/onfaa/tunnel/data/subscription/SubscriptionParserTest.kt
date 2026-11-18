package com.onfaa.tunnel.data.subscription

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionParserTest {

    @Test
    fun `plain link list is parsed`() {
        val raw = """
            vless://uuid@host:443?security=tls#A
            junk line without scheme
            trojan://pass@host:443#B

            socks://YWRkcg==
        """.trimIndent()
        val parsed = SubscriptionParser.parse(raw)
        assertEquals(3, parsed.links.size)
        assertEquals(1, parsed.skipped)
        assertTrue(parsed.links[0].startsWith("vless://"))
        assertTrue(parsed.links[2].startsWith("socks://"))
    }

    @Test
    fun `base64 v2ray payload is decoded`() {
        val body = "vless://uuid@host:443#A\nvmess://legacy\nss://method:pass@host:8388#C\n"
        val payload = Base64.getEncoder().encodeToString(body.toByteArray(Charsets.UTF_8))
        val parsed = SubscriptionParser.parse(payload)
        assertEquals(3, parsed.links.size)
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `url-safe base64 without padding is accepted`() {
        val body = "trojan://pass@host:443#A"
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(body.toByteArray())
        val parsed = SubscriptionParser.parse(payload)
        assertEquals(1, parsed.links.size)
    }

    @Test
    fun `empty and blank inputs yield nothing`() {
        assertEquals(0, SubscriptionParser.parse("").links.size)
        assertEquals(0, SubscriptionParser.parse("\n\n  \n").links.size)
    }

    @Test
    fun `invalid base64 without scheme marker is rejected`() {
        assertThrows(SubscriptionException::class.java) {
            SubscriptionParser.parse("!!!! not base64 and not links !!!!")
        }
    }

    @Test
    fun `only supported schemes are kept`() {
        val raw = "vless://a#1\nvmess://b#2\ntrojan://c#3\nss://d#4\nsocks://e#5\nhttp://no\nssh://no"
        val parsed = SubscriptionParser.parse(raw)
        assertEquals(5, parsed.links.size)
        assertEquals(2, parsed.skipped)
    }
}
