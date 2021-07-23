package org.readium.r2.navigator.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapFactory : android.graphics.BitmapFactory() {

    suspend fun decodeByteArray(data: ByteArray, options: BitmapFactory.Options? = null): Bitmap =
        withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        }

    suspend fun decodeByteArrayFitting(data: ByteArray, maxSize: Size) =
        BitmapFactory.Options().run {
            inJustDecodeBounds = true
            decodeByteArray(data, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(maxSize.width, maxSize.height, this.outWidth, this.outHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            decodeByteArray(data, this)
        }

    private fun calculateInSampleSize(reqWidth: Int, reqHeight: Int, width: Int, height: Int): Int =
        when {
            reqHeight <= height && reqWidth <= width -> 1
            reqHeight == 0 -> width / reqWidth
            reqWidth == 0 -> height / reqHeight
            else -> Math.min(height / reqHeight, width / reqWidth)
        }
}
