package com.hermeswatch.wear.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
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
import com.hermeswatch.wear.ui.theme.HermesColors
import com.hermeswatch.wear.ui.theme.HermesTheme

/**
 * Approve / Deny (Hermes & Claude Code approvals) or Yes / No (texts, emails,
 * car commands). Title in amber, the full prompt scrollable, then two
 * full-width chips: the primary action in amber, the refusal in a muted red so
 * the two can never be confused at a glance. Swipe right = leave it unanswered.
 */
@Composable
fun ConfirmScreen(state: WearUiState.Confirm, onChoice: (Boolean) -> Unit) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val chunks = remember(state.text) { TextChunks.split(state.text, maxChars = 120).ifEmpty { listOf("?") } }
    val yesLabel = if (state.approve) R.string.approve else R.string.yes
    val noLabel = if (state.approve) R.string.deny else R.string.no
    val title = if (state.approve) R.string.confirm_title_approve else R.string.confirm_title_yes_no

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 24.dp),
            scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 0.85f, edgeAlpha = 0.6f),
        ) {
            item(key = "title") {
                ListHeader {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(chunks) { chunk ->
                Text(
                    text = chunk,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }
            item(key = "yes") {
                Chip(
                    onClick = { onChoice(true) },
                    label = { Text(stringResource(yesLabel)) },
                    icon = { HIcon(R.drawable.ic_check, null, size = ChipDefaults.IconSize) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            item(key = "no") {
                Chip(
                    onClick = { onChoice(false) },
                    label = { Text(stringResource(noLabel)) },
                    icon = { HIcon(R.drawable.ic_close, null, size = ChipDefaults.IconSize) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = HermesColors.DangerContainer,
                        contentColor = HermesColors.Danger,
                        iconColor = HermesColors.Danger,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun ApprovePreview() = HermesTheme {
    ConfirmScreen(WearUiState.Confirm(null, "git push origin main", "c1", approve = true), {})
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun YesNoPreview() = HermesTheme {
    ConfirmScreen(WearUiState.Confirm("text Sam", "Text Sam: \"On my way, 10 min\"?", "c2", approve = false), {})
}
