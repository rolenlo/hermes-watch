package com.hermeswatch.wear.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * OLED-first palette: true black background, one warm accent (Hermes amber),
 * a muted danger tone for Deny. Everything else stays neutral so the answer
 * text is the hero.
 */
object HermesColors {
    val Amber = Color(0xFFFFB547)
    val OnAmber = Color(0xFF2A1800)
    val Surface = Color(0xFF1E1F22)
    val SurfaceHigh = Color(0xFF2A2B2F)
    val OnSurface = Color(0xFFF2F2F2)
    val OnSurfaceMuted = Color(0xFFB4B6BC)
    val Danger = Color(0xFFFF7A70)
    val DangerContainer = Color(0xFF3B1714)
}

private val colors = Colors(
    primary = HermesColors.Amber,
    primaryVariant = Color(0xFFE09A2E),
    secondary = HermesColors.SurfaceHigh,
    secondaryVariant = HermesColors.Surface,
    error = HermesColors.Danger,
    onPrimary = HermesColors.OnAmber,
    onSecondary = HermesColors.OnSurface,
    onError = Color.Black,
    background = Color.Black,
    onBackground = HermesColors.OnSurface,
    surface = HermesColors.Surface,
    onSurface = HermesColors.OnSurface,
    onSurfaceVariant = HermesColors.OnSurfaceMuted,
)

/** Theme + a true-black canvas (OLED: black pixels are off, and the round edge disappears). */
@Composable
fun HermesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = colors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        ) {
            content()
        }
    }
}
