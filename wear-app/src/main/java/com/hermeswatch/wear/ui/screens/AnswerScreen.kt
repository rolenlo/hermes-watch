package com.hermeswatch.wear.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.scrollAway
import androidx.wear.tooling.preview.devices.WearDevices
import com.hermeswatch.wear.R
import com.hermeswatch.wear.state.TextChunks
import com.hermeswatch.wear.state.WearUiState
import com.hermeswatch.wear.ui.theme.HermesTheme

/**
 * Answer: what you asked (muted, small) → the answer as short paragraphs in
 * a ScalingLazyColumn (crown / bezel scrolls it — built into Wear Compose 1.4)
 * → 「再問」 primary chip and a quiet 「完成」. TimeText scrolls away so long
 * answers get the full circle.
 */
@Composable
fun AnswerScreen(state: WearUiState.Answer, onAskAgain: () -> Unit, onDone: () -> Unit) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val chunks = remember(state.text) { TextChunks.split(state.text).ifEmpty { listOf("—") } }
    val short = state.text.length <= 60

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 24.dp),
            // Gentler fisheye than the default: this is reading, not a menu.
            scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 0.85f, edgeAlpha = 0.6f),
        ) {
            item(key = "heard") {
                ListHeader {
                    Text(
                        text = state.heard?.let { stringResource(R.string.quoted, it) }
                            ?: stringResource(R.string.answer_header),
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            items(chunks) { chunk ->
                Text(
                    text = chunk,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface,
                    textAlign = if (short) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
            item(key = "again") {
                Chip(
                    onClick = onAskAgain,
                    label = { Text(stringResource(R.string.ask_again)) },
                    icon = { HIcon(R.drawable.ic_mic, null, size = ChipDefaults.IconSize) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            item(key = "done") {
                CompactChip(
                    onClick = onDone,
                    label = { Text(stringResource(R.string.done)) },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun AnswerPreview() = HermesTheme {
    AnswerScreen(
        WearUiState.Answer(
            heard = "今日要做咩？",
            text = "今日 3 件事：\n• 10:00 同 Alex 開會\n• 交 Q3 report\n• 6pm 前覆 Tesla 服務中心。要唔要我提你？",
        ),
        {}, {},
    )
}
