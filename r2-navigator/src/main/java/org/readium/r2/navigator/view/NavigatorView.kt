package org.readium.r2.navigator.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import org.readium.r2.navigator.view.html.HtmlDoubleAdapter
import org.readium.r2.navigator.view.html.HtmlSingleAdapter
import org.readium.r2.navigator.view.image.ImageDoubleAdapter
import org.readium.r2.navigator.view.image.ImageSingleAdapter
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class NavigatorView(context: Context, attributes: AttributeSet) : View(context, attributes) {

    interface Listener {
        fun onTap()

        fun onLocationChanged()

        fun onHighlightActivated()
    }

    data class Settings(
        val errorBitmap: Lazy<Bitmap> = lazy { Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8) }
    )

    var settings: Settings = Settings()
    var listener: Listener? = null

    private val viewPager: ViewPager2 = ViewPager2(context)
    private var adapter: NavigatorAdapter? = null

    private val currentItem: Int = 0

    suspend fun loadPublication(publication: Publication) {
        val adapters: List<SpreadAdapter> = listOf(
            ImageSingleAdapter(publication, settings.errorBitmap),
            ImageDoubleAdapter(publication, settings.errorBitmap),
            HtmlSingleAdapter(),
            HtmlDoubleAdapter()
        )
        this.adapter = NavigatorAdapter(context, adapters, publication)
        this.viewPager.adapter = this.adapter
        this.viewPager.currentItem = 0
    }

    suspend fun goTo(locator: Locator) {
        val adapter = checkNotNull(this.adapter)
        val spreadIndex = adapter.positionForHref(locator.href)
        this.viewPager.currentItem = spreadIndex
        adapter.scrollTo(locator.locations, currentView)
    }

    private val currentView: View
        get() = this.viewPager.findViewWithTag(this.viewPager.currentItem)
}

