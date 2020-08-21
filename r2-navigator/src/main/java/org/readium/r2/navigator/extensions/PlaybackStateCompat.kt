/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */
package org.readium.r2.navigator.extensions

import android.support.v4.media.session.PlaybackStateCompat

inline val PlaybackStateCompat.isPrepared get() =
    (state == PlaybackStateCompat.STATE_BUFFERING) ||
    (state == PlaybackStateCompat.STATE_PLAYING) ||
    (state == PlaybackStateCompat.STATE_PAUSED)

inline val PlaybackStateCompat.isPlaying get() =
    (state == PlaybackStateCompat.STATE_BUFFERING) ||
    (state == PlaybackStateCompat.STATE_PLAYING)

inline val PlaybackStateCompat.canPlay get() =
    (actions and PlaybackStateCompat.ACTION_PLAY != 0L) ||
    ((actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) && (state == PlaybackStateCompat.STATE_PAUSED))
