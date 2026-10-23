package com.hermeswatch.wear.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.tooling.preview.devices.WearDevices
import com.hermeswatch.wear.R
import com.hermeswatch.wear.state.ErrorReason
import com.hermeswatch.wear.state.Op
import com.hermeswatch.wear.state.WearUiState
import com.hermeswatch.wear.ui.theme.HermesTheme

private fun ErrorReason.title(): Int = when (this) {
    ErrorReason.NO_PHONE -> R.string.err_no_phone
    ErrorReason.PHONE_SILENT -> R.string.err_phone_silent
    ErrorReason.NOT_CONFIGURED -> R.string.err_not_configured
    ErrorReason.UNAUTHORIZED -> R.string.err_unauthorized
    ErrorReason.UNREACHABLE -> R.string.err_unreachable
    ErrorReason.RELAY -> R.string.err_relay
    ErrorReason.TOO_SLOW -> R.string.err_too_slow
    ErrorReason.INPUT -> R.string.err_input
}

private fun ErrorReason.hint(): Int = when (this) {
    ErrorReason.NO_PHONE -> R.string.err_no_phone_hint
    ErrorReason.PHONE_SILENT -> R.string.err_phone_silent_hint
    ErrorReason.NOT_CONFIGURED -> R.string.err_not_configured_hint
    ErrorReason.UNAUTHORIZED -> R.string.err_unauthorized_hint
    ErrorReason.UNREACHABLE -> R.string.err_unreachable_hint
    ErrorReason.RELAY -> R.string.err_relay_hint
    ErrorReason.TOO_SLOW -> R.string.err_too_slow_hint
    ErrorReason.INPUT -> R.string.err_input_hint
}

/**
 * Error: warning glyph, a 2–5 character title that says what broke, one line
 * on how to fix it, then 「再試」 (re-sends the same thing with a fresh
 * requestId). Relay detail (English, from the PC) only shows for relay errors,
 * small and capped, so the screen never turns into a log.
 */
@Composable
fun ErrorScreen(state: WearUiState.Error, onRetry: () -> Unit, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(listState)) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 24.dp),
        ) {
            item(key = "icon") {
                HIcon(R.drawable.ic_warning, null, size = 28.dp, tint = MaterialTheme.colors.error)
            }
            item(key = "title") {
                Text(
                    text = stringResource(state.reason.title()),
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                )
            }
            item(key = "hint") {
                Text(
                    text = stringResource(state.reason.hint()),
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            val detail = state.detail
            if (state.reason == ErrorReason.RELAY && !detail.isNullOrBlank()) {
                item(key = "detail") {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.caption3,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (state.retry != null) {
                item(key = "retry") {
                    Chip(
                        onClick = onRetry,
                        label = { Text(stringResource(R.string.retry)) },
                        icon = { HIcon(R.drawable.ic_refresh, null, size = ChipDefaults.IconSize) },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
            item(key = "back") {
                CompactChip(
                    onClick = onBack,
                    label = { Text(stringResource(R.string.back)) },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun ErrorPreview() = HermesTheme {
    ErrorScreen(WearUiState.Error(ErrorReason.UNREACHABLE, retry = Op.Ask("hi")), {}, {})
}
