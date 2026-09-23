package com.procam.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.procam.app.camera.CameraController
import com.procam.app.camera.CameraUiState
import com.procam.app.camera.CaptureMode
import com.procam.app.camera.FilterType
import com.procam.app.camera.FlashState
import com.procam.app.camera.ImageProcessor
import com.procam.app.camera.LensFacingOption
import com.procam.app.camera.ManualSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    private var controller: CameraController? = null

    fun attachController(lifecycleOwner: LifecycleOwner) {
        if (controller == null) {
            controller = CameraController(getApplication(), lifecycleOwner).apply {
                onError = { msg -> _events.value = msg }
                onVideoStateChanged = { recording ->
                    _uiState.value = _uiState.value.copy(isRecording = recording)
                }
            }
        }
    }

    fun controllerOrNull() = controller

    suspend fun bindCamera(previewView: androidx.camera.view.PreviewView) {
        controller?.bindUseCases(previewView, _uiState.value)
        controller?.getZoomRange()?.let { (min, max) ->
            _uiState.value = _uiState.value.copy(minZoom = min, maxZoom = max)
        }
    }

    fun setCaptureMode(mode: CaptureMode) {
        _uiState.value = _uiState.value.copy(captureMode = mode)
    }

    fun toggleFlash() {
        val next = when (_uiState.value.flash) {
            FlashState.OFF -> FlashState.ON
            FlashState.ON -> FlashState.AUTO
            FlashState.AUTO -> FlashState.TORCH
            FlashState.TORCH -> FlashState.OFF
        }
        _uiState.value = _uiState.value.copy(flash = next)
        controller?.applyFlash(next)
    }

    fun switchLens() {
        val next = if (_uiState.value.lensFacing == LensFacingOption.BACK)
            LensFacingOption.FRONT else LensFacingOption.BACK
        _uiState.value = _uiState.value.copy(lensFacing = next)
    }

    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(_uiState.value.minZoom, _uiState.value.maxZoom)
        _uiState.value = _uiState.value.copy(zoomRatio = clamped)
        controller?.applyZoom(clamped)
    }

    fun toggleGrid() {
        _uiState.value = _uiState.value.copy(gridEnabled = !_uiState.value.gridEnabled)
    }

    fun cycleTimer() {
        val next = when (_uiState.value.timerSeconds) {
            0 -> 3
            3 -> 10
            else -> 0
        }
        _uiState.value = _uiState.value.copy(timerSeconds = next)
    }

    fun setFilter(filter: FilterType) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun toggleHdr() {
        _uiState.value = _uiState.value.copy(hdrEnabled = !_uiState.value.hdrEnabled)
    }

    fun toggleManual() {
        val m = _uiState.value.manual.copy(isManualEnabled = !_uiState.value.manual.isManualEnabled)
        _uiState.value = _uiState.value.copy(manual = m)
        controller?.updateManualLive(m)
    }

    fun updateManual(update: (ManualSettings) -> ManualSettings) {
        val m = update(_uiState.value.manual)
        _uiState.value = _uiState.value.copy(manual = m)
        controller?.updateManualLive(m)
    }

    fun setEv(ev: Int) {
        _uiState.value = _uiState.value.copy(manual = _uiState.value.manual.copy(evCompensation = ev))
        controller?.applyExposureCompensation(ev)
    }

    fun toggleSaveLocation() {
        _uiState.value = _uiState.value.copy(saveLocation = !_uiState.value.saveLocation)
    }

    fun toggleWatermark() {
        _uiState.value = _uiState.value.copy(watermarkEnabled = !_uiState.value.watermarkEnabled)
    }

    fun toggleShutterSound() {
        _uiState.value = _uiState.value.copy(shutterSoundEnabled = !_uiState.value.shutterSoundEnabled)
    }

    fun capturePhoto() {
        val timer = _uiState.value.timerSeconds
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCapturing = true)
            if (timer > 0) {
                for (s in timer downTo 1) {
                    _events.value = "Mengambil foto dalam $s..."
                    delay(1000)
                }
            }
            val state = _uiState.value
            controller?.takePhoto(
                state,
                onSaved = { uri ->
                    _uiState.value = _uiState.value.copy(
                        lastThumbnailPath = uri.toString(),
                        isCapturing = false
                    )
                    viewModelScope.launch {
                        runCatching {
                            ImageProcessor.applyFilterAndWatermark(
                                getApplication(), uri, state.filter, state.watermarkEnabled
                            )
                        }
                    }
                },
                onFailed = { msg ->
                    _events.value = msg
                    _uiState.value = _uiState.value.copy(isCapturing = false)
                }
            )
        }
    }

    fun toggleVideoRecording() {
        val recording = _uiState.value.isRecording
        if (recording) {
            controller?.stopRecording()
        } else {
            controller?.startRecording(
                onStarted = { },
                onFinished = { uri ->
                    uri?.let {
                        _uiState.value = _uiState.value.copy(lastThumbnailPath = it.toString())
                    }
                }
            )
        }
    }

    fun tapToFocus(x: Float, y: Float, w: Int, h: Int) {
        controller?.tapToFocus(x, y, w, h)
    }

    fun consumeEvent() {
        _events.value = null
    }

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
