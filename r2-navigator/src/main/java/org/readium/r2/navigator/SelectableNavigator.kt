/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.graphics.Rect
import android.text.Selection
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Locator

/**
 * A navigator supporting user selection.
 */
interface SelectableNavigator : Navigator {

    /** Currently selected content. */
    val currentSelection: StateFlow<Selection?>

    /** Clears the current selection. */
    fun clearSelection()
}

/**
 * Represents a user content selection in a navigator.
 *
 * @param locator Location of the user selection in the publication.
 * @param rect Frame of the bounding rect for the selection, in the coordinate of the navigator
 *        view. This is only useful in the context of a VisualNavigator.
 */
data class Selection(
    val locator: Locator,
    val rect: Rect?,
)