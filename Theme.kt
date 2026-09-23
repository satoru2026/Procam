package com.procam.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProCamColors = darkColorScheme(
    primary = Color(0xFFFFD60A),
    onPrimary = Color.Black,
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ProCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ProCamColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
