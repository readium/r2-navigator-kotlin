/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.asFlow
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.readium.r2.navigator.extensions.let
import org.readium.r2.navigator.media.extensions.publicationId
import org.readium.r2.shared.AudioSupport
import org.readium.r2.shared.extensions.splitAt
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.cover
import timber.log.Timber
import kotlin.reflect.KMutableProperty0

@AudioSupport
@OptIn(ExperimentalCoroutinesApi::class)
open class MediaService : MediaBrowserServiceCompat(), MediaPlayer.Listener, CoroutineScope by MainScope() {

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

    protected val mediaSession: MediaSessionCompat get() = getMediaSession(this, javaClass)

    // MediaPlayer.Listener

    override fun locatorFromMediaId(mediaId: String, extras: Bundle?): Locator? {
        val navigator = navigator.value ?: return null
        val (publicationId, href) = mediaId.splitAt("#")

        if (navigator.publicationId != publicationId) {
            return null
        }

        val locator = (extras?.getParcelable(EXTRA_LOCATOR) as? Locator)
            ?: href?.let { navigator.publication.linkWithHref(it)?.toLocator() }

        if (locator != null && href != null && locator.href != href) {
            Timber.e("Ambiguous playback location provided. HREF `$href` doesn't match locator $locator.")
        }

        return locator
    }

    override suspend fun coverOfPublication(publication: Publication, publicationId: PublicationId): Bitmap? =
        publication.cover()

    private var notificationId: Int? = null
    private var notification: Notification? = null

    override fun onNotificationPosted(notificationId: Int, notification: Notification) {
        this.notificationId = notificationId
        this.notification = notification
        startForeground(notificationId, notification)
    }

    override fun onNotificationCancelled(notificationId: Int) {
        this.notificationId = null
        this.notification = null
        stopForeground(true)
    }

    // We don't support any custom commands by default.
    override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?): Boolean = false

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
                player = onCreatePlayer(mediaSession, it)
                mediaSession.publicationId = it.publicationId
            }
        }

        launch {
            navigator
                .flatMapLatest { navigator ->
                    navigator ?: return@flatMapLatest emptyFlow<Pair<PublicationId, Locator?>>()
                    navigator.currentLocator.asFlow().map { Pair(navigator.publicationId, it) }
                }
                .collect { (publicationId, locator) ->
                    if (locator != null) {
                        onCurrentLocatorChanged(publicationId, locator)
                    }
                }
        }

        launch {
            navigator
                .flatMapLatest { navigator ->
                    navigator?.playback?.map { it.state }
                        ?: flowOf(MediaPlayback.State.Idle)
                }
                .collect {
                    if (it.isPlaying) {
                        let(notificationId, notification) { id, note ->
                            startForeground(id, note)
                        }
                    } else {
                        stopForeground(false)
                    }
                }
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
        navigator.value?.stop()
        navigator.value = null
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

        @Volatile private var connection: Connection? = null

        private val pendingMedia = Channel<PendingMedia>(Channel.CONFLATED)
        private val navigator = MutableStateFlow<MediaSessionNavigator?>(null)
        @Volatile private var mediaSession: MediaSessionCompat? = null

        fun connect(context: Context, serviceClass: Class<*> = MediaService::class.java): Connection =
            createOnceIn(this::connection, this) {
                Connection(context, serviceClass)
            }

        private fun getMediaSession(context: Context, serviceClass: Class<*>): MediaSessionCompat =
            createOnceIn(this::mediaSession, this) {
                MediaSessionCompat(context, /* log tag */ serviceClass.simpleName)
            }

    }

    class Connection internal constructor(private val context: Context, private val serviceClass: Class<*>) {

        val currentNavigator: StateFlow<MediaSessionNavigator?> get() = navigator

        fun getNavigator(publication: Publication, publicationId: PublicationId, initialLocator: Locator?): MediaSessionNavigator {
            context.startService(Intent(context, serviceClass))

            navigator.value
                ?.takeIf { it.publicationId == publicationId }
                ?.let { return it }

            pendingMedia.offer(PendingMedia(publication, publicationId, locator = initialLocator ?: publication.readingOrder.first().toLocator()))

            return MediaSessionNavigator(publication, publicationId, getMediaSession(context, serviceClass).controller)
                .also {
                    navigator.value?.stop()
                    navigator.value = it
                }
        }

    }

}

// FIXME: Move to r2-shared
internal fun <T> createOnceIn(property: KMutableProperty0<T?>, owner: Any, factory: () -> T): T =
    property.get() ?: synchronized(owner) {
        property.get() ?: factory().also {
            property.set(it)
        }
    }

private const val ROOT_ID = "/"
private const val EXTRA_LOCATOR = "locator"
