package com.hermeswatch.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Wear round-screen safe area for fixed (non-scrolling) layouts.
 *
 * Horizontal: ≈14.6% each side (Wear OS guidance — the square inscribed in the
 * circle). Vertical: none by default — fixed screens centre a compact stack, so
 * the empty space above/below already sits inside the curve; adding vertical
 * inset only risks squeezing the last element on small watches.
 */
@Composable
fun roundSafePadding(verticalFraction: Float = 0f): PaddingValues {
    val config = LocalConfiguration.current
    return if (config.isScreenRound) {
        PaddingValues(
            horizontal = (config.screenWidthDp * 0.146f).dp,
            vertical = (config.screenHeightDp * verticalFraction).dp,
        )
    } else {
        PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    }
}

@Composable
fun Modifier.roundSafe(verticalFraction: Float = 0f): Modifier = padding(roundSafePadding(verticalFraction))
