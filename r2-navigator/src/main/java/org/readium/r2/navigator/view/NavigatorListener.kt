package org.readium.r2.navigator.view

import android.graphics.PointF
import org.readium.r2.shared.publication.Locator

interface NavigatorListener {
    fun onTap(point: PointF): Boolean

    fun onLocationChanged(newLocation: Locator)

    fun onHighlightActivated()
}