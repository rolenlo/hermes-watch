package com.hermeswatch.wear.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.hermeswatch.wear.R
import com.hermeswatch.wear.state.Op
import com.hermeswatch.wear.state.Pending
import com.hermeswatch.wear.state.WearUiState
import com.hermeswatch.wear.ui.roundSafe
import com.hermeswatch.wear.ui.theme.HermesTheme
import kotlinx.coroutines.delay

/** After this long the title softens to 「仲諗緊…」 so a slow answer doesn't feel stuck. */
private const val SLOW_AFTER_MS = 12_000L

/**
 * Waiting: an indeterminate amber ring hugging the bezel (uses the round
 * screen instead of fighting it), a one-word status, the question echoed in
 * muted text, and a single cancel button. Screen stays on while waiting so the
 * answer is visible the moment it lands.
 */
@Composable
fun WaitingScreen(state: WearUiState.Waiting, onCancel: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var slow by remember(state.pending.requestId) { mutableStateOf(false) }
    LaunchedEffect(state.pending.requestId) {
        delay(SLOW_AFTER_MS)
        slow = true
    }

    val title = when (state.pending.op) {
        is Op.Ask -> if (slow) R.string.waiting_slow else R.string.waiting
        Op.NewChat -> R.string.waiting_new_chat
        is Op.Confirm -> if (slow) R.string.waiting_slow else R.string.waiting_confirm
    }
    val reconnecting = state.pending.attempt > 1

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            indicatorColor = MaterialTheme.colors.primary,
            trackColor = Color.Transparent,
            strokeWidth = 4.dp,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .roundSafe(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
                label = "waiting-title",
            ) { res ->
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                )
            }
            val heard = state.heard
            if (!heard.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.quoted, heard),
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedVisibility(visible = reconnecting, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.waiting_reconnecting),
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier,
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                HIcon(R.drawable.ic_close, contentDescription = stringResource(R.string.cancel), size = 22.dp)
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun WaitingPreview() = HermesTheme {
    WaitingScreen(WearUiState.Waiting(Pending("r", Op.Ask("今日天氣點？"), 1), heard = "今日天氣點？"), {})
}
