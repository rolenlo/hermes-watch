package com.hermeswatch.phone.relay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestDeduperTest {
    private var now = 0L
    private fun deduper(max: Int = 64, keep: Long = 1_000L) =
        RequestDeduper<String>(maxEntries = max, keepMs = keep, clock = { now })

    @Test
    fun firstArrivalStarts_retryWhileRunningIsInFlight() {
        val d = deduper()
        assertEquals(RequestDeduper.Decision.Start, d.onArrival("a"))
        assertEquals(RequestDeduper.Decision.InFlight<String>(null), d.onArrival("a"))
        d.heartbeat("a", "working")
        assertEquals(RequestDeduper.Decision.InFlight("working"), d.onArrival("a"))
        assertEquals("working", d.latestHeartbeat("a"))
    }

    @Test
    fun completedRequestReplaysCachedReply() {
        val d = deduper()
        d.onArrival("a")
        d.complete("a", "answer")
        assertEquals(RequestDeduper.Decision.Replay("answer"), d.onArrival("a"))
        assertNull(d.latestHeartbeat("a"))
    }

    @Test
    fun differentIdsAreIndependent() {
        val d = deduper()
        d.onArrival("a")
        assertEquals(RequestDeduper.Decision.Start, d.onArrival("b"))
    }

    @Test
    fun entriesExpire() {
        val d = deduper(keep = 1_000L)
        d.onArrival("a")
        d.complete("a", "answer")
        now = 5_000L
        assertEquals(RequestDeduper.Decision.Start, d.onArrival("a"))
    }

    @Test
    fun boundedSize_evictsOldest() {
        val d = deduper(max = 2, keep = Long.MAX_VALUE)
        d.onArrival("a"); d.onArrival("b"); d.onArrival("c")
        assertTrue(d.size() <= 2)
        assertEquals(RequestDeduper.Decision.Start, d.onArrival("a"))
    }

    @Test
    fun forgetAllowsFreshStart() {
        val d = deduper()
        d.onArrival("a")
        d.forget("a")
        assertEquals(RequestDeduper.Decision.Start, d.onArrival("a"))
    }
}
