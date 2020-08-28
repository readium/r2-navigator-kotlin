/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

internal interface MediaPlayer {

    interface Factory {
        fun create(context: Context, mediaSession: MediaSessionCompat, publication: Publication, initialLocator: Locator?): MediaPlayer
    }

    /**
     * Prepares the player to render the publication.
     *
     * If [playWhenReady] is true, will start the playback as soon as possible.
     */
    fun prepare(playWhenReady: Boolean)

}