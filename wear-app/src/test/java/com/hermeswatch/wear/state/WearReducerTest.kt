package com.hermeswatch.wear.state

import com.hermeswatch.shared.AskRequest
import com.hermeswatch.shared.ConfirmRequest
import com.hermeswatch.shared.ErrorCode
import com.hermeswatch.shared.ReplyKind
import com.hermeswatch.shared.WatchReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WearReducerTest {
    private var counter = 0
    private lateinit var reducer: WearReducer

    @Before
    fun setUp() {
        counter = 0
        reducer = WearReducer(newRequestId = { "r${++counter}" }, retryAfterMs = 25_000L, maxAttempts = 4)
    }

    private fun WearUiState.on(e: WearEvent) = reducer.reduce(this, e)
    private fun reply(rid: String, kind: String, text: String? = null, heard: String? = null, id: String? = null, approve: Boolean? = null, code: String? = null) =
        WearEvent.ReplyReceived(WatchReply(rid, kind, text, heard, id, approve, code))

    private fun waitingAfterAsk(text: String = "今日天氣點？"): WearUiState.Waiting =
        WearUiState.Talk().on(WearEvent.Ask(text)).state as WearUiState.Waiting

    @Test
    fun ask_goesToWaiting_sendsAndArms() {
        val t = WearUiState.Talk().on(WearEvent.Ask("  今日天氣點？ "))
        val w = t.state as WearUiState.Waiting
        assertEquals("r1", w.pending.requestId)
        assertEquals(Op.Ask("今日天氣點？"), w.pending.op)
        assertEquals("今日天氣點？", w.heard)
        assertEquals(
            listOf(
                WearEffect.SendAsk(AskRequest("r1", "今日天氣點？")),
                WearEffect.ArmTimeout("r1", 1, 25_000L),
            ),
            t.effects,
        )
    }

    @Test
    fun blankAsk_isIgnored() {
        val t = WearUiState.Talk().on(WearEvent.Ask("   "))
        assertEquals(WearUiState.Talk(), t.state)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun askWhileWaiting_isIgnored() {
        val w = waitingAfterAsk()
        val t = w.on(WearEvent.Ask("another"))
        assertEquals(w, t.state)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun answer_showsAnswerWithHaptic() {
        val t = waitingAfterAsk().on(reply("r1", ReplyKind.ANSWER, text = "晴，28°C", heard = "今日天氣點"))
        assertEquals(WearUiState.Answer("今日天氣點", "晴，28°C"), t.state)
        assertEquals(listOf(WearEffect.DisarmTimeout, WearEffect.Haptic(HapticKind.ANSWER)), t.effects)
    }

    @Test
    fun answerWithoutHeard_keepsTypedText() {
        val t = waitingAfterAsk("hi").on(reply("r1", ReplyKind.ANSWER, text = "hello"))
        assertEquals(WearUiState.Answer("hi", "hello"), t.state)
    }

    @Test
    fun staleReply_isIgnored() {
        val w = waitingAfterAsk()
        val t = w.on(reply("other", ReplyKind.ANSWER, text = "x"))
        assertEquals(w, t.state)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun replyAfterCancel_isIgnored() {
        val cancelled = waitingAfterAsk().on(WearEvent.Dismiss)
        assertEquals(WearUiState.Talk(), cancelled.state)
        assertEquals(listOf(WearEffect.DisarmTimeout), cancelled.effects)
        val t = cancelled.state.on(reply("r1", ReplyKind.ANSWER, text = "late"))
        assertEquals(WearUiState.Talk(), t.state)
    }

    @Test
    fun working_acksAndResetsTimer() {
        // Two silent windows → attempt 3, then a heartbeat resets to 1.
        var s: WearUiState = waitingAfterAsk()
        s = s.on(WearEvent.AttemptTimedOut("r1", 1)).state
        s = s.on(WearEvent.AttemptTimedOut("r1", 2)).state
        assertEquals(3, (s as WearUiState.Waiting).pending.attempt)
        val t = s.on(reply("r1", ReplyKind.WORKING, heard = "build the app"))
        val w = t.state as WearUiState.Waiting
        assertEquals(1, w.pending.attempt)
        assertTrue(w.phoneAcked)
        assertEquals("build the app", w.heard)
        assertEquals(listOf(WearEffect.ArmTimeout("r1", 1, 25_000L)), t.effects)
    }

    @Test
    fun timeout_resendsSameRequestId() {
        val t = waitingAfterAsk("hi").on(WearEvent.AttemptTimedOut("r1", 1))
        val w = t.state as WearUiState.Waiting
        assertEquals("r1", w.pending.requestId)
        assertEquals(2, w.pending.attempt)
        assertEquals(
            listOf(WearEffect.SendAsk(AskRequest("r1", "hi")), WearEffect.ArmTimeout("r1", 2, 25_000L)),
            t.effects,
        )
    }

    @Test
    fun staleTimeout_isIgnored() {
        val w = waitingAfterAsk()
        assertEquals(w, w.on(WearEvent.AttemptTimedOut("r1", 3)).state)
        assertEquals(w, w.on(WearEvent.AttemptTimedOut("zzz", 1)).state)
    }

    @Test
    fun maxSilentAttempts_endsInPhoneSilentError() {
        var s: WearUiState = waitingAfterAsk("hi")
        for (a in 1..3) s = s.on(WearEvent.AttemptTimedOut("r1", a)).state
        val t = s.on(WearEvent.AttemptTimedOut("r1", 4))
        assertEquals(WearUiState.Error(ErrorReason.PHONE_SILENT, retry = Op.Ask("hi")), t.state)
        assertTrue(t.effects.contains(WearEffect.DisarmTimeout))
        assertTrue(t.effects.contains(WearEffect.Haptic(HapticKind.ERROR)))
    }

    @Test
    fun retry_usesFreshRequestId() {
        val err = WearUiState.Error(ErrorReason.UNREACHABLE, retry = Op.Ask("hi"))
        val t = err.on(WearEvent.Retry)
        val w = t.state as WearUiState.Waiting
        assertEquals("r1", w.pending.requestId)
        val again = (WearUiState.Error(ErrorReason.UNREACHABLE, retry = Op.Ask("hi")).on(WearEvent.Retry).state as WearUiState.Waiting)
        assertNotEquals(w.pending.requestId, again.pending.requestId)
    }

    @Test
    fun retryWithoutOp_isNoOp() {
        val err = WearUiState.Error(ErrorReason.INPUT)
        assertEquals(err, err.on(WearEvent.Retry).state)
    }

    @Test
    fun noPhone_failsFast() {
        val t = waitingAfterAsk("hi").on(WearEvent.SendFailed("r1", noPhone = true))
        assertEquals(WearUiState.Error(ErrorReason.NO_PHONE, retry = Op.Ask("hi")), t.state)
    }

    @Test
    fun transientSendFailure_waitsForTimeout() {
        val w = waitingAfterAsk()
        assertEquals(w, w.on(WearEvent.SendFailed("r1", noPhone = false)).state)
    }

    @Test
    fun errorReply_mapsCode() {
        val t = waitingAfterAsk("hi").on(reply("r1", ReplyKind.ERROR, text = "401", code = ErrorCode.UNAUTHORIZED))
        assertEquals(WearUiState.Error(ErrorReason.UNAUTHORIZED, "401", Op.Ask("hi")), t.state)
        assertEquals(ErrorReason.NOT_CONFIGURED, WearReducer.reasonFor(ErrorCode.NOT_CONFIGURED))
        assertEquals(ErrorReason.UNREACHABLE, WearReducer.reasonFor(ErrorCode.UNREACHABLE))
        assertEquals(ErrorReason.TOO_SLOW, WearReducer.reasonFor(ErrorCode.TOO_SLOW))
        assertEquals(ErrorReason.RELAY, WearReducer.reasonFor(null))
        assertEquals(ErrorReason.RELAY, WearReducer.reasonFor("something_new"))
    }

    @Test
    fun approveCard_thenApprove_sendsConfirm_thenAnswer() {
        val card = waitingAfterAsk("deploy").on(reply("r1", ReplyKind.CONFIRM, text = "git push?", id = "c1", approve = true))
        assertEquals(WearUiState.Confirm("deploy", "git push?", "c1", approve = true), card.state)
        assertTrue(card.effects.contains(WearEffect.Haptic(HapticKind.CONFIRM)))

        val sent = card.state.on(WearEvent.ConfirmChoice(yes = true))
        val w = sent.state as WearUiState.Waiting
        assertEquals("r2", w.pending.requestId)
        assertEquals(Op.Confirm("c1", yes = true, approve = true), w.pending.op)
        assertEquals(WearEffect.SendConfirm(ConfirmRequest("r2", "c1", true)), sent.effects.first())

        val done = w.on(reply("r2", ReplyKind.ANSWER, text = "Pushed.", heard = "ignored"))
        assertEquals(WearUiState.Answer(null, "Pushed."), done.state)
    }

    @Test
    fun yesNoCard_deny() {
        val card = WearUiState.Confirm("text Sam", "Send?", "c9", approve = false)
        val t = card.on(WearEvent.ConfirmChoice(yes = false))
        assertEquals(WearEffect.SendConfirm(ConfirmRequest("r1", "c9", false)), t.effects.first())
    }

    @Test
    fun confirmWithoutId_degradesToAnswer() {
        val t = waitingAfterAsk("x").on(reply("r1", ReplyKind.CONFIRM, text = "?"))
        assertTrue(t.state is WearUiState.Answer)
    }

    @Test
    fun newChat_sendsResetPhrase_andShowsNotice() {
        val t = WearUiState.Talk().on(WearEvent.NewChat)
        assertEquals(WearEffect.SendAsk(AskRequest("r1", WearReducer.NEW_CHAT_TEXT)), t.effects.first())
        val done = t.state.on(reply("r1", ReplyKind.ANSWER, text = "New session started."))
        assertEquals(WearUiState.Talk(Notice.NEW_CHAT_STARTED), done.state)
        assertEquals(WearUiState.Talk(), done.state.on(WearEvent.ClearNotice).state)
    }

    @Test
    fun askAgainFromAnswer() {
        val t = WearUiState.Answer("a", "b").on(WearEvent.Ask("next"))
        assertTrue(t.state is WearUiState.Waiting)
    }

    @Test
    fun dismiss_fromEveryScreen_returnsToTalk() {
        listOf(
            WearUiState.Answer(null, "x"),
            WearUiState.Confirm(null, "x", "c", true),
            WearUiState.Error(ErrorReason.RELAY),
            WearUiState.Talk(Notice.NEW_CHAT_STARTED),
        ).forEach { assertEquals(WearUiState.Talk(), it.on(WearEvent.Dismiss).state) }
    }

    @Test
    fun inputUnavailable_showsInputError() {
        assertEquals(WearUiState.Error(ErrorReason.INPUT), WearUiState.Talk().on(WearEvent.InputUnavailable).state)
    }
}
