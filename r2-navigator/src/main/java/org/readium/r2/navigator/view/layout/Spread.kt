package org.readium.r2.navigator.view.layout

sealed class Spread {

    data class SinglePage(val page: Int) : Spread()

    data class DoublePage(val left: Int?, val right: Int?) : Spread()

    companion object {
        const val SINGLE_PAGE_TYPE = 0
        const val DOUBLE_PAGE_TYPE = 1
    }
}