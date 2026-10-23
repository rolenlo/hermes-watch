package com.hermeswatch.wear.state

/**
 * Pure-Kotlin model for the watch UI. One [WearUiState] drives every screen;
 * [WearReducer] is the only thing that changes it. No Android imports here so
 * the whole flow is unit-tested on the JVM.
 */

/** What the watch is waiting on. Kept so a timeout can resend, and Error can retry. */
sealed interface Op {
    data class Ask(val text: String) : Op
    data object NewChat : Op
    data class Confirm(val cardId: String, val yes: Boolean, val approve: Boolean) : Op
}

data class Pending(
    val requestId: String,
    val op: Op,
    /** 1-based send attempt for this requestId (resets to 1 on any heartbeat). */
    val attempt: Int,
)

enum class Notice { NEW_CHAT_STARTED }

enum class ErrorReason {
    /** No reachable phone node (Bluetooth off / not paired / out of range). */
    NO_PHONE,
    /** Sent, but the companion never answered across all retries. */
    PHONE_SILENT,
    NOT_CONFIGURED,
    UNAUTHORIZED,
    UNREACHABLE,
    RELAY,
    TOO_SLOW,
    /** The watch has no RemoteInput (voice / keyboard) activity. */
    INPUT,
}

sealed interface WearUiState {
    /** Stable key per screen type — drives AnimatedContent / SwipeToDismissBox. */
    val screenKey: String

    data class Talk(val notice: Notice? = null) : WearUiState {
        override val screenKey get() = "talk"
    }

    data class Waiting(
        val pending: Pending,
        /** What the user said / typed (or the relay's `heard`), shown under the spinner. */
        val heard: String? = null,
        /** True once the phone acked (any `working` heartbeat) — the relay call is underway. */
        val phoneAcked: Boolean = false,
    ) : WearUiState {
        override val screenKey get() = "waiting"
    }

    data class Answer(val heard: String?, val text: String) : WearUiState {
        override val screenKey get() = "answer"
    }

    data class Confirm(
        val heard: String?,
        val text: String,
        val cardId: String,
        /** true = Hermes/Claude Code approval (批准/拒絕); false = Yes/No card (好/唔好). */
        val approve: Boolean,
    ) : WearUiState {
        override val screenKey get() = "confirm"
    }

    data class Error(
        val reason: ErrorReason,
        val detail: String? = null,
        /** What 「再試」 will resend (with a fresh requestId); null = no retry, only back. */
        val retry: Op? = null,
    ) : WearUiState {
        override val screenKey get() = "error"
    }
}

sealed interface WearEvent {
    data class Ask(val text: String) : WearEvent
    data object NewChat : WearEvent
    data class ConfirmChoice(val yes: Boolean) : WearEvent
    /** Swipe-to-dismiss, hardware back, or the cancel button. */
    data object Dismiss : WearEvent
    data object Retry : WearEvent
    data object ClearNotice : WearEvent
    data object InputUnavailable : WearEvent
    data class ReplyReceived(val reply: com.hermeswatch.shared.WatchReply) : WearEvent
    data class AttemptTimedOut(val requestId: String, val attempt: Int) : WearEvent
    data class SendFailed(val requestId: String, val noPhone: Boolean) : WearEvent
}

enum class HapticKind { ANSWER, CONFIRM, ERROR, TICK }

sealed interface WearEffect {
    data class SendAsk(val request: com.hermeswatch.shared.AskRequest) : WearEffect
    data class SendConfirm(val request: com.hermeswatch.shared.ConfirmRequest) : WearEffect
    data class ArmTimeout(val requestId: String, val attempt: Int, val delayMs: Long) : WearEffect
    data object DisarmTimeout : WearEffect
    data class Haptic(val kind: HapticKind) : WearEffect
}

data class Transition(val state: WearUiState, val effects: List<WearEffect> = emptyList())
