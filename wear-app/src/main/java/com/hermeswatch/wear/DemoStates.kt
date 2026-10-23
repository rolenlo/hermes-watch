package com.hermeswatch.wear

import com.hermeswatch.wear.state.ErrorReason
import com.hermeswatch.wear.state.Op
import com.hermeswatch.wear.state.Pending
import com.hermeswatch.wear.state.WearUiState

/** Sample states for debug UI review (`--es demo <key>`); never reachable in release builds. */
internal object DemoStates {
    fun forKey(key: String): WearUiState? = when (key) {
        "talk" -> WearUiState.Talk()
        "waiting" -> WearUiState.Waiting(Pending("demo", Op.Ask("今日有咩要做？"), 1), heard = "今日有咩要做？", phoneAcked = true)
        "answer" -> WearUiState.Answer(
            heard = "今日有咩要做？",
            text = "今日 3 件事：\n10:00 同 Alex 開會，講 Q3 roadmap。\n下晝 3 點前交 report 畀財務。\n6pm 前覆 Tesla 服務中心，確認星期一取車。要唔要我提你？",
        )
        "approve" -> WearUiState.Confirm(null, "Claude Code wants to run:\ngit push origin main", "demo", approve = true)
        "yesno" -> WearUiState.Confirm("text Sam on my way", "Text Sam: \"On my way, 10 min\"?", "demo", approve = false)
        "error" -> WearUiState.Error(ErrorReason.UNREACHABLE, retry = Op.Ask("hi"))
        else -> null
    }
}
