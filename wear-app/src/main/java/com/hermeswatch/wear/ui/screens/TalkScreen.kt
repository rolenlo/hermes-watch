package com.hermeswatch.wear.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.tooling.preview.Preview
import com.hermeswatch.wear.R
import com.hermeswatch.wear.state.Notice
import com.hermeswatch.wear.ui.roundSafe
import com.hermeswatch.wear.ui.theme.HermesTheme
import kotlinx.coroutines.delay

/**
 * Home: one big amber TALK button (≈ 42% of the screen, impossible to miss),
 * one quiet secondary "新對話" chip. A brief confirmation replaces the chip
 * after a new chat starts.
 */
@Composable
fun TalkScreen(
    notice: Notice?,
    onTalk: () -> Unit,
    onNewChat: () -> Unit,
    onClearNotice: () -> Unit,
) {
    if (notice != null) {
        LaunchedEffect(notice) {
            delay(2_200)
            onClearNotice()
        }
    }
    val talkDescription = stringResource(R.string.talk_description)
    // No vignette: nothing scrolls here, and it would dim the 新對話 chip.
    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .roundSafe(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onTalk,
                modifier = Modifier
                    .size(96.dp)
                    .semantics { contentDescription = talkDescription },
                colors = ButtonDefaults.primaryButtonColors(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HIcon(R.drawable.ic_mic, contentDescription = null, size = 34.dp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.talk),
                        style = MaterialTheme.typography.button,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = notice,
                transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(180))) },
                label = "talk-secondary",
            ) { n ->
                if (n == Notice.NEW_CHAT_STARTED) {
                    // Same height as the chip so nothing jumps.
                    Row(
                        modifier = Modifier.height(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HIcon(R.drawable.ic_check, null, size = 18.dp, tint = MaterialTheme.colors.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.new_chat_done),
                            style = MaterialTheme.typography.caption1,
                            color = MaterialTheme.colors.primary,
                        )
                    }
                } else {
                    CompactChip(
                        onClick = onNewChat,
                        label = { Text(stringResource(R.string.new_chat)) },
                        icon = { HIcon(R.drawable.ic_add, null, size = ChipDefaults.SmallIconSize) },
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun TalkPreview() = HermesTheme { TalkScreen(null, {}, {}, {}) }

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun TalkNoticePreview() = HermesTheme { TalkScreen(Notice.NEW_CHAT_STARTED, {}, {}, {}) }
