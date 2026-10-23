package com.hermeswatch.phone.relay

import com.hermeswatch.shared.ErrorCode
import com.hermeswatch.shared.ReplyKind
import com.hermeswatch.shared.WatchReply

/** relay.js [AskResponse] → watch [WatchReply]. Pure; unit-tested. */
object ReplyMapper {
    fun toWatch(requestId: String, r: AskResponse): WatchReply {
        if (!r.ok && r.kind == null) {
            return WatchReply(
                requestId = requestId,
                kind = ReplyKind.ERROR,
                text = r.error ?: "Relay error",
                errorCode = ErrorCode.RELAY,
            )
        }
        return when (r.kind) {
            ReplyKind.CONFIRM -> if (r.id.isNullOrBlank()) {
                // A card without an id can't be answered — show it as plain text.
                WatchReply(requestId, ReplyKind.ANSWER, text = r.text.orEmpty(), heard = r.heard)
            } else {
                WatchReply(
                    requestId = requestId,
                    kind = ReplyKind.CONFIRM,
                    text = r.text.orEmpty(),
                    heard = r.heard,
                    id = r.id,
                    approve = r.approve == true,
                )
            }
            ReplyKind.WORKING -> WatchReply(requestId, ReplyKind.WORKING, text = r.note, heard = r.heard)
            else -> WatchReply(
                requestId = requestId,
                kind = ReplyKind.ANSWER,
                text = r.text?.takeIf { it.isNotBlank() } ?: r.note ?: r.error ?: "",
                heard = r.heard,
            )
        }
    }

    fun error(requestId: String, code: String, detail: String?): WatchReply =
        WatchReply(requestId = requestId, kind = ReplyKind.ERROR, text = detail, errorCode = code)
}
