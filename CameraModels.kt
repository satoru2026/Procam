package com.procam.app.camera

enum class CaptureMode { PHOTO, VIDEO, PORTRAIT, NIGHT, PANORAMA, SLOW_MOTION }

enum class FlashState { OFF, ON, AUTO, TORCH }

enum class LensFacingOption { BACK, FRONT }

enum class FilterType(val label: String) {
    NONE("Normal"),
    MONO("Mono"),
    SEPIA("Sepia"),
    VIVID("Vivid"),
    COOL("Cool"),
    WARM("Warm"),
    NOIR("Noir")
}

data class ManualSettings(
    val isManualEnabled: Boolean = false,
    val iso: Int = 100,              // sensor sensitivity
    val shutterSpeedNs: Long = 1_000_000_000L / 60, // exposure time in nanoseconds (default 1/60s)
    val focusDistanceDiopters: Float = 0f, // 0 = infinity
    val whiteBalanceKelvin: Int = 5500,
    val evCompensation: Int = 0
)

data class CameraUiState(
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val flash: FlashState = FlashState.OFF,
    val lensFacing: LensFacingOption = LensFacingOption.BACK,
    val zoomRatio: Float = 1f,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val gridEnabled: Boolean = false,
    val timerSeconds: Int = 0,       // 0, 3, or 10
    val filter: FilterType = FilterType.NONE,
    val manual: ManualSettings = ManualSettings(),
    val isRecording: Boolean = false,
    val lastThumbnailPath: String? = null,
    val isCapturing: Boolean = false,
    val hdrEnabled: Boolean = true,
    val saveLocation: Boolean = false,
    val watermarkEnabled: Boolean = false,
    val shutterSoundEnabled: Boolean = true
)
