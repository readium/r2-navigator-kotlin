package org.readium.r2.navigator.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import org.readium.r2.navigator.settings.PresentationSettings
import org.readium.r2.navigator.view.image.ImageSpreadAdapterFactory
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.presentation.Presentation

class NavigatorView(context: Context, attributes: AttributeSet) : View(context, attributes) {

    private val viewPager: ViewPager2 = ViewPager2(context)
    private var navAdapter: NavigatorAdapter? = null
    private val adapter: NavigatorAdapter get() = checkNotNull(navAdapter)
    private var publication: Publication? = null

    private val visibleView: View
        get() = checkNotNull(this.viewPager.findViewWithTag(this.viewPager.currentItem))

    private val loadedViews: List<View> get() =
        (0 until this.adapter.itemCount).mapNotNull { this.viewPager.findViewWithTag(it) }

    var configuration: NavigatorConfiguration = NavigatorConfiguration()

    var settings: PresentationSettings = PresentationSettings()
        set(value) {
            val previous = field
            field = value
            if (previous.spread != value.spread) {
                this.publication?.let { this.loadPublication(it) }
            }
        }

    var listener: NavigatorListener? = null

    val visibleResources: List<ResourceAdapter> get() =
        this.adapter.resourcesForView(this.visibleView)

    val loadedResources: List<ResourceAdapter> get() =
        this.loadedViews.flatMap { this.adapter.resourcesForView(it) }

    fun loadPublication(publication: Publication) {
        this.publication = publication
        val adapterFactories = createAdapterFactories(publication)
        this.navAdapter = NavigatorAdapter(context, publication.readingOrder, adapterFactories)
        this.viewPager.adapter = this.adapter
        this.viewPager.currentItem = 0
    }

    private fun createAdapterFactories(publication: Publication): List<SpreadAdapterFactory> {
        val readingProgression = configuration.layoutPolicy.resolveReadingProgression(publication)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val resolveSpreadHint = { link: Link ->
            when (settings.spread) {
                Presentation.Spread.AUTO -> configuration.layoutPolicy.resolveSpreadHint(link, publication, isLandscape)
                Presentation.Spread.BOTH -> true
                Presentation.Spread.NONE -> false
                Presentation.Spread.LANDSCAPE -> isLandscape
            }
        }

        val imageSpreadAdapterFactory =
            ImageSpreadAdapterFactory(
                publication,
                readingProgression,
                resolveSpreadHint,
                configuration.errorBitmap,
                configuration.emptyBitmap
            )

        return listOf(
            imageSpreadAdapterFactory
        )
    }

    suspend fun applyDecorations(decorations: List<Any>, group: String) {

    }

    suspend fun goTo(locator: Locator) {
        val spreadIndex = this.adapter.positionForHref(locator.href)
        this.viewPager.currentItem = spreadIndex
        this.adapter.scrollTo(locator.locations, visibleView, spreadIndex)
    }
}

