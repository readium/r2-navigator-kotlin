package org.readium.r2.navigator.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import org.readium.r2.navigator.settings.PresentationSettings
import org.readium.r2.navigator.view.image.ImageSpreadAdapterFactory
import org.readium.r2.navigator.view.layout.EffectiveReadingProgression
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

class NavigatorView(context: Context, attributes: AttributeSet) : View(context, attributes) {

    private val readingView: ReadingView = ReadingView(context, attributes)
    private var navAdapter: NavigatorAdapter? = null
    private val adapter: NavigatorAdapter get() = checkNotNull(navAdapter)
    private var publication: Publication? = null

    /* private val loadedViews: List<View> get() =
        (0 until this.adapter.itemCount).mapNotNull { this.readingView.findViewByPosition(it) }
     */

    var configuration: NavigatorConfiguration = NavigatorConfiguration()

    var settings: PresentationSettings = PresentationSettings()
        set(value) {
            val previous = field
            field = value
            if (previous.spread != value.spread) {
                this.publication?.let { this.loadPublication(it) }
            }
            if (previous.continuous != value.continuous) {
                this.publication?.let {
                    val continuous = this.computeContinuous(value.continuous, it)
                    this.readingView.setContinuous(continuous)
                }
            }
            if (previous.readingProgression != value.readingProgression) {
                this.publication?.let {
                    val progression = this.computeReadingProgression(value.readingProgression, it)
                    this.readingView.setReadingProgression(progression)
                }
            }
            if (previous != value) {
                val adapterSettings = this.computeAdapterSettings(value)
                if (adapterSettings != this.computeAdapterSettings(previous)) {
                    this.adapter.applySettings(adapterSettings)
                }
            }
        }

    var listener: NavigatorListener? = null

    /*
    val visibleResources: List<ResourceAdapter> get() =
        this.adapter.resourcesForView(this.visibleView)

    val loadedResources: List<ResourceAdapter> get() =
        this.loadedViews.flatMap { this.adapter.resourcesForView(it) }
     */

    fun loadPublication(publication: Publication) {
        this.publication = publication
        val adapterFactories = createAdapterFactories(publication)
        val navAdapter = NavigatorAdapter(context, publication.readingOrder, adapterFactories)
        this.navAdapter = navAdapter
        this.readingView.setAdapter(navAdapter)
        val continuous = this.computeContinuous(settings.continuous, publication)
        this.readingView.setContinuous(continuous)
        val readingProgression = this.computeReadingProgression(settings.readingProgression, publication)
        this.readingView.setReadingProgression(readingProgression)
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
        this.readingView.scrollToPosition(spreadIndex)
        val view = checkNotNull(this.readingView.findViewByPosition(spreadIndex))
        this.adapter.scrollTo(locator.locations, view, spreadIndex)
    }

    private fun computeReadingProgression(setting: ReadingProgression, publication: Publication): EffectiveReadingProgression =
        when (setting) {
            ReadingProgression.RTL -> EffectiveReadingProgression.RTL
            ReadingProgression.LTR -> EffectiveReadingProgression.LTR
            ReadingProgression.TTB -> EffectiveReadingProgression.TTB
            ReadingProgression.BTT -> EffectiveReadingProgression.BTT
            ReadingProgression.AUTO ->  configuration.layoutPolicy.resolveReadingProgression(publication)
        }

    private fun computeContinuous(setting: Boolean?, publication: Publication) =
        setting == true || configuration.layoutPolicy.resolveContinuous(publication)

    private fun computeAdapterSettings(settings: PresentationSettings): NavigatorAdapter.Settings =
        NavigatorAdapter.Settings(
            fontSize = settings.fontSize
        )
}

