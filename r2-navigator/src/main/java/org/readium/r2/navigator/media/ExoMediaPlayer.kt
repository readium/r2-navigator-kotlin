/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import org.readium.r2.navigator.audio.PublicationDataSource
import org.readium.r2.navigator.extensions.timeWithDuration
import org.readium.r2.shared.publication.*
import timber.log.Timber
import java.lang.IllegalArgumentException
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private const val EXTRA_LOCATOR = "locator"

@OptIn(ExperimentalTime::class)
internal class ExoMediaPlayer(context: Context, mediaSession: MediaSessionCompat, private val publication: Publication, private val initialLocator: Locator?) : MediaPlayer {

    class Factory: MediaPlayer.Factory {

        override fun create(context: Context, mediaSession: MediaSessionCompat, publication: Publication, initialLocator: Locator?): MediaPlayer =
            ExoMediaPlayer(context, mediaSession, publication, initialLocator)

    }

    private val player: ExoPlayer = SimpleExoPlayer.Builder(context).build().apply {
        audioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        setHandleAudioBecomingNoisy(true)
//        addAnalyticsListener(EventLogger(null))
    }

    private val mediaSessionConnector = MediaSessionConnector(mediaSession)

    init {
        mediaSessionConnector.apply {
            setPlaybackPreparer(PlaybackPreparer())
            setQueueNavigator(QueueNavigator(mediaSession))
            setPlayer(player)
        }
    }

    private fun seekTo(locator: Locator): Boolean {
        val index = publication.readingOrder.indexOfFirstWithHref(locator.href)
            ?: return false

        val duration = publication.readingOrder[index].duration?.seconds
        val time = locator.locations.timeWithDuration(duration)
        player.seekTo(index, time?.toLongMilliseconds() ?: 0)

        return true
    }

    // MediaPlayer

    private var isPrepared = false

    override fun prepare(playWhenReady: Boolean) {
        check(!isPrepared) { "This player is already prepared." }
        isPrepared = true

        val dataSourceFactory = PublicationDataSource.Factory(publication)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        val trackSources = publication.readingOrder.map { link ->
            val uri = Uri.parse(link.href)
            mediaSourceFactory.createMediaSource(uri)
        }
        val tracklistSource = ConcatenatingMediaSource(*trackSources.toTypedArray())

        player.stop(true)
        player.playWhenReady = playWhenReady
        player.prepare(tracklistSource)

        initialLocator?.let { seekTo(it) }
    }

    private inner class PlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        // We don't support any custom commands yet.
        override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean = false

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
            PlaybackStateCompat.ACTION_PLAY_FROM_URI or
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
            PlaybackStateCompat.ACTION_PREPARE_FROM_URI

        override fun onPrepare(playWhenReady: Boolean) {}

        override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) =
            onPrepareFromHref(href = mediaId, locator = extras?.getParcelable(EXTRA_LOCATOR), playWhenReady = playWhenReady)

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {}

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) =
            onPrepareFromHref(href = uri.toString(), locator = extras?.getParcelable(EXTRA_LOCATOR), playWhenReady = playWhenReady)

        private fun onPrepareFromHref(href: String, locator: Locator?, playWhenReady: Boolean) {
            if (locator?.href != null && locator.href != href) {
                throw IllegalArgumentException("Ambiguous playback location provided. HREF `$href` doesn't match locator $locator.")
            }

            @Suppress("NAME_SHADOWING")
            val locator = locator
                ?: publication.linkWithHref(href)?.toLocator()
                ?: return

            player.playWhenReady = playWhenReady
            seekTo(locator)
        }

    }

    private inner class QueueNavigator(mediaSession: MediaSessionCompat) : TimelineQueueNavigator(mediaSession) {

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return createMediaMetadata(publication.readingOrder[windowIndex]).description
        }

    }

    private fun createMediaMetadata(link: Link) = MediaMetadataCompat.Builder().apply {
        putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, link.href)
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, link.title)
    }.build()

}
