package org.readium.r2.navigator.interfaces

import android.content.SharedPreferences
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Publication

interface R2EpubPageFragmentListener {

    val publication: Publication
    val preferences: SharedPreferences
    val publicationIdentifier: String
    val resourcePager: R2ViewPager
    var allowToggleActionBar: Boolean

    fun onPageEnded(endReached: Boolean)
    fun onPageChanged(index: Int, numPages: Int, url: String?)
    fun previousResource(smoothScroll: Boolean)
    fun nextResource(smoothScroll: Boolean)
    fun storeProgression(locations: Locations?)
    fun toggleActionBar()

}