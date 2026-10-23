package com.hermeswatch.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WireCodecTest {
    private fun utf8(s: String) = s.toByteArray(Charsets.UTF_8)

    @Test
    fun ask_roundTrip_keepsCjk() {
        val req = AskRequest(requestId = "r-1", text = "今日天氣點？")
        val decoded = WireCodec.decodeAsk(WireCodec.encodeAsk(req))
        assertEquals(req, decoded)
    }

    @Test
    fun ask_wireShape_isFlatJson() {
        val s = String(WireCodec.encodeAsk(AskRequest("r-1", "hi")), Charsets.UTF_8)
        assertEquals("""{"requestId":"r-1","text":"hi"}""", s)
    }

    @Test
    fun confirm_roundTrip() {
        val req = ConfirmRequest(requestId = "r-2", id = "abc123", yes = true)
        assertEquals(req, WireCodec.decodeConfirm(WireCodec.encodeConfirm(req)))
        assertEquals(
            """{"requestId":"r-2","id":"abc123","yes":true}""",
            String(WireCodec.encodeConfirm(req), Charsets.UTF_8),
        )
    }

    @Test
    fun reply_omitsNulls() {
        val s = String(WireCodec.encodeReply(WatchReply("r", ReplyKind.ANSWER, text = "ok")), Charsets.UTF_8)
        assertEquals("""{"requestId":"r","kind":"answer","text":"ok"}""", s)
    }

    @Test
    fun reply_confirmCard_roundTrip() {
        val reply = WatchReply("r", ReplyKind.CONFIRM, text = "Run rm -rf build?", heard = "clean", id = "c1", approve = true)
        val decoded = WireCodec.decodeReply(WireCodec.encodeReply(reply))!!
        assertEquals(reply, decoded)
        assertTrue(decoded.isFinal)
    }

    @Test
    fun reply_working_isNotFinal() {
        val r = WireCodec.decodeReply(utf8("""{"requestId":"r","kind":"working"}"""))!!
        assertFalse(r.isFinal)
    }

    @Test
    fun decode_ignoresUnknownKeys() {
        val r = WireCodec.decodeReply(utf8("""{"requestId":"r","kind":"answer","text":"x","speech":"s1","future":1}"""))
        assertEquals("x", r?.text)
    }

    @Test
    fun decode_rejectsGarbageAndMissingFields() {
        assertNull(WireCodec.decodeAsk(null))
        assertNull(WireCodec.decodeAsk(ByteArray(0)))
        assertNull(WireCodec.decodeAsk(utf8("not json")))
        assertNull(WireCodec.decodeAsk(utf8("""{"text":"no id"}""")))
        assertNull(WireCodec.decodeAsk(utf8("""{"requestId":"","text":"blank id"}""")))
        assertNull(WireCodec.decodeConfirm(utf8("""{"requestId":"r","id":"","yes":true}""")))
        assertNull(WireCodec.decodeReply(utf8("""{"requestId":"r"}""")))
    }

    @Test
    fun peekRequestId_worksOnPartialPayload() {
        assertEquals("r-9", WireCodec.peekRequestId(utf8("""{"requestId":"r-9","text":5}""")))
        assertNull(WireCodec.peekRequestId(utf8("[]")))
        assertNull(WireCodec.peekRequestId(utf8("""{"requestId":7}""")))
    }
}
