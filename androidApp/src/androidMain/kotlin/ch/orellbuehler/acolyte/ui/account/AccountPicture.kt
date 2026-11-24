package ch.orellbuehler.acolyte.ui.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal data class SquareCrop(
    val left: Int,
    val top: Int,
    val side: Int,
)

internal fun squareCrop(
    width: Int,
    height: Int,
): SquareCrop {
    val side = minOf(width, height)
    return SquareCrop((width - side) / 2, (height - side) / 2, side)
}

internal fun sampleSize(
    shortSide: Int,
    target: Int,
): Int {
    var sample = 1
    while (shortSide / (sample * 2) >= target) sample *= 2
    return sample
}

internal fun exifDegrees(orientation: Int): Int =
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

suspend fun loadSquareJpeg(
    context: Context,
    uri: Uri,
    size: Int = 512,
): ByteArray? =
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(minOf(bounds.outWidth, bounds.outHeight), size) }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@withContext null
            val degrees =
                resolver.openInputStream(uri)?.use {
                    exifDegrees(ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
                } ?: 0
            val upright =
                if (degrees == 0) {
                    decoded
                } else {
                    Bitmap.createBitmap(
                        decoded,
                        0,
                        0,
                        decoded.width,
                        decoded.height,
                        Matrix().apply { postRotate(degrees.toFloat()) },
                        true,
                    )
                }
            val crop = squareCrop(upright.width, upright.height)
            val square = Bitmap.createBitmap(upright, crop.left, crop.top, crop.side, crop.side)
            val scaled = if (square.width > size) Bitmap.createScaledBitmap(square, size, size, true) else square
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.toByteArray()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
