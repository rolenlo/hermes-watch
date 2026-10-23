package com.hermeswatch.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

/**
 * Phase 0 navigation stub. Wire to real recording + Data Layer in Phase 1.
 *
 * Screens mirror the Amazfit/Zepp flow:
 * Talk → Waiting → Answer | ApproveDeny
 */
enum class WearScreen {
    Talk,
    Waiting,
    Answer,
    ApproveDeny,
}

@Composable
fun HermesWearNav() {
    var screen by remember { mutableStateOf(WearScreen.Talk) }
    when (screen) {
        WearScreen.Talk -> TalkScreen(onTalk = { screen = WearScreen.Waiting })
        WearScreen.Waiting -> WaitingScreen(onDemoAnswer = { screen = WearScreen.Answer })
        WearScreen.Answer -> AnswerScreen(
            onNewChat = { screen = WearScreen.Talk },
            onShowApprove = { screen = WearScreen.ApproveDeny },
        )
        WearScreen.ApproveDeny -> ApproveDenyScreen(
            onDone = { screen = WearScreen.Talk },
        )
    }
}

@Composable
fun TalkScreen(onTalk: () -> Unit) {
    // TODO(Phase 1): MediaRecorder → 16 kHz mono WAV/PCM; send via DataLayerPaths.MSG_ASK
    PlaceholderColumn(title = "TALK") {
        Button(onClick = onTalk) {
            Text("TALK (stub)")
        }
    }
}

@Composable
fun WaitingScreen(onDemoAnswer: () -> Unit) {
    // TODO(Phase 1): poll / wait for MSG_RESULT from phone companion
    PlaceholderColumn(title = "Waiting…") {
        Button(onClick = onDemoAnswer) {
            Text("Demo answer")
        }
    }
}

@Composable
fun AnswerScreen(onNewChat: () -> Unit, onShowApprove: () -> Unit) {
    // TODO(Phase 1): render relay text; optional TTS later
    PlaceholderColumn(title = "Answer") {
        Text(
            text = "(stub) Hermes reply appears here.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Button(onClick = onNewChat) { Text("New chat") }
        Button(onClick = onShowApprove) { Text("Demo Approve") }
    }
}

@Composable
fun ApproveDenyScreen(onDone: () -> Unit) {
    // TODO(Phase 1): map to POST /watch/confirm via phone companion
    PlaceholderColumn(title = "Approve?") {
        Button(onClick = onDone) { Text("Approve") }
        Button(onClick = onDone) { Text("Deny") }
    }
}

@Composable
private fun PlaceholderColumn(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, textAlign = TextAlign.Center)
        content()
    }
}
