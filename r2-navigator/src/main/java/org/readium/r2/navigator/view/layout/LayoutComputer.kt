package org.readium.r2.navigator.view.layout

import org.readium.r2.shared.publication.presentation.Presentation

internal class LayoutComputer(val rtl: Boolean) {

    fun compute(pageSpreads: List<Presentation.Page?>): List<Spread> =
        if (rtl) computeRtlLayout(pageSpreads) else computeLtrLayout(pageSpreads)

    private fun computeLtrLayout(pageSpreads: List<Presentation.Page?>): List<Spread> {
        val layout = mutableListOf<Spread>()
        var spreadStarted = false

        for ((idx, page) in pageSpreads.withIndex()) {
            when (page) {
                null -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(idx - 1, idx))
                        spreadStarted = false
                    } else {
                        spreadStarted = true
                    }
                }
                Presentation.Page.CENTER -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(idx - 1, null))
                        spreadStarted = false
                    }
                    layout.add(Spread.SinglePage(idx))
                }
                Presentation.Page.LEFT -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(idx - 1, null))
                    }
                    spreadStarted = true
                }
                Presentation.Page.RIGHT -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(idx - 1, idx))
                    } else {
                        layout.add(Spread.DoublePage(null, idx))
                    }
                    spreadStarted = false
                }
            }
        }

        if (spreadStarted) {
            layout.add(Spread.DoublePage(pageSpreads.size - 1, null))
        }

        return layout
    }

    private fun computeRtlLayout(pageSpreads: List<Presentation.Page?>): List<Spread> {
        val layout = mutableListOf<Spread>()
        var spreadStarted = false

        for ((idx, page) in pageSpreads.withIndex()) {
            when (page) {
                null -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(idx, idx - 1))
                        spreadStarted = false
                    } else {
                        spreadStarted = true
                    }
                }
                Presentation.Page.CENTER -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(null, idx - 1))
                        spreadStarted = false
                    }
                    layout.add(Spread.SinglePage(idx))
                }
                Presentation.Page.RIGHT -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(null, idx - 1))
                    }
                    spreadStarted = true
                }
                Presentation.Page.LEFT -> {
                    if (spreadStarted) {
                        layout.add(Spread.DoublePage(idx, idx - 1))
                    } else {
                        layout.add(Spread.DoublePage(idx, null))
                    }
                    spreadStarted = false
                }
            }
        }

        if (spreadStarted) {
            layout.add(Spread.DoublePage(null, pageSpreads.size - 1))
        }

        return layout
    }
}