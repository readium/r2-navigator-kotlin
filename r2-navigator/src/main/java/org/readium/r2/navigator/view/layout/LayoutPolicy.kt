package org.readium.r2.navigator.view.layout

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

interface LayoutPolicy {

    fun resolveSpreadHint(link: Link, publication: Publication, isLandscape: Boolean): Boolean

    fun resolveReadingProgression(publication: Publication): EffectiveReadingProgression
}