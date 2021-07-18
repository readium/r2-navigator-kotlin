/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.epub

import androidx.lifecycle.ViewModel
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.DecorationChange
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.changesByHref
import org.readium.r2.navigator.epub.extensions.javascriptForGroup
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.util.createViewModelFactory
import org.readium.r2.shared.publication.Link
import kotlin.reflect.KClass

internal class EpubNavigatorViewModel(
    val decorationTemplates: HtmlDecorationTemplates
) : ViewModel() {

    data class RunScriptCommand(val script: String, val scope: Scope) {
        sealed class Scope {
            object CurrentResource : Scope()
            object LoadedResources : Scope()
            data class Resource(val href: String) : Scope()
            data class WebView(val webView: R2BasicWebView) : Scope()
        }
    }

    fun onResourceLoaded(link: Link?, webView: R2BasicWebView): List<RunScriptCommand> {
        val cmds = mutableListOf<RunScriptCommand>()

        val styles = decorationTemplates.toJSON().toString()
            .replace("\\n", " ")
        cmds.add(RunScriptCommand(
            "readium.registerDecorationStyles($styles);",
            scope = RunScriptCommand.Scope.WebView(webView)
        ))

        if (link != null) {
            for ((group, decorations) in decorations) {
                val changes = decorations
                    .filter { it.locator.href == link.href }
                    .map { DecorationChange.Added(it) }

                val script = changes.javascriptForGroup(group, decorationTemplates) ?: continue
                cmds.add(RunScriptCommand(script, scope = RunScriptCommand.Scope.WebView(webView)))
            }
        }

        return cmds
    }

    // Selection

    fun clearSelection(): RunScriptCommand =
        RunScriptCommand(
            "window.getSelection().removeAllRanges();",
            scope = RunScriptCommand.Scope.CurrentResource
        )

    // Decorations

    /** Current decorations, indexed by the group name. */
    val decorations: MutableMap<String, List<Decoration>> = mutableMapOf()

    fun <T : Decoration.Style> supportsDecorationStyle(style: KClass<T>): Boolean =
        decorationTemplates.styles.containsKey(style)

    suspend fun applyDecorations(decorations: List<Decoration>, group: String): List<RunScriptCommand> {
        val source = this.decorations[group] ?: emptyList()
        val target = decorations.toList()
        this.decorations[group] = target

        val cmds = mutableListOf<RunScriptCommand>()

        if (target.isEmpty()) {
            cmds.add(RunScriptCommand(
                "readium.getDecorations('$group').clear();",
                scope = RunScriptCommand.Scope.LoadedResources
            ))
        } else {
            for ((href, changes) in source.changesByHref(target)) {
                val script = changes.javascriptForGroup(group, decorationTemplates) ?: continue
                cmds.add(RunScriptCommand(script, scope = RunScriptCommand.Scope.Resource(href)))
            }
        }

        return cmds
    }

    companion object {
        fun createFactory(decorationTemplates: HtmlDecorationTemplates) = createViewModelFactory {
            EpubNavigatorViewModel(decorationTemplates)
        }
    }
}