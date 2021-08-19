package org.readium.r2.navigator.view

import android.view.View
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

interface SpreadAdapter {

    val links: List<Link>

    fun bind(view: View)

    fun unbind()

    fun scrollTo(locations: Locator.Locations, view: View) {}

    fun applySettings() {}

    fun resourceAdapters(view: View): List<ResourceAdapter>
}