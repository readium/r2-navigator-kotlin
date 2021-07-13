/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.publication.Locator
import kotlin.reflect.KClass

/**
 * A navigator able to render arbitrary decorations over a publication.
 */
interface DecorableNavigator {

    /**
     * Declares the current state of the decorations in the given decoration [group].
     *
     * The Navigator will decide when to actually render each decoration efficiently. Your only
     * responsibility is to submit the updated list of decorations when there are changes.
     * Name each decoration group as you see fit. A good practice is to use the name of the feature
     * requiring decorations, e.g. annotation, search, tts, etc.
     */
    fun applyDecorations(decorations: List<Decoration>, group: String)

    /**
     * Indicates whether the Navigator supports the given decoration [style] class.
     *
     * You should check whether the Navigator supports drawing the decoration styles required by a
     * particular feature before enabling it. For example, underlining an audiobook does not make
     * sense, so an Audiobook Navigator would not support the `underline` decoration style.
     */
    fun <T: Decoration.Style> supportsDecorationStyle(style: KClass<T>): Boolean
}

/**
 * A decoration is a user interface element drawn on top of a publication. It associates a [style]
 * to be rendered with a discrete [locator] in the publication.
 *
 * For example, decorations can be used to draw highlights, images or buttons.
 *
 * @param id An identifier for this decoration. It must be unique in the group the decoration is applied to.
 * @param locator Location in the publication where the decoration will be rendered.
 * @param style Declares the look and feel of the decoration.
 * @param extras Additional context data specific to a reading app. Readium does not use it.
 */
@Parcelize
data class Decoration(
    val id: DecorationId,
    val locator: Locator,
    val style: Style,
    val extras: Bundle = Bundle(),
) : JSONable, Parcelable {

    /**
     * The Decoration Style determines the look and feel of a decoration once rendered by a
     * Navigator.
     *
     * It is media type agnostic, meaning that each Navigator will translate the style into a set of
     * rendering instructions which makes sense for the resource type.
     */
    interface Style : Parcelable {
        @Parcelize
        class Highlight(@ColorInt val tint: Int? = null) : Style
        @Parcelize
        class Underline(@ColorInt val tint: Int? = null) : Style
    }

    override fun toJSON() = JSONObject().apply {
        put("id", id)
        put("locator", locator.toJSON())
        putOpt("style", style::class.qualifiedName)
    }
}

/** Unique identifier for a decoration. */
typealias DecorationId = String
