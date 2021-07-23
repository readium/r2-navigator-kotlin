package org.readium.r2.navigator.view.image

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.View
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.readium.r2.navigator.extensions.readAsBitmap
import org.readium.r2.navigator.view.SpreadAdapter
import org.readium.r2.navigator.view.layout.Spread
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import kotlin.coroutines.CoroutineContext

internal class ImageDoubleAdapter(
    private val publication: Publication,
    private val errorBitmap: Lazy<Bitmap>
) : SpreadAdapter, CoroutineScope {

    private val emptyBitmap by lazy {
        Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8)
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    override fun supportsSpread(spread: Spread): Boolean {
        if (spread !is Spread.DoublePage) {
            return false
        }

        val leftMediaType = spread.left?.let { publication.readingOrder[it] }?.mediaType
        val rightMediaType = spread.right?.let { publication.readingOrder[it] }?.mediaType

        val leftIsBitmap = leftMediaType == null || leftMediaType.isBitmap
        val rightIsBitmap = rightMediaType == null || rightMediaType.isBitmap

        return leftIsBitmap && rightIsBitmap
    }

    override fun createView(context: Context): View {
        return PhotoView(context)
    }

    override fun bindSpread(spread: Spread, view: View): Job {
        check(view is PhotoView)
        check (spread is Spread.DoublePage)

        val oneMaxSize = Size(view.width / 2, view.height)

        return launch {
            val leftImage =
                if (spread.left == null) {
                    emptyBitmap
                } else
                    spread.left
                        .let { publication.readingOrder[it] }
                        .let { publication.get(it) }
                        .readAsBitmap(oneMaxSize)
                        .getOrElse { errorBitmap.value }
            val rightImage =
                if (spread.right == null) {
                    emptyBitmap
                } else
                    spread.right
                        .let { publication.readingOrder[it] }
                        .let { publication.get(it) }
                        .readAsBitmap(oneMaxSize)
                        .getOrElse { errorBitmap.value }

            val doublePage = mergeBitmaps(leftImage, rightImage)
            view.setImageBitmap(doublePage)
        }
    }
    private fun mergeBitmaps(leftImage: Bitmap, rightImage: Bitmap): Bitmap {
        //TODO
        throw NotImplementedError()
    }
}
