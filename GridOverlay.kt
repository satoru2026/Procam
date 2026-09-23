package com.procam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val color = Color.White.copy(alpha = 0.5f)
        val strokeWidth = 1f
        drawLine(color, androidx.compose.ui.geometry.Offset(w / 3f, 0f), androidx.compose.ui.geometry.Offset(w / 3f, h), strokeWidth)
        drawLine(color, androidx.compose.ui.geometry.Offset(2 * w / 3f, 0f), androidx.compose.ui.geometry.Offset(2 * w / 3f, h), strokeWidth)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, h / 3f), androidx.compose.ui.geometry.Offset(w, h / 3f), strokeWidth)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, 2 * h / 3f), androidx.compose.ui.geometry.Offset(w, 2 * h / 3f), strokeWidth)
    }
}
