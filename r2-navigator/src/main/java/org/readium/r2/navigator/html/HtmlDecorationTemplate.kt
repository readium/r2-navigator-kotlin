/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.html

import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.Decoration.Style
import org.readium.r2.shared.JSONable
import kotlin.reflect.KClass

/**
 * An [HtmlDecorationTemplate] renders a [Decoration] into a set of HTML elements and associated
 * stylesheet.
 *
 * @param layout Determines the number of created HTML elements and their position relative to the
 *        matching DOM range.
 * @param width Indicates how the width of each created HTML element expands in the viewport.
 * @param element Closure used to generate a new HTML element for the given [Decoration]. Several
 *        elements will be created for a single decoration when using the BOXES layout.
 *        The Navigator will automatically position the created elements according to the
 *        decoration's Locator. The template is only responsible for the look and feel of the
 *        generated elements.
 *        Every child elements with a `data-activable="1"` HTML attribute will handle tap events.
 *        If no element has this attribute, the root element will handle taps.
 * @param stylesheet A CSS stylesheet which will be injected in the resource, which can be
 *        referenced by the created elements. Make sure to use unique identifiers for your classes
 *        and IDs to avoid conflicts with the HTML resource itself. Best practice is to prefix with
 *        your app name. r2- and readium- are reserved by the Readium toolkit.
 */
data class HtmlDecorationTemplate(
    val layout: Layout,
    val width: Width = Width.WRAP,
    val element: (Decoration) -> String = { "<div/>" },
    val stylesheet: String? = null,
) : JSONable {

    /**
     * Determines the number of created HTML elements and their position relative to the matching
     * DOM range.
     */
    @Parcelize
    enum class Layout(val value: String) : Parcelable {
        /** A single HTML element covering the smallest region containing all CSS border boxes. */
        BOUNDS("bounds"),
        /** One HTML element for each CSS border box (e.g. line of text). */
        BOXES("boxes");
    }

    /**
     * Indicates how the width of each created HTML element expands in the viewport.
     */
    @Parcelize
    enum class Width(val value: String) : Parcelable {
        /** Smallest width fitting the CSS border box. */
        WRAP("wrap"),
        /** Fills the bounds layout. */
        BOUNDS("bounds"),
        /** Fills the anchor page, useful for dual page. */
        VIEWPORT("viewport"),
        /** Fills the whole viewport. */
        PAGE("page");
    }

    private data class Padding(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0)

    override fun toJSON() = JSONObject().apply {
        put("layout", layout.value)
        put("width", width.value)
        putOpt("stylesheet", stylesheet)
    }

    companion object {

        /**
         * Creates the default list of decoration styles with associated HTML templates.
         */
        fun defaultStyles(
            @ColorInt defaultTint: Int = Color.YELLOW,
            lineWeight: Int = 2,
            cornerRadius: Int = 3,
            alpha: Double = 0.3
        ) = HtmlDecorationTemplates {
            set(Style.Highlight::class, highlight(defaultTint = defaultTint, cornerRadius = cornerRadius, alpha = alpha))
            set(Style.Underline::class, underline(defaultTint = defaultTint, lineWeight = lineWeight, cornerRadius = cornerRadius))
        }


        /** Creates a new decoration template for the highlight style. */
        fun highlight(@ColorInt defaultTint: Int, cornerRadius: Int, alpha: Double): HtmlDecorationTemplate {
            val className = createUniqueClassName("highlight")
            val padding = Padding(left = 1, right = 1)
            return HtmlDecorationTemplate(
                layout = Layout.BOXES,
                element = { decoration ->
                    val style = decoration.style as? Style.Highlight
                    val tint = style?.tint ?: defaultTint
                    var extraStyle = ""
                    if (style?.isActive == true) {
                        extraStyle += " border-bottom: 2px solid ${tint.toCss()}"
                    }
                    """
                    <div class="$className" style="background-color: ${tint.toCss(alpha = alpha)} !important; $extraStyle"/>"
                    """
                },
                stylesheet = """
                    .$className {
                        margin-left: ${-padding.left}px;
                        padding-right: ${padding.left + padding.right}px;
                        margin-top: ${-padding.top}px;
                        padding-bottom: ${padding.top + padding.bottom}px;
                        border-radius: ${cornerRadius}px;
                        box-sizing: border-box;
                    }
                    """
            )
        }

        /** Creates a new decoration template for the underline style. */
        fun underline(@ColorInt defaultTint: Int, lineWeight: Int, cornerRadius: Int): HtmlDecorationTemplate {
            val className = createUniqueClassName("underline")
            return HtmlDecorationTemplate(
                layout = Layout.BOXES,
                element = { decoration ->
                    val style = decoration.style as? Style.Underline
                    val tint = style?.tint ?: defaultTint
                    """
                    <div><span class="$className" style="background-color: ${tint.toCss()} !important"/></div>"
                    """
                },
                stylesheet = """
                    .$className {
                        display: inline-block;
                        width: 100%;
                        height: ${lineWeight}px;
                        border-radius: ${cornerRadius}px;
                        vertical-align: text-bottom;
                    }
                    """
            )
        }

        private var classNamesId = 0;
        private fun createUniqueClassName(key: String): String =
            "r2-$key-${++classNamesId}"
    }
}

class HtmlDecorationTemplates private constructor(
    internal val styles: MutableMap<KClass<*>, HtmlDecorationTemplate> = mutableMapOf()
): JSONable {

    operator fun <S : Style> get(style: KClass<S>): HtmlDecorationTemplate? =
        styles[style]

    operator fun <S : Style> set(style: KClass<S>, template: HtmlDecorationTemplate) {
        styles[style] = template
    }

    override fun toJSON() = JSONObject(
        styles.entries.associate {
            it.key.qualifiedName!! to it.value.toJSON()
        }
    )

    fun copy() = HtmlDecorationTemplates(styles.toMutableMap())

    companion object {
        operator fun invoke(build: HtmlDecorationTemplates.() -> Unit): HtmlDecorationTemplates =
            HtmlDecorationTemplates().apply(build)
    }
}

/**
 * Converts the receiver color int to a CSS expression.
 *
 * @param alpha When set, overrides the actual color alpha.
 */
fun @receiver:ColorInt Int.toCss(alpha: Double? = null): String {
    val r = Color.red(this)
    val g = Color.green(this)
    val b = Color.blue(this)
    val a = alpha ?: Color.alpha(this).toDouble() / 255
    return "rgba($r, $g, $b, $a)"
}