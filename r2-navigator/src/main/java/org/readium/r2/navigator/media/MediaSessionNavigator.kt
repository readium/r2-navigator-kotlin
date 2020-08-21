/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.app.PendingIntent
import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import org.readium.r2.navigator.MediaNavigator
import org.readium.r2.navigator.extensions.combine
import org.readium.r2.navigator.extensions.isPlaying
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.shared.publication.*
import kotlin.math.roundToInt
import kotlin.time.*

@OptIn(ExperimentalTime::class)
internal class MediaSessionNavigator(
    context: Context,
    private val publication: Publication,
    initialLocator: Locator?,
    activityIntent: PendingIntent?,
    mediaPlayerFactory: MediaPlayer.Factory
) : MediaNavigator {

    private val durations: List<Duration?> =
        publication.readingOrder.map { link -> link.duration?.takeIf { it > 0 }?.seconds }

    private val totalDuration: Duration? =
        durations.sum().takeIf { it > 0.seconds }

    private val session = MediaSessionCompat(context, /* log tag */ "${javaClass.simpleName}.mediaSession").apply {
        isActive = true
        if (activityIntent != null) {
            setSessionActivity(activityIntent)
        }
    }

    private val player: MediaPlayer = mediaPlayerFactory.create(context, session, publication, initialLocator).apply {
        prepare(playWhenReady = true)
    }

    private val controller = MediaControllerCompat(context, session).apply {
        registerCallback(MediaControllerCallback())
    }

    private val lastPlaybackState = MutableLiveData<PlaybackStateCompat?>(null)
    private val lastMetadata = MutableLiveData<MediaMetadataCompat?>(null)

    /**
     * Create a [Locator] from the given playback state and media metadata.
     */
    private fun createLocator(playbackState: PlaybackStateCompat?, metadata: MediaMetadataCompat?): Locator? {
        val href = metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) ?: return null
        val index = publication.readingOrder.indexOfFirstWithHref(href) ?: return null
        var locator = publication.readingOrder[index].toLocator()

        if (playbackState != null) {
            val currentPosition = playbackState.position.milliseconds
            val startPosition = durations.slice(0 until index).sum()
            val duration = durations[index]

            locator = locator.copyWithLocations(
                fragments = listOf("t=${currentPosition.inSeconds.roundToInt()}"),
                progression = duration?.let { currentPosition / duration },
                totalProgression = totalDuration?.let { (startPosition + currentPosition) / totalDuration }
            )
        }

        return locator
    }

    // MediaControllerCompat.Callback

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            lastMetadata.postValue(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            lastPlaybackState.postValue(state)
        }

    }


    // Navigator

    override val currentLocator: LiveData<Locator?> =
        combine(lastPlaybackState, lastMetadata, ::createLocator)

    override fun go(locator: Locator, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        TODO("Not yet implemented")
    }


    // MediaNavigator

    override fun play() {
        controller.transportControls.play()
    }

    override fun pause() {
        controller.transportControls.pause()
    }

    override fun playPause() {
        if (controller.playbackState.isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

}
