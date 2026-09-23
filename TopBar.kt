package com.procam.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.procam.app.camera.CameraUiState
import com.procam.app.camera.FlashState

@Composable
fun TopBar(
    state: CameraUiState,
    onFlashClick: () -> Unit,
    onGridClick: () -> Unit,
    onTimerClick: () -> Unit,
    onHdrClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onFlashClick) {
            Icon(
                imageVector = when (state.flash) {
                    FlashState.OFF -> Icons.Default.FlashOff
                    FlashState.ON -> Icons.Default.FlashOn
                    FlashState.AUTO -> Icons.Default.FlashAuto
                    FlashState.TORCH -> Icons.Default.WbSunny
                },
                contentDescription = "Flash",
                tint = Color.White
            )
        }
        IconButton(onClick = onHdrClick) {
            Icon(
                Icons.Default.Tonality,
                contentDescription = "HDR",
                tint = if (state.hdrEnabled) Color.Yellow else Color.White
            )
        }
        IconButton(onClick = onTimerClick) {
            Icon(Icons.Default.Timer, contentDescription = "Timer", tint = Color.White)
        }
        if (state.timerSeconds > 0) {
            Text("${state.timerSeconds}s", color = Color.Yellow)
        }
        IconButton(onClick = onGridClick) {
            Icon(
                Icons.Default.GridOn,
                contentDescription = "Grid",
                tint = if (state.gridEnabled) Color.Yellow else Color.White
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }
    }
}
