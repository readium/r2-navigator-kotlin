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

internal class ImageSingleAdapter(
    private val publication: Publication,
    private val errorBitmap: Lazy<Bitmap>
) : SpreadAdapter, CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    override fun supportsSpread(spread: Spread): Boolean {
        if (spread !is Spread.SinglePage) {
            return false
        }

        val link = publication.readingOrder[spread.page]
        return link.mediaType.isBitmap
    }

    override fun createView(context: Context): View {
        return PhotoView(context)
    }

    override fun bindSpread(spread: Spread, view: View): Job {
        check(view is PhotoView)
        check (spread is Spread.SinglePage)

        val link = publication.readingOrder[spread.page]
        val maxSize = Size(view.width, view.height)

        return launch {
            publication.get(link)
                .readAsBitmap(maxSize)
                .getOrElse { errorBitmap.value }
                .let { view.setImageBitmap(it) }
        }
    }
}
