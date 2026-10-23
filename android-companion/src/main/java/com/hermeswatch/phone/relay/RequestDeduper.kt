package com.hermeswatch.phone.relay

/**
 * Companion-side requestId dedupe (pure Kotlin, JVM-testable).
 *
 * The watch re-sends a request with the **same** requestId when it hears
 * nothing for 25 s. For each arrival the companion asks [onArrival]:
 * - [Decision.Start]  — first time seen → run it against the relay
 * - [Decision.InFlight] — still running → don't start a second relay call;
 *   just re-send the latest heartbeat so the watch knows we're alive
 * - [Decision.Replay] — already finished → re-send the cached final reply
 *   (the first one was probably lost over Bluetooth)
 *
 * relay.js dedupes on `rid` too, so even a killed-and-restarted companion
 * process (empty cache) won't run Hermes twice.
 */
class RequestDeduper<R : Any>(
    private val maxEntries: Int = 64,
    private val keepMs: Long = 30 * 60_000L,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    sealed interface Decision<out R> {
        data object Start : Decision<Nothing>
        data class InFlight<R>(val lastHeartbeat: R?) : Decision<R>
        data class Replay<R>(val reply: R) : Decision<R>
    }

    private class Entry<R>(var at: Long, var done: R? = null, var heartbeat: R? = null)

    // Access-ordered so the oldest entry is evicted first.
    private val entries = object : LinkedHashMap<String, Entry<R>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry<R>>?): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun onArrival(requestId: String): Decision<R> {
        prune()
        val e = entries[requestId]
        return when {
            e == null -> {
                entries[requestId] = Entry(at = clock())
                Decision.Start
            }
            e.done != null -> Decision.Replay(e.done!!)
            else -> Decision.InFlight(e.heartbeat)
        }
    }

    @Synchronized
    fun heartbeat(requestId: String, reply: R) {
        entries[requestId]?.heartbeat = reply
    }

    /** Latest heartbeat for a running request, without counting as an arrival. */
    @Synchronized
    fun latestHeartbeat(requestId: String): R? = entries[requestId]?.takeIf { it.done == null }?.heartbeat

    @Synchronized
    fun complete(requestId: String, reply: R) {
        val e = entries[requestId] ?: Entry<R>(at = clock()).also { entries[requestId] = it }
        e.done = reply
        e.at = clock()
    }

    /** Forget a request (e.g. it crashed before completing) so a retry starts fresh. */
    @Synchronized
    fun forget(requestId: String) {
        entries.remove(requestId)
    }

    @Synchronized
    fun size(): Int = entries.size

    private fun prune() {
        val now = clock()
        val it = entries.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value.at > keepMs) it.remove()
        }
    }
}
