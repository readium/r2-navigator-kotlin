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
import org.readium.r2.shared.AudioSupport
import org.readium.r2.shared.publication.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@AudioSupport
@OptIn(ExperimentalTime::class)
internal class ExoMediaPlayer(
    context: Context,
    mediaSession: MediaSessionCompat,
    media: PendingMedia
) : MediaPlayer {

    override var listener: MediaPlayer.Listener? = null

    private val publication: Publication = media.publication

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

        prepareTracklist()
        seekTo(media.locator)
    }

    override fun onDestroy() {
        player.stop(true)
        player.release()
    }

    private fun prepareTracklist() {
        val dataSourceFactory = PublicationDataSource.Factory(publication)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        val trackSources = publication.readingOrder.map { link ->
            val uri = Uri.parse(link.href)
            mediaSourceFactory.createMediaSource(uri)
        }
        val tracklistSource = ConcatenatingMediaSource(*trackSources.toTypedArray())

        player.prepare(tracklistSource)
    }

    private fun seekTo(locator: Locator) {
        val readingOrder = publication.readingOrder
        val index = readingOrder.indexOfFirstWithHref(locator.href) ?: 0

        val duration = readingOrder[index].duration?.seconds
        val time = locator.locations.timeWithDuration(duration)
        player.seekTo(index, time?.toLongMilliseconds() ?: 0)
    }

    private inner class PlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        // We don't support any custom commands for now.
        override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean = false

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID

        override fun onPrepare(playWhenReady: Boolean) {}

        override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
            val locator = listener?.locatorFromMediaId(mediaId, extras) ?: return

            player.stop()
            player.playWhenReady = playWhenReady

        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {}

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {}

    }

    private inner class QueueNavigator(mediaSession: MediaSessionCompat) : TimelineQueueNavigator(mediaSession) {

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            createMediaMetadata(publication.readingOrder[windowIndex]).description

    }

    private fun createMediaMetadata(link: Link) = MediaMetadataCompat.Builder().apply {
        putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, link.href)
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, link.title)
    }.build()

}
