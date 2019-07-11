/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.navigator.extensions.layoutDirectionIsRTL
import org.readium.r2.navigator.interfaces.R2EpubPageFragmentListener
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.navigator.pager.R2ViewPager
import org.readium.r2.shared.Locations
import org.readium.r2.shared.Publication
import kotlin.coroutines.CoroutineContext


class R2CbzActivity : AppCompatActivity(), CoroutineScope, R2EpubPageFragmentListener {
    /**
     * Context of this scope.
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override lateinit var preferences: SharedPreferences
    override lateinit var resourcePager: R2ViewPager
    override lateinit var publication: Publication
    override lateinit var publicationIdentifier: String
    override var allowToggleActionBar = true

    private lateinit var publicationPath: String
    private lateinit var cbzName: String

    var resources = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_r2_viewpager)

        preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        resourcePager = findViewById(R.id.resourcePager)

        publicationPath = intent.getStringExtra("publicationPath")
        publication = intent.getSerializableExtra("publication") as Publication
        cbzName = intent.getStringExtra("cbzName")
        publicationIdentifier = publication.metadata.identifier
        title = publication.metadata.title

        for (link in publication.pageList) {
            resources.add(link.href.toString())
        }

        val index = preferences.getInt("$publicationIdentifier-document", 0)

        val adapter = R2PagerAdapter(supportFragmentManager, resources, publication.metadata.title, Publication.TYPE.CBZ, publicationPath, this)

        resourcePager.adapter = adapter

        if (index == 0) {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resources.size - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = index
            }
        } else {
            resourcePager.currentItem = index
        }

        toggleActionBar()
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        // TODO we could add a thumbnail view here
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return super.onOptionsItemSelected(item)
//    }

    override fun onPause() {
        super.onPause()
        val publicationIdentifier = publication.metadata.identifier
        val documentIndex = resourcePager.currentItem
        preferences.edit().putInt("$publicationIdentifier-document", documentIndex).apply()
    }

    /**
     * storeProgression() : save in the preference the last progression in the spine item
     */
    override fun storeProgression(locations: Locations?) {
        //
    }

    /**
     * onPageChanged() : when page changed inside webview
     */
    override fun onPageChanged(index: Int, numPages: Int, url: String?) {
        // optional
    }

    /**
     * onPageEnded() : when page ended inside webview
     */
    override fun onPageEnded(end: Boolean) {
        // optional
    }

    /**
     * toggleActionBar() : toggle actionbar when touch center
     */
    override fun toggleActionBar() {
        launch {
            if (allowToggleActionBar) {
                supportActionBar?.let {
                    if (it.isShowing) {
                        resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                or View.SYSTEM_UI_FLAG_IMMERSIVE)
                    } else {
                        resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    }
                }
            }
        }
    }

    /**
     * nextResource() : return next resource
     */
    override fun nextResource(smoothScroll: Boolean) {
        launch {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            }
        }
    }

    /**
     * nextResource() : return previous resource
     */
    override fun previousResource(smoothScroll: Boolean) {
        launch {
            if (layoutDirectionIsRTL()) {
                // The view has RTL layout
                resourcePager.currentItem = resourcePager.currentItem + 1
            } else {
                // The view has LTR layout
                resourcePager.currentItem = resourcePager.currentItem - 1
            }

        }
    }

}

