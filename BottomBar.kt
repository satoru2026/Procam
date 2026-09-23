package com.procam.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.procam.app.camera.CameraUiState
import com.procam.app.camera.CaptureMode

private val modes = listOf(
    CaptureMode.NIGHT to "Malam",
    CaptureMode.PORTRAIT to "Potret",
    CaptureMode.PHOTO to "Foto",
    CaptureMode.VIDEO to "Video",
    CaptureMode.SLOW_MOTION to "Slow-Mo",
    CaptureMode.PANORAMA to "Panorama",
)

@Composable
fun ModeSelector(state: CameraUiState, onModeSelected: (CaptureMode) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        items(modes) { (mode, label) ->
            val selected = state.captureMode == mode
            Text(
                text = label,
                color = if (selected) Color.Yellow else Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .clickable { onModeSelected(mode) }
            )
        }
    }
}

@Composable
fun BottomBar(
    state: CameraUiState,
    onCapture: () -> Unit,
    onSwitchLens: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .clickable { onGalleryClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
        }

        ShutterButton(
            isVideoMode = state.captureMode == CaptureMode.VIDEO || state.captureMode == CaptureMode.SLOW_MOTION,
            isRecording = state.isRecording,
            isCapturing = state.isCapturing,
            onClick = onCapture
        )

        IconButton(
            onClick = onSwitchLens,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.DarkGray)
        ) {
            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
        }
    }
}

@Composable
private fun ShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    val outerColor = if (isVideoMode) Color.Red else Color.White
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .clickable(enabled = !isCapturing) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 34.dp else 60.dp)
                .clip(if (isRecording) androidx.compose.foundation.shape.RoundedCornerShape(6.dp) else CircleShape)
                .background(outerColor)
        )
    }
}
