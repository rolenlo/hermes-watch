package com.hermeswatch.phone.relay

import com.hermeswatch.shared.ErrorCode
import com.hermeswatch.shared.ReplyKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException

class ReplyMapperTest {
    private fun resp(
        ok: Boolean = true,
        kind: String? = null,
        text: String? = null,
        heard: String? = null,
        id: String? = null,
        job: String? = null,
        approve: Boolean? = null,
        error: String? = null,
        note: String? = null,
    ) = AskResponse(ok, heard, kind, text, id, job, approve, error, note, rawJson = "{}")

    @Test
    fun answer() {
        val w = ReplyMapper.toWatch("r", resp(kind = "answer", text = "It's 3pm", heard = "what time"))
        assertEquals(ReplyKind.ANSWER, w.kind)
        assertEquals("It's 3pm", w.text)
        assertEquals("what time", w.heard)
        assertNull(w.id)
    }

    @Test
    fun approveCard() {
        val w = ReplyMapper.toWatch("r", resp(kind = "confirm", text = "git push?", id = "c1", approve = true))
        assertEquals(ReplyKind.CONFIRM, w.kind)
        assertEquals("c1", w.id)
        assertEquals(true, w.approve)
    }

    @Test
    fun yesNoCard_approveFalse() {
        val w = ReplyMapper.toWatch("r", resp(kind = "confirm", text = "Send text?", id = "c2"))
        assertEquals(false, w.approve)
    }

    @Test
    fun confirmWithoutId_degradesToAnswer() {
        val w = ReplyMapper.toWatch("r", resp(kind = "confirm", text = "?"))
        assertEquals(ReplyKind.ANSWER, w.kind)
    }

    @Test
    fun relayOkFalse_isError() {
        val w = ReplyMapper.toWatch("r", resp(ok = false, error = "Speech-to-text isn't running on the PC."))
        assertEquals(ReplyKind.ERROR, w.kind)
        assertEquals(ErrorCode.RELAY, w.errorCode)
    }

    @Test
    fun working_isHeartbeat() {
        val w = ReplyMapper.toWatch("r", resp(kind = "working", job = "j1", heard = "build it"))
        assertEquals(ReplyKind.WORKING, w.kind)
        assertFalse(w.isFinal)
        assertEquals("build it", w.heard)
    }

    @Test
    fun wireErrors_codes() {
        assertEquals(ErrorCode.UNAUTHORIZED, WireErrors.codeFor(RelayHttpException(401, "bad token")))
        assertEquals(ErrorCode.RELAY, WireErrors.codeFor(RelayHttpException(500, "boom")))
        assertEquals(ErrorCode.UNREACHABLE, WireErrors.codeFor(SocketTimeoutException()))
        assertEquals(ErrorCode.UNREACHABLE, WireErrors.codeFor(RuntimeException(ConnectException())))
        assertEquals(ErrorCode.NOT_CONFIGURED, WireErrors.codeFor(IllegalArgumentException("Base URL is empty.")))
    }
}
