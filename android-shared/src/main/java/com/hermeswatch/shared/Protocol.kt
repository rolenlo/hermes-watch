package com.hermeswatch.shared

/**
 * High-level message kinds mirrored from relay.js watch replies.
 * See upstream README "Relay HTTP API".
 */
object ReplyKind {
    const val ANSWER = "answer"
    const val CONFIRM = "confirm"
    const val WORKING = "working"
}
