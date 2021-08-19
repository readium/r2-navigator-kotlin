package org.readium.r2.navigator.view

interface NavigatorListener {
    fun onTap()

    fun onLocationChanged()

    fun onHighlightActivated()
}