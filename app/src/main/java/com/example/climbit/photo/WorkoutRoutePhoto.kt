package com.example.climbit.photo

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.util.Size
import android.util.TypedValue
import androidx.exifinterface.media.ExifInterface
import java.io.File

class WorkoutRoutePhoto(val file: File) {
    val routeID: Long

    init {
        val nameParts = file.name.split("___")
        routeID = nameParts[0].toLong()
    }

    fun asBitmap(): Bitmap {
        return loadPhoto(null)
    }

    fun asBitmap(heightDp: Float): Bitmap {
        return loadPhoto(heightDp)
    }

    private fun loadPhoto(heightDp: Float?): Bitmap {
        val bitmap = if (file.extension == "mp4") {
            ThumbnailUtils.createVideoThumbnail(file, Size(512, 512), null)
        } else {
            BitmapFactory.decodeFile(file.absolutePath)
        }

        val orientation = ExifInterface(file).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return loadPhoto(bitmap, 90.0F, heightDp)
        }

        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return loadPhoto(bitmap, 180.0F, heightDp)
        }

        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return loadPhoto(bitmap, 270.0F, heightDp)
        }

        return loadPhoto(bitmap, 0.0F, heightDp)
    }

    private fun loadPhoto(source: Bitmap, angle: Float, heightDp: Float?): Bitmap {
        var scale = 1.0F

        heightDp?.also {
            val metrics = Resources.getSystem().displayMetrics
            val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, metrics)

            if (heightPx < source.height) {
                scale = heightPx / source.height
            }
        }

        val matrix = Matrix()
        matrix.postRotate(angle)
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
