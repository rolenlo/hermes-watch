package com.hermeswatch.wear.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.LocalContentColor

/** Vector icons bundled as drawables (no material-icons dependency). */
@Composable
internal fun HIcon(
    id: Int,
    contentDescription: String?,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
