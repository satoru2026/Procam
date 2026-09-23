package com.procam.app.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Wraps CameraX use cases and exposes manual Camera2 controls (ISO, shutter speed,
 * focus distance, white balance) via Camera2Interop, plus multi-frame capture helpers
 * used to approximate HDR and Night mode.
 *
 * NOTE: True computational photography stacks (Google's HDR+/Night Sight) rely on
 * proprietary, on-device trained models and burst-alignment pipelines that are not
 * public. What's implemented here is a transparent, from-scratch approximation:
 * - "HDR" = ImageCapture with CAPTURE_MODE_MAXIMIZE_QUALITY (+ optional AE bracketing hook)
 * - "Night" = a longer, manually-set exposure time captured through the same pipeline
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var preview: Preview? = null

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private val ioExecutor = Executors.newSingleThreadExecutor()

    var onPhotoSaved: ((Uri) -> Unit)? = null
    var onVideoStateChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    suspend fun getProvider(): ProcessCameraProvider {
        cameraProvider?.let { return it }
        return suspendGetProvider().also { cameraProvider = it }
    }

    private suspend fun suspendGetProvider(): ProcessCameraProvider =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                cont.resume(future.get()) {}
            }, mainExecutor)
        }

    @SuppressLint("RestrictedApi")
    suspend fun bindUseCases(
        previewView: androidx.camera.view.PreviewView,
        state: CameraUiState
    ) {
        val provider = getProvider()
        provider.unbindAll()

        val lensFacing = if (state.lensFacing == LensFacingOption.BACK)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(
                if (state.hdrEnabled) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            )

        applyManualCaptureOptions(captureBuilder, state.manual)
        imageCapture = captureBuilder.build()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    listOf(Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            camera = when (state.captureMode) {
                CaptureMode.VIDEO, CaptureMode.SLOW_MOTION -> provider.bindToLifecycle(
                    lifecycleOwner, selector, preview, videoCapture
                )
                else -> provider.bindToLifecycle(
                    lifecycleOwner, selector, preview, imageCapture
                )
            }
            camera?.cameraInfo?.zoomState?.value?.let { /* consumed via observer in ViewModel */ }
            applyZoom(state.zoomRatio)
            applyFlash(state.flash)
        } catch (e: Exception) {
            Log.e(TAG, "bindUseCases failed", e)
            onError?.invoke("Gagal membuka kamera: ${e.message}")
        }
    }

    /** Push manual ISO / shutter speed / focus distance / white balance via Camera2Interop. */
    @SuppressLint("RestrictedApi")
    private fun applyManualCaptureOptions(builder: ImageCapture.Builder, manual: ManualSettings) {
        if (!manual.isManualEnabled) return
        val ext = Camera2Interop.Extender(builder)
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        ext.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, manual.iso)
        ext.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, manual.shutterSpeedNs)
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        ext.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, manual.focusDistanceDiopters)
    }

    /** Live-update manual params on an already-bound session (no rebind needed). */
    @SuppressLint("RestrictedApi")
    fun updateManualLive(manual: ManualSettings) {
        val cam = camera ?: return
        val control = Camera2CameraControl.from(cam.cameraControl)
        val options = CaptureRequestOptions.Builder().apply {
            if (manual.isManualEnabled) {
                setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, manual.iso)
                setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, manual.shutterSpeedNs)
                setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, manual.focusDistanceDiopters)
            } else {
                setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
        }.build()
        control.captureRequestOptions = options
    }

    fun applyZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun applyExposureCompensation(ev: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(ev)
    }

    fun applyFlash(flash: FlashState) {
        imageCapture?.flashMode = when (flash) {
            FlashState.ON -> ImageCapture.FLASH_MODE_ON
            FlashState.AUTO -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        camera?.cameraControl?.enableTorch(flash == FlashState.TORCH)
    }

    fun tapToFocus(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        val cam = camera ?: return
        val factory = SurfaceOrientedMeteringPointFactory(viewWidth.toFloat(), viewHeight.toFloat())
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    fun getZoomRange(): Pair<Float, Float> {
        val state = camera?.cameraInfo?.zoomState?.value
        return Pair(state?.minZoomRatio ?: 1f, state?.maxZoomRatio ?: 1f)
    }

    /**
     * Captures a photo. For NIGHT mode, temporarily forces a longer manual exposure
     * (simulating a single long-exposure "night" shot) then restores previous settings.
     */
    fun takePhoto(
        state: CameraUiState,
        onSaved: (Uri) -> Unit,
        onFailed: (String) -> Unit
    ) {
        val capture = imageCapture ?: run { onFailed("Kamera belum siap"); return }

        if (state.captureMode == CaptureMode.NIGHT) {
            val nightManual = state.manual.copy(
                isManualEnabled = true,
                iso = maxOf(state.manual.iso, 800),
                shutterSpeedNs = 1_000_000_000L / 4 // ~1/4s long exposure
            )
            updateManualLive(nightManual)
        }

        val name = "ProCam_${timestamp()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProCam")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (state.captureMode == CaptureMode.NIGHT) {
                        updateManualLive(state.manual) // restore
                    }
                    output.savedUri?.let { onSaved(it) } ?: onFailed("URI kosong")
                }

                override fun onError(exc: ImageCaptureException) {
                    if (state.captureMode == CaptureMode.NIGHT) {
                        updateManualLive(state.manual)
                    }
                    onFailed(exc.message ?: "Gagal mengambil foto")
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onStarted: () -> Unit, onFinished: (Uri?) -> Unit, withAudio: Boolean = true) {
        val videoCap = videoCapture ?: return
        val name = "ProCam_${timestamp()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ProCam")
            }
        }
        val outputOptions = androidx.camera.video.MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        var pendingRecording = videoCap.output.prepareRecording(context, outputOptions)
        if (withAudio) pendingRecording = pendingRecording.withAudioEnabled()

        activeRecording = pendingRecording.start(mainExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> onStarted()
                is VideoRecordEvent.Finalize -> {
                    onVideoStateChanged?.invoke(false)
                    if (!event.hasError()) onFinished(event.outputResults.outputUri)
                    else onFinished(null)
                }
                else -> {}
            }
        }
        onVideoStateChanged?.invoke(true)
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun release() {
        cameraProvider?.unbindAll()
        ioExecutor.shutdown()
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

    companion object {
        private const val TAG = "CameraController"
    }
}
