package com.hermeswatch.phone.relay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayUrlTest {
    @Test
    fun normalize_trimsAndStripsSlash() {
        val r = RelayUrl.normalize("  https://pc.tailnet.ts.net:8453/  ")
        assertTrue(r.isSuccess)
        assertEquals("https://pc.tailnet.ts.net:8453", r.getOrThrow())
    }

    @Test
    fun normalize_rejectsBlank() {
        assertTrue(RelayUrl.normalize("   ").isFailure)
    }

    @Test
    fun normalize_rejectsNonHttp() {
        assertTrue(RelayUrl.normalize("ftp://pc").isFailure)
        assertTrue(RelayUrl.normalize("pc.tailnet.ts.net:8453").isFailure)
    }

    @Test
    fun normalize_allowsHttpIp() {
        val r = RelayUrl.normalize("http://100.64.0.1:8453")
        assertEquals("http://100.64.0.1:8453", r.getOrThrow())
    }

    @Test
    fun join_paths() {
        assertEquals(
            "https://pc.ts.net:8453/health",
            RelayUrl.join("https://pc.ts.net:8453/", "/health"),
        )
        assertEquals(
            "https://pc.ts.net:8453/watch/ask",
            RelayUrl.join("https://pc.ts.net:8453", "watch/ask"),
        )
    }
}
