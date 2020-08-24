/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.audio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.readium.r2.navigator.MediaNavigator
import org.readium.r2.navigator.R
import org.readium.r2.navigator.media.ExoMediaPlayer
import org.readium.r2.navigator.media.MediaPlayer
import org.readium.r2.navigator.media.MediaSessionNavigator
import org.readium.r2.shared.AudioSupport
import org.readium.r2.shared.FragmentNavigator
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.extensions.putPublication
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

@AudioSupport
open class AudioActivity : AppCompatActivity() {

    private lateinit var publication: Publication
    private lateinit var player: MediaPlayer

    val navigator: MediaNavigator get() =
        supportFragmentManager.findFragmentById(R.id.audio_navigator) as MediaNavigator

    @OptIn(FragmentNavigator::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        publication = intent.getPublication(this)
        val initialLocator = intent.getParcelableExtra("locator") as? Locator

        val mediaSession = MediaSessionCompat(this, /* log tag */ "${javaClass.simpleName}.mediaSession").apply {
//            if (activityIntent != null) {
//                setSessionActivity(activityIntent)
//            }
        }

        player = ExoMediaPlayer.Factory().create(this, mediaSession, publication, initialLocator).apply {
            prepare(playWhenReady = true)
        }

        val mediaNavigator = MediaSessionNavigator(mediaSession, publication)
        mediaNavigator.currentLocator.observe(this, Observer {
            Timber.e("CURRENT ${it}")
        })

        supportFragmentManager.fragmentFactory = AudioNavigatorFragment.Factory(mediaNavigator)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.r2_audio_activity)
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    companion object {

        private const val EXTRA_LOCATOR = "locator"

        fun createIntent(context: Context, publication: Publication, initialLocator: Locator?): Intent = Intent(context, AudioActivity::class.java).apply {
            putPublication(publication)
            putExtra(EXTRA_LOCATOR, initialLocator)
        }

    }

}
