package org.readium.r2.navigator.view.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Size
import android.view.View
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.readium.r2.navigator.extensions.readAsBitmap
import org.readium.r2.navigator.view.ResourceAdapter
import org.readium.r2.navigator.view.SpreadAdapter
import org.readium.r2.navigator.view.layout.EffectiveReadingProgression
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import kotlin.math.max


internal class ImageSpreadAdapter(
    override val links: List<Link>,
    private val publication: Publication,
    private val readingProgression: EffectiveReadingProgression,
    private val errorBitmap: (Size) -> Bitmap,
    private val emptyBitmap: (Size) -> Bitmap
) : SpreadAdapter {

    private var bindingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun bind(view: View) {
        check(view is PhotoView)

        val oneMaxSize = Size(view.width / 2, view.height)

        bindingJob = scope.launch {
            val bitmaps = links.map {
                publication.get(it)
                    .readAsBitmap(oneMaxSize)
                    .getOrElse { errorBitmap(oneMaxSize) }
            }
            val image =
                when (bitmaps.size) {
                    0 -> emptyBitmap(oneMaxSize)
                    1 -> bitmaps[0]
                    2 -> mergeBitmaps(bitmaps[0], bitmaps[1])
                    else -> throw IllegalStateException()
                }
            view.setImageBitmap(image)
        }
    }

    override fun unbind() {
        bindingJob?.cancel()
        bindingJob = null
    }

    override fun resourceAdapters(view: View): List<ResourceAdapter> {
        return links.map {
            ImageResourceAdapter(it, view)
        }
    }

    private fun mergeBitmaps(first: Bitmap, second: Bitmap): Bitmap {
        val left = when(readingProgression) {
            EffectiveReadingProgression.LTR -> first
            EffectiveReadingProgression.RTL -> second
            else -> throw java.lang.IllegalStateException()
        }
        val right = when(readingProgression) {
            EffectiveReadingProgression.LTR -> second
            EffectiveReadingProgression.RTL -> first
            else -> throw java.lang.IllegalStateException()
        }
        val width = left.width + right.width
        val height = max(left.height, right.height)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        val canvas = Canvas(result)
        canvas.drawBitmap(left, 0f, 0f, null)
        canvas.drawBitmap(right, left.width.toFloat(), 0f, null)
        return result
    }
}
