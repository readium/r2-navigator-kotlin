/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import android.support.v4.media.session.MediaControllerCompat
import org.readium.r2.navigator.extensions.publicationId
import org.readium.r2.shared.AudioSupport
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationId

@AudioSupport
data class Media(
    val publication: Publication,
    val publicationId: PublicationId,
    val controller: MediaControllerCompat
) {

    /**
     * Indicates whether the media session is loaded with a resource from this [publication]. This
     * is necessary because a single media session could be used to play multiple publications.
     */
    val isActive: Boolean get() =
        controller.publicationId == publicationId

}
