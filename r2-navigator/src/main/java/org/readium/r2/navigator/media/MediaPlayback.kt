/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator.media

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * State of the playback at a point in time.
 *
 * @param state State of the playback.
 * @param timeline Position and duration of the current resource.
 */
@OptIn(ExperimentalTime::class)
data class MediaPlayback(val state: State, val timeline: Timeline) {

    enum class State { Idle, Loading, Playing, Paused }

    data class Timeline(
        val position: Duration,
        val duration: Duration?,
        val buffered: Duration?
    )

    val isPlaying: Boolean get() =
        (state == State.Playing || state == State.Loading)

}
