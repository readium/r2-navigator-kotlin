/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(ExperimentalTime::class)

package org.readium.r2.navigator.media

import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaControllerCompat.TransportControls
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.MediaNavigator
import org.readium.r2.navigator.extensions.*
import org.readium.r2.navigator.extensions.sum
import org.readium.r2.shared.publication.*
import kotlin.math.roundToInt
import kotlin.time.*

/**
 * Rate at which the current locator is broadcasted during playback.
 */
private const val playbackPositionRefreshRate: Double = 2.0  // Hz

private val skipForwardInterval: Duration = 10.seconds
private val skipBackwardInterval: Duration = 10.seconds

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class MediaSessionNavigator(
    private val mediaSession: MediaSessionCompat,
    override val publication: Publication
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

    private val mediaMetadata = MutableStateFlow<MediaMetadataCompat?>(null)
    private val playbackState = MutableStateFlow<PlaybackStateCompat?>(null)
    private val playbackPosition = MutableStateFlow(0.seconds)

    private val transportControls: TransportControls get() =
        mediaSession.controller.transportControls

    init {
        mediaSession.isActive
        mediaSession.controller.registerCallback(MediaControllerCallback())
    }

    /**
     * Observes recursively the playback position, as long as it is playing.
     */
    private fun updatePlaybackPosition() {
        val state = mediaSession.controller.playbackState
        val newPosition = state.elapsedPosition.milliseconds
        if (playbackPosition.value != newPosition) {
            playbackPosition.value = newPosition
        }

        if (state.state == PlaybackStateCompat.STATE_PLAYING) {
            val delay = (1.0 / playbackPositionRefreshRate).seconds
            handler.postDelayed(::updatePlaybackPosition,  delay.toLongMilliseconds())
        }
    }

    // MediaControllerCompat.Callback

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mediaMetadata.value = metadata
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.value = state
            if (state?.state == PlaybackState.STATE_PLAYING) {
                updatePlaybackPosition()
            }
        }

    }


    // Navigator

    override val currentLocator: LiveData<Locator?> =
        playbackPosition.combine(mediaMetadata, ::createLocator).asLiveData()

    /**
     * Creates a [Locator] from the given media [metadata] and playback [position].
     */
    @Suppress("RedundantSuspendModifier")
    private suspend fun createLocator(position: Duration?, metadata: MediaMetadataCompat?): Locator? {
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
        transportControls.playFromUri(Uri.parse(locator.href), Bundle().apply {
            putParcelable("locator", locator)
        })
        completion()
        return true
    }

    override fun go(link: Link, animated: Boolean, completion: () -> Unit): Boolean =
        go(link.toLocator(), animated, completion)

    override fun goForward(animated: Boolean, completion: () -> Unit): Boolean {
        seekBy(skipForwardInterval)
        completion()
        return true
    }

    override fun goBackward(animated: Boolean, completion: () -> Unit): Boolean {
        seekBy(-skipBackwardInterval)
        completion()
        return true
    }


    // MediaNavigator

    override val playback: Flow<MediaPlayback> =
        combine(mediaMetadata, playbackState, playbackPosition) { metadata, state, position ->
            val index = metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                ?.let { publication.readingOrder.indexOfFirstWithHref(it) }

            val duration = index?.let { durations[index] }

            MediaPlayback(
                state = state?.toPlaybackState() ?: MediaPlayback.State.Idle,
                timeline = MediaTimeline(
                    position = position.coerceAtMost(duration ?: position),
                    duration = duration,
                    // Buffering is not yet supported, but will be with media2:
                    // https://developer.android.com/reference/androidx/media2/common/SessionPlayer#getBufferedPosition()
                    buffered = null
                )
            )
        }
        .distinctUntilChanged()
        .conflate()

    override fun play() {
        transportControls.play()
    }

    override fun pause() {
        transportControls.pause()
    }

    override fun playPause() {
        if (mediaSession.controller.playbackState.isPlaying) {
            transportControls.pause()
        } else {
            transportControls.play()
        }
    }

    override fun stop() {
        transportControls.stop()
    }

    override fun seekTo(position: Duration) {
        @Suppress("NAME_SHADOWING")
        val position = position.coerceAtLeast(0.seconds)

        // We overwrite the current position to allow skipping successively several time without
        // having to wait for the playback position to actually update.
        playbackPosition.value = position

        transportControls.seekTo(position.toLongMilliseconds())
    }

    override fun seekBy(interval: Duration) {
        seekTo(playbackPosition.value + interval)
    }

}