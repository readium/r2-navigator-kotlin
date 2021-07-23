package org.readium.r2.navigator.extensions

import android.util.Size
import org.readium.r2.navigator.util.BitmapFactory
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.mapCatching

suspend fun Resource.readAsBitmap(maxSize: Size) =
    read().mapCatching {
        BitmapFactory.decodeByteArrayFitting(it, maxSize)
    }
