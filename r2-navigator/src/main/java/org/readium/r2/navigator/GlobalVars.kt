/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator

import org.readium.r2.shared.Publication

/**
 * Created by aferditamuriqi on 10/3/17.
 */


/**
 * Global Parameters
 */
const val BASE_URL = "http://127.0.0.1"

class PublicationData(
        val path: String,
        val publication: Publication,
        val fileName: String?,
        val bookId: Long? = null,
        val cover: ByteArray? = null
)

var CURRENT_PUB: PublicationData? = null
