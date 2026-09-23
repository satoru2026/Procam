package com.procam.app.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.procam.app.MainViewModel
import com.procam.app.camera.CaptureMode
import com.procam.app.ui.components.BottomBar
import com.procam.app.ui.components.FilterStrip
import com.procam.app.ui.components.GridOverlay
import com.procam.app.ui.components.ManualControlsPanel
import com.procam.app.ui.components.ModeSelector
import com.procam.app.ui.components.TopBar
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScopeCompat()

    var showManualPanel by remember { mutableStateOf(false) }
    var viewSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    viewModel.attachController(lifecycleOwner)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    scope.launch { viewModel.bindCamera(pv) }
                }
            },
            update = { pv ->
                scope.launch { viewModel.bindCamera(pv) }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        viewModel.tapToFocus(offset.x, offset.y, size.width, size.height)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = (state.zoomRatio * zoom)
                        viewModel.setZoom(newZoom)
                    }
                }
        )

        if (state.gridEnabled) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                state = state,
                onFlashClick = { viewModel.toggleFlash() },
                onGridClick = { viewModel.toggleGrid() },
                onTimerClick = { viewModel.cycleTimer() },
                onHdrClick = { viewModel.toggleHdr() },
                onSettingsClick = onOpenSettings
            )

            Box(modifier = Modifier.weight(1f)) {
                if (state.captureMode == CaptureMode.PANORAMA) {
                    Text(
                        "Geser perlahan ke kanan sambil menekan tombol rana",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    )
                }
                event?.let {
                    Text(
                        it,
                        color = Color.Yellow,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (showManualPanel) {
                ManualControlsPanel(
                    manual = state.manual,
                    onToggleManual = { viewModel.toggleManual() },
                    onIsoChange = { v -> viewModel.updateManual { it.copy(iso = v) } },
                    onShutterChange = { v -> viewModel.updateManual { it.copy(shutterSpeedNs = v) } },
                    onFocusChange = { v -> viewModel.updateManual { it.copy(focusDistanceDiopters = v) } },
                    onWbChange = { v -> viewModel.updateManual { it.copy(whiteBalanceKelvin = v) } },
                    onEvChange = { v -> viewModel.setEv(v) }
                )
            }

            FilterStrip(selected = state.filter, onSelect = { viewModel.setFilter(it) })

            ModeSelector(state = state) { mode ->
                viewModel.setCaptureMode(mode)
                showManualPanel = mode == CaptureMode.PHOTO || mode == CaptureMode.NIGHT
            }

            BottomBar(
                state = state,
                onCapture = {
                    if (state.captureMode == CaptureMode.VIDEO || state.captureMode == CaptureMode.SLOW_MOTION) {
                        viewModel.toggleVideoRecording()
                    } else {
                        viewModel.capturePhoto()
                    }
                },
                onSwitchLens = { viewModel.switchLens() },
                onGalleryClick = onOpenGallery
            )
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
