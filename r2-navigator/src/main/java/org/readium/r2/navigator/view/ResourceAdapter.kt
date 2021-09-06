package org.readium.r2.navigator.view

import org.readium.r2.shared.publication.Locator

interface ResourceAdapter {

    val currentLocation: Locator
}