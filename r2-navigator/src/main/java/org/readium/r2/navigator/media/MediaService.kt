/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.extensions.publicationId
import org.readium.r2.shared.AudioSupport
import org.readium.r2.shared.extensions.splitAt
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationId
import org.readium.r2.shared.publication.toLocator
import org.readium.r2.shared.util.Try
import timber.log.Timber
import kotlin.reflect.KMutableProperty0

@AudioSupport
@OptIn(ExperimentalCoroutinesApi::class)
open class MediaService : MediaBrowserServiceCompat(), MediaPlayer.Listener, CoroutineScope by MainScope() {

    open suspend fun openPublication(publicationId: PublicationId): Try<Publication, Publication.OpeningError> =
        Try.failure(Publication.OpeningError.Unavailable(null))

    /**
     * @param packageName The package name of the application which is requesting access.
     * @param uid The UID of the application which is requesting access.
     */
    open fun isClientAuthorized(packageName: String, uid: Int): Boolean =
        (uid == Process.myUid())

    open val navigatorActivityIntent: PendingIntent? = null

    open fun onCreatePlayer(mediaSession: MediaSessionCompat, media: PendingMedia): MediaPlayer =
        ExoMediaPlayer(this, mediaSession, media)

    open fun onCurrentLocatorChanged(publicationId: PublicationId, locator: Locator) {}

    private var player: MediaPlayer? = null
        set(value) {
            field?.apply {
                onDestroy()
                listener = null
            }

            field = value?.apply {
                listener = this@MediaService
            }
        }

    private val mediaSession: MediaSessionCompat get() = getMediaSession(this, javaClass)

    // MediaPlayer.Listener

    override fun locatorFromMediaId(mediaId: String, extras: Bundle?): Locator? {
        val media = currentMedia.value ?: return null
        val (publicationId, href) = mediaId.splitAt("#")

        if (media.publicationId != publicationId) {
            return null
        }

        val locator = (extras?.getParcelable(EXTRA_LOCATOR) as? Locator)
            ?: href?.let { media.publication.linkWithHref(it)?.toLocator() }

        if (locator != null && href != null && locator.href != href) {
            Timber.e("Ambiguous playback location provided. HREF `$href` doesn't match locator $locator.")
        }

        return locator
    }

    // Service

    override fun onCreate() {
        super.onCreate()

        sessionToken = mediaSession.sessionToken

        mediaSession.run {
            isActive = true
            navigatorActivityIntent?.let(::setSessionActivity)
        }

        launch {
            pendingMedia.receiveAsFlow().collect {
                mediaSession.publicationId = it.publicationId
                player = onCreatePlayer(mediaSession, it)
                currentMedia.value = Media(it.publication, it.publicationId, mediaSession.controller)
            }

//            navigator
//                .flatMapLatest { navigator ->
//                    navigator ?: return@flatMapLatest emptyFlow<Pair<PublicationId, Locator?>>()
//                    navigator.currentLocator.asFlow().map { Pair(navigator.publicationId, it) }
//                }
//                .collect { (publicationId, locator) ->
//                    if (locator != null) {
//                        onCurrentLocatorChanged(publicationId, locator)
//                    }
//                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()

        mediaSession.run {
            isActive = false
            release()
        }

        player?.onDestroy()
    }

    // MediaBrowserServiceCompat

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        if (!isClientAuthorized(packageName = clientPackageName, uid = clientUid)) {
            return null
        }

        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    companion object {

        const val EVENT_PUBLICATION_CHANGED = "org.readium.r2.navigator.EVENT_PUBLICATION_CHANGED"
        const val EXTRA_PUBLICATION_ID = "org.readium.r2.navigator.EXTRA_PUBLICATION_ID"

        private val pendingMedia = Channel<PendingMedia>(Channel.CONFLATED)
        private val currentMedia = MutableStateFlow<Media?>(null)

        @Volatile private var connection: Connection? = null
        @Volatile private var mediaSession: MediaSessionCompat? = null

        fun connect(context: Context, serviceClass: Class<MediaService> = MediaService::class.java): Connection =
            createOnceIn(this::connection, this) {
                Connection(context, serviceClass)
            }

        private fun getMediaSession(context: Context, serviceClass: Class<MediaService>): MediaSessionCompat =
            createOnceIn(this::mediaSession, this) {
                MediaSessionCompat(context, /* log tag */ serviceClass.simpleName)
            }

    }

    class Connection internal constructor(private val context: Context, private val serviceClass: Class<MediaService>) {

        fun preparePlayback(publication: Publication, publicationId: PublicationId, initialLocator: Locator?): Media {
            context.startService(Intent(context, serviceClass))
            pendingMedia.offer(PendingMedia(publication, publicationId, locator = initialLocator ?: publication.readingOrder.first().toLocator()))
            return Media(publication, publicationId, getMediaSession(context, serviceClass).controller)
        }

    }

}

fun <T> createOnceIn(property: KMutableProperty0<T?>, owner: Any, factory: () -> T): T =
    property.get() ?: synchronized(owner) {
        property.get() ?: factory().also {
            property.set(it)
        }
    }

private const val ROOT_ID = "/"
private const val EXTRA_LOCATOR = "locator"
