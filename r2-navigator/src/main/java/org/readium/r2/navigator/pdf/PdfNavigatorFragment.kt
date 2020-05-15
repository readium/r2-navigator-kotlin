/*
 * Module: r2-navigator-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pdf

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.extensions.page
import org.readium.r2.navigator.extensions.urlToHref
import org.readium.r2.shared.format.MediaType
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

class PdfNavigatorFragment(
    private val publication: Publication,
    private val initialLocator: Locator? = null,
    private val listener: Navigator.Listener? = null
) : Fragment(), Navigator {

    lateinit var pdfView: PDFView

    private var currentHref: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        pdfView = PDFView(context, null)

        if (initialLocator != null) {
            go(initialLocator)
        } else {
            go(publication.readingOrder.first())
        }

        return pdfView
    }

    private fun goToHref(href: String, page: Int, animated: Boolean = false, completion: () -> Unit = {}): Boolean {
        val url = publication.urlToHref(href) ?: return false

        if (currentHref == href) {
            pdfView.jumpTo(page, animated)
            completion()

        } else {
            val listener = this
            lifecycleScope.launch {
                try {
                    // Android forbids network requests on the main thread by default, so we have to
                    // do that in the IO dispatcher.
                    withContext(Dispatchers.IO) {
                        pdfView.fromStream(url.openStream())
                            .defaultPage(page)
                            .spacing(10)
                            .pageFitPolicy(FitPolicy.WIDTH)
                            .onRender { _ -> completion() }
                            .onPageChange { page, pageCount -> onPageChanged(page, pageCount) }
                            .onTap { event -> onTap(event) }
                            .load()
                    }

                    currentHref = href

                } catch (e: Exception) {
                    Timber.e(e)
                    completion()
                }
            }
        }

        return true
    }

    // Navigator

    override val currentLocator: LiveData<Locator?> get() = _currentLocator
    private val _currentLocator = MutableLiveData<Locator?>(null)

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean =
        goToHref(locator.href, locator.locations.page ?: 0, animated, completion)

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean =
        goToHref(link.href, 0, animated, completion)

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pdfView.currentPage
        val pageCount = pdfView.pageCount
        if (page >= (pageCount - 1)) return false

        pdfView.jumpTo(page + 1, animated)
        completion()
        return true
    }


    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        val page = pdfView.currentPage
        if (page <= 0) return false

        pdfView.jumpTo(page - 1, animated)
        completion()
        return true
    }

    // PdfView Listeners
    private fun onPageChanged(page: Int, pageCount: Int) {
        val href = currentHref ?: return
        val link = publication.linkWithHref(href)
        val progression = if (pageCount > 0) page / pageCount.toDouble() else 0.0
        // FIXME: proper position and totalProgression
        _currentLocator.value = Locator(
            href = href,
            type = link?.type ?: MediaType.PDF.toString(),
            title = link?.title,
            locations = Locator.Locations(
                fragments = listOf("page=${page + 1}"),
                position = page + 1,
                progression = progression,
                totalProgression = progression
            )
        )
    }

    private fun onTap(e: MotionEvent?): Boolean {
        e ?: return false
        val listener = (listener as? Navigator.VisualListener) ?: return false
        return listener.onTap(PointF(e.x, e.y))
    }

}