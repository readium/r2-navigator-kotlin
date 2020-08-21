/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.app.PendingIntent
import android.content.Context
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.readium.r2.navigator.MediaNavigator
import org.readium.r2.navigator.extensions.combine
import org.readium.r2.navigator.extensions.elapsedPosition
import org.readium.r2.navigator.extensions.isPlaying
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.shared.publication.*
import kotlin.math.roundToInt
import kotlin.time.*

/**
 * Rate at which the current locator is broadcasted during playback.
 */
private const val playbackPositionRefreshRate: Double = 2.0  // Hz

@OptIn(ExperimentalTime::class)
internal class MediaSessionNavigator(
    context: Context,
    private val publication: Publication,
    initialLocator: Locator?,
    activityIntent: PendingIntent?,
    mediaPlayerFactory: MediaPlayer.Factory
) : MediaNavigator {

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Duration of each reading order resource.
     */
    private val durations: List<Duration?> =
        publication.readingOrder.map { link -> link.duration?.takeIf { it > 0 }?.seconds }

    /**
     * Total duration of the publication.
     */
    private val totalDuration: Duration? =
        durations.sum().takeIf { it > 0.seconds }

    private val session = MediaSessionCompat(context, /* log tag */ "${javaClass.simpleName}.mediaSession").apply {
        if (activityIntent != null) {
            setSessionActivity(activityIntent)
        }
    }

    private val player: MediaPlayer = mediaPlayerFactory.create(context, session, publication, initialLocator)

    private val controller = MediaControllerCompat(context, session).apply {
        registerCallback(MediaControllerCallback())
    }

    private val mediaMetadata = MutableLiveData<MediaMetadataCompat?>(null)
    private val playbackPosition = MutableLiveData<Duration?>(null)

    init {
        session.isActive
        player.prepare(playWhenReady = true)
    }

    /**
     * Observes recursively the playback position, as long as it is playing.
     */
    private fun updatePlaybackPosition() {
        val state = controller.playbackState
        playbackPosition.postValue(state.elapsedPosition.milliseconds)

        if (state.state == PlaybackStateCompat.STATE_PLAYING) {
            val delay = (1.0 / playbackPositionRefreshRate).seconds
            handler.postDelayed(::updatePlaybackPosition,  delay.toLongMilliseconds())
        }
    }

    // MediaControllerCompat.Callback

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mediaMetadata.postValue(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state?.state == PlaybackState.STATE_PLAYING) {
                updatePlaybackPosition()
            }
        }

    }


    // Navigator

    override val currentLocator: LiveData<Locator?> =
        combine(mediaMetadata, playbackPosition, ::createLocator)

    /**
     * Creates a [Locator] from the given media [metadata] and playback [position].
     */
    private fun createLocator(metadata: MediaMetadataCompat?, position: Duration?): Locator? {
        val href = metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) ?: return null
        val index = publication.readingOrder.indexOfFirstWithHref(href) ?: return null
        var locator = publication.readingOrder[index].toLocator()

        if (position != null) {
            val startPosition = durations.slice(0 until index).sum()
            val duration = durations[index]

            locator = locator.copyWithLocations(
                fragments = listOf("t=${position.inSeconds.roundToInt()}"),
                progression = duration?.let { position / duration },
                totalProgression = totalDuration?.let { (startPosition + position) / totalDuration }
            )
        }

        return locator
    }

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
