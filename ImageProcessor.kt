package com.procam.app.camera

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Applies a chosen [FilterType] and an optional watermark to a saved photo in place.
 * Runs on a background thread; call from a coroutine / worker, not the main thread.
 */
object ImageProcessor {

    fun applyFilterAndWatermark(
        context: Context,
        uri: Uri,
        filter: FilterType,
        addWatermark: Boolean
    ) {
        if (filter == FilterType.NONE && !addWatermark) return
        val resolver = context.contentResolver
        val original = resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
            ?: return

        val filtered = applyFilter(original, filter)
        val finalBitmap = if (addWatermark) drawWatermark(filtered) else filtered

        resolver.openOutputStream(uri, "w")?.use { out: OutputStream ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        if (filtered !== original) original.recycle()
        if (finalBitmap !== filtered) filtered.recycle()
    }

    private fun applyFilter(src: Bitmap, filter: FilterType): Bitmap {
        if (filter == FilterType.NONE) return src
        val matrix = when (filter) {
            FilterType.MONO -> ColorMatrix().apply { setSaturation(0f) }
            FilterType.NOIR -> ColorMatrix().apply {
                setSaturation(0f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.2f, 0f, 0f, 0f, -20f,
                    0f, 1.2f, 0f, 0f, -20f,
                    0f, 0f, 1.2f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterType.SEPIA -> ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            FilterType.VIVID -> ColorMatrix().apply { setSaturation(1.6f) }
            FilterType.COOL -> ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, -10f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            ))
            FilterType.WARM -> ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 20f,
                0f, 1f, 0f, 0f, 5f,
                0f, 0f, 1f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            ))
            FilterType.NONE -> ColorMatrix()
        }

        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    private fun drawWatermark(src: Bitmap): Bitmap {
        val output = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val textSize = output.width * 0.028f
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = 200
            this.textSize = textSize
            isAntiAlias = true
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        val label = "ProCam · ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
        canvas.drawText(label, textSize * 0.5f, output.height - textSize, paint)
        return output
    }

    /** Very simple portrait-style background blur: box-blurs the whole frame and
     * composites a sharp elliptical "subject" region in the center. This is a stand-in
     * for real depth-based segmentation, which needs a dual-camera/ToF sensor or an
     * on-device ML segmentation model (not included here). */
    fun applySimplePortraitBlur(src: Bitmap): Bitmap {
        val blurred = boxBlur(src, radius = 12)
        val output = blurred.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val cx = src.width / 2f
        val cy = src.height / 2f
        val rx = src.width * 0.32f
        val ry = src.height * 0.42f
        val path = android.graphics.Path().apply {
            addOval(cx - rx, cy - ry, cx + rx, cy + ry, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(src, 0f, 0f, null)
        canvas.restore()
        return output
    }

    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(
            src, maxOf(1, src.width / radius), maxOf(1, src.height / radius), true
        )
        return Bitmap.createScaledBitmap(scaled, src.width, src.height, true)
    }
}
