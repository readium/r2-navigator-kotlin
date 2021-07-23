package org.readium.r2.navigator.view

import android.content.Context
import android.view.View
import kotlinx.coroutines.Job
import org.readium.r2.navigator.view.layout.Spread
import org.readium.r2.shared.publication.Locator

interface SpreadAdapter {

    fun supportsSpread(spread: Spread): Boolean

    fun createView(context: Context): View

    fun bindSpread(spread: Spread, view: View): Job

    fun scrollTo(locations: Locator.Locations, view: View) {}
}