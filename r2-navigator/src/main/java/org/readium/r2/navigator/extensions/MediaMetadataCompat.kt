/*
 * Copyright 2020 Readium Foundation. All rights reserved2
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.extensions

import android.support.v4.media.MediaMetadataCompat

internal val MediaMetadataCompat.id: String? get() =
    getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)

internal val MediaMetadataCompat.publicationId: String? get() =
    id?.substringBefore("#")

internal val MediaMetadataCompat.resourceHref: String? get() =
    id?.substringAfter("#", missingDelimiterValue = "")
        ?.takeIf { it.isNotEmpty() }
