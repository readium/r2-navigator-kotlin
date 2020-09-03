/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationId

interface MediaPlayer {

    interface Listener {

        fun locatorFromMediaId(mediaId: String, extras: Bundle?): Locator?

        fun onNotificationPosted(notificationId: Int, notification: Notification)
        fun onNotificationCancelled(notificationId: Int)

    }

    var listener: Listener?

    fun onDestroy()

}