package com.hermeswatch.wear.state

import com.hermeswatch.shared.AskRequest
import com.hermeswatch.shared.ConfirmRequest
import com.hermeswatch.shared.ErrorCode
import com.hermeswatch.shared.ReplyKind
import com.hermeswatch.shared.WatchReply

/**
 * The watch state machine: `(state, event) → (state, effects)`. Pure and
 * deterministic (request ids come from [newRequestId]).
 *
 * Retry design mirrors upstream zepp-app (`POLL_STUCK_MS = 25000`): if nothing
 * arrives for [retryAfterMs] the same request is re-sent **with the same
 * requestId** (companion + relay dedupe, so Hermes runs once). Any `working`
 * heartbeat from the phone resets the attempt counter, so a slow Hermes answer
 * never exhausts retries; only [maxAttempts] silent windows in a row end in
 * [ErrorReason.PHONE_SILENT].
 */
class WearReducer(
    private val newRequestId: () -> String,
    val retryAfterMs: Long = RETRY_AFTER_MS,
    val maxAttempts: Int = MAX_ATTEMPTS,
) {
    fun reduce(state: WearUiState, event: WearEvent): Transition = when (event) {
        is WearEvent.Ask -> {
            val text = event.text.trim()
            if (text.isEmpty() || !canStart(state)) Transition(state) else start(Op.Ask(text), heard = text)
        }

        WearEvent.NewChat -> if (canStart(state)) start(Op.NewChat, heard = null) else Transition(state)

        is WearEvent.ConfirmChoice -> if (state is WearUiState.Confirm) {
            start(Op.Confirm(state.cardId, event.yes, state.approve), heard = null)
        } else {
            Transition(state)
        }

        WearEvent.Dismiss -> when (state) {
            is WearUiState.Talk -> Transition(state.copy(notice = null))
            is WearUiState.Waiting -> Transition(WearUiState.Talk(), listOf(WearEffect.DisarmTimeout))
            else -> Transition(WearUiState.Talk())
        }

        WearEvent.Retry -> {
            val op = (state as? WearUiState.Error)?.retry
            if (op == null) Transition(state) else start(op, heard = (op as? Op.Ask)?.text)
        }

        WearEvent.ClearNotice ->
            if (state is WearUiState.Talk && state.notice != null) Transition(state.copy(notice = null)) else Transition(state)

        WearEvent.InputUnavailable ->
            if (state is WearUiState.Talk) {
                Transition(WearUiState.Error(ErrorReason.INPUT), listOf(WearEffect.Haptic(HapticKind.ERROR)))
            } else {
                Transition(state)
            }

        is WearEvent.ReplyReceived -> onReply(state, event.reply)

        is WearEvent.AttemptTimedOut -> onTimeout(state, event)

        is WearEvent.SendFailed -> {
            val w = state as? WearUiState.Waiting
            if (w != null && w.pending.requestId == event.requestId && event.noPhone) {
                fail(ErrorReason.NO_PHONE, null, w.pending.op)
            } else {
                // Transient send error: the armed timeout will resend.
                Transition(state)
            }
        }
    }

    private fun canStart(state: WearUiState) = state is WearUiState.Talk || state is WearUiState.Answer

    private fun start(op: Op, heard: String?): Transition {
        val pending = Pending(requestId = newRequestId(), op = op, attempt = 1)
        return Transition(
            WearUiState.Waiting(pending, heard = heard),
            listOf(sendEffect(pending), WearEffect.ArmTimeout(pending.requestId, 1, retryAfterMs)),
        )
    }

    private fun onReply(state: WearUiState, reply: WatchReply): Transition {
        val w = state as? WearUiState.Waiting
        // Stale (cancelled / superseded / duplicate) replies are dropped.
        if (w == null || w.pending.requestId != reply.requestId) return Transition(state)
        val heard = reply.heard?.takeIf { it.isNotBlank() } ?: w.heard
        return when (reply.kind) {
            ReplyKind.WORKING -> Transition(
                w.copy(pending = w.pending.copy(attempt = 1), heard = heard, phoneAcked = true),
                listOf(WearEffect.ArmTimeout(w.pending.requestId, 1, retryAfterMs)),
            )
            ReplyKind.CONFIRM -> {
                val id = reply.id
                if (id.isNullOrBlank()) {
                    answer(w, heard, reply.text)
                } else {
                    Transition(
                        WearUiState.Confirm(heard, reply.text.orEmpty(), id, reply.approve == true),
                        listOf(WearEffect.DisarmTimeout, WearEffect.Haptic(HapticKind.CONFIRM)),
                    )
                }
            }
            ReplyKind.ERROR -> fail(reasonFor(reply.errorCode), reply.text, w.pending.op)
            else -> answer(w, heard, reply.text)
        }
    }

    private fun answer(w: WearUiState.Waiting, heard: String?, text: String?): Transition {
        if (w.pending.op == Op.NewChat) {
            return Transition(
                WearUiState.Talk(notice = Notice.NEW_CHAT_STARTED),
                listOf(WearEffect.DisarmTimeout, WearEffect.Haptic(HapticKind.TICK)),
            )
        }
        // After a Yes/Approve the relay's `heard` is empty — don't echo anything.
        val shownHeard = if (w.pending.op is Op.Confirm) null else heard
        return Transition(
            WearUiState.Answer(shownHeard, text?.trim().orEmpty()),
            listOf(WearEffect.DisarmTimeout, WearEffect.Haptic(HapticKind.ANSWER)),
        )
    }

    private fun onTimeout(state: WearUiState, event: WearEvent.AttemptTimedOut): Transition {
        val w = state as? WearUiState.Waiting ?: return Transition(state)
        if (w.pending.requestId != event.requestId || w.pending.attempt != event.attempt) return Transition(state)
        if (w.pending.attempt >= maxAttempts) return fail(ErrorReason.PHONE_SILENT, null, w.pending.op)
        val next = w.pending.copy(attempt = w.pending.attempt + 1)
        return Transition(
            w.copy(pending = next),
            listOf(sendEffect(next), WearEffect.ArmTimeout(next.requestId, next.attempt, retryAfterMs)),
        )
    }

    private fun fail(reason: ErrorReason, detail: String?, op: Op?): Transition = Transition(
        WearUiState.Error(reason, detail?.takeIf { it.isNotBlank() }, retry = op),
        listOf(WearEffect.DisarmTimeout, WearEffect.Haptic(HapticKind.ERROR)),
    )

    private fun sendEffect(p: Pending): WearEffect = when (val op = p.op) {
        is Op.Ask -> WearEffect.SendAsk(AskRequest(p.requestId, op.text))
        Op.NewChat -> WearEffect.SendAsk(AskRequest(p.requestId, NEW_CHAT_TEXT))
        is Op.Confirm -> WearEffect.SendConfirm(ConfirmRequest(p.requestId, op.cardId, op.yes))
    }

    companion object {
        const val RETRY_AFTER_MS = 25_000L
        const val MAX_ATTEMPTS = 4

        /** relay.js `isResetCommand` — same phrase the upstream phone side sends for "New chat". */
        const val NEW_CHAT_TEXT = "start new session"

        fun reasonFor(code: String?): ErrorReason = when (code) {
            ErrorCode.NOT_CONFIGURED -> ErrorReason.NOT_CONFIGURED
            ErrorCode.UNAUTHORIZED -> ErrorReason.UNAUTHORIZED
            ErrorCode.UNREACHABLE -> ErrorReason.UNREACHABLE
            ErrorCode.TOO_SLOW -> ErrorReason.TOO_SLOW
            else -> ErrorReason.RELAY
        }
    }
}
