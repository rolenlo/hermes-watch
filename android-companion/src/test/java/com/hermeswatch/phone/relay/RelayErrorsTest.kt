package com.hermeswatch.phone.relay

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RelayErrorsTest {
    @Test
    fun mapsTimeout() {
        val msg = RelayErrors.fromThrowable(SocketTimeoutException("read timed out"))
        assertTrue(msg.contains("Timeout"))
    }

    @Test
    fun mapsConnectionRefused() {
        val msg = RelayErrors.fromThrowable(ConnectException("Connection refused"))
        assertTrue(msg.contains("Connection refused"))
    }

    @Test
    fun mapsUnknownHost() {
        val msg = RelayErrors.fromThrowable(UnknownHostException("no.such.host"))
        assertTrue(msg.contains("Unknown host"))
    }

    @Test
    fun maps401() {
        val msg = RelayErrors.http(401, """{"ok":false,"error":"bad token"}""")
        assertTrue(msg.contains("401"))
        assertTrue(msg.contains("token"))
    }

    @Test
    fun mapsNestedCause() {
        val wrapped = RuntimeException("wrap", ConnectException("Connection refused"))
        val msg = RelayErrors.fromThrowable(wrapped)
        assertTrue(msg.contains("Connection refused"))
    }
}
