package org.readium.r2.navigator.view

import android.content.Context
import android.content.res.Configuration
import org.readium.r2.navigator.settings.PresentationProperties
import org.readium.r2.navigator.settings.PresentationSettings
import org.readium.r2.navigator.view.image.ImageSpreadAdapterFactory
import org.readium.r2.navigator.view.layout.EffectiveReadingProgression
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.ReadingProgression
import org.readium.r2.shared.publication.presentation.Presentation

internal class NavigatorEngine(
    private val context: Context,
    private val publication: Publication,
    private val links: List<Link>,
    private val configuration: NavigatorConfiguration,
    private val readingView: ReadingView,
) {
    private val adapterFactories = createAdapterFactories()

    var listener: NavigatorListener? = null

    init {
        this.readingView.setOnScrollListener {
            this.listener?.onLocationChanged(this.currentLocation)
        }
    }

    var adapter: NavigatorAdapter = this.renewAdapter()
        private set

    var properties: PresentationProperties = this.renewProperties()
        private set

    var settings: PresentationSettings = PresentationSettings()
        set(value) {
            val previous = field
            field = value
            if (previous.spread != value.spread) {
                this.renewAdapter()
            }
            if (previous.continuous != value.continuous || previous.readingProgression != value.readingProgression) {
                this.renewProperties()
            }
            if (previous != value) {
                this.updateAdapterSettings()
            }
        }

    val currentLocation: Locator
        get() {
            val firstVisiblePosition = this.readingView.findFirstVisiblePosition()
            val firstVisibleView = this.readingView.findViewByPosition(firstVisiblePosition)!!
            val firstResource = this.adapter.resourcesForView(firstVisibleView).first()
            return firstResource.currentLocation
        }

    private fun renewProperties(): PresentationProperties {
        val readingProgression = this.computeReadingProgression()
        this.readingView.setReadingProgression(readingProgression)
        val continuous = this.computeContinuous()
        this.readingView.setContinuous(continuous)
        return PresentationProperties(
            readingProgression = readingProgression,
            continuous = continuous,
        )
    }

    private fun computeReadingProgression(): EffectiveReadingProgression =
        when (settings.readingProgression) {
            ReadingProgression.RTL -> EffectiveReadingProgression.RTL
            ReadingProgression.LTR -> EffectiveReadingProgression.LTR
            ReadingProgression.TTB -> EffectiveReadingProgression.TTB
            ReadingProgression.BTT -> EffectiveReadingProgression.BTT
            ReadingProgression.AUTO ->  configuration.layoutPolicy.resolveReadingProgression(publication)
        }

    private fun computeContinuous() =
        settings.continuous == true || configuration.layoutPolicy.resolveContinuous(publication)

    private fun renewAdapter(): NavigatorAdapter {
        adapter = NavigatorAdapter(context, links, adapterFactories)
        this.readingView.setAdapter(adapter)
        return adapter
    }

    private fun updateAdapterSettings()  {
        val adapterSettings = this.computeAdapterSettings()
        this.adapter.applySettings(adapterSettings)
    }

    private fun computeAdapterSettings(): NavigatorAdapter.Settings =
        NavigatorAdapter.Settings(
            fontSize = settings.fontSize
        )

    private fun createAdapterFactories(): List<SpreadAdapterFactory> {
        val imageSpreadAdapterFactory =
            ImageSpreadAdapterFactory(
                publication,
                properties.readingProgression,
                this::resolveSpreadHint,
                configuration.errorBitmap,
                configuration.emptyBitmap
            )

        return listOf(
            imageSpreadAdapterFactory
        )
    }

    private fun resolveSpreadHint(link: Link): Boolean {
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return when (settings.spread) {
            Presentation.Spread.AUTO -> configuration.layoutPolicy.resolveSpreadHint(link, publication, isLandscape)
            Presentation.Spread.BOTH -> true
            Presentation.Spread.NONE -> false
            Presentation.Spread.LANDSCAPE -> isLandscape
        }
    }

    suspend fun goTo(locator: Locator) {
        val spreadIndex = this.adapter.positionForHref(locator.href)
        this.readingView.scrollToPosition(spreadIndex)
        val view = checkNotNull(this.readingView.findViewByPosition(spreadIndex))
        this.adapter.scrollTo(locator.locations, view, spreadIndex)
    }
}
