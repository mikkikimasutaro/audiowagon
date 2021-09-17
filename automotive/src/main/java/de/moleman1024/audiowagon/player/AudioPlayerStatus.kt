/*
SPDX-FileCopyrightText: 2021 MoleMan1024 <moleman1024dev@gmail.com>
SPDX-License-Identifier: GPL-3.0-or-later
*/

package de.moleman1024.audiowagon.player

import android.support.v4.media.session.PlaybackStateCompat

data class AudioPlayerStatus(var playbackState: Int = PlaybackStateCompat.STATE_NONE) {
    var positionInMilliSec: Long = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
    var errorCode: Int = 0
    var errorMsg: String = ""
    var queueItemID: Long = -1
    var isShuffling: Boolean = false
    var isRepeating: Boolean = false

    override fun toString(): String {
        return "AudioPlayerStatus(playbackState=${getStatusName(playbackState)}, queueItemID=$queueItemID, " +
                "positionInMilliSec=$positionInMilliSec, errorCode=$errorCode, errorMsg='$errorMsg')"
    }

    private fun getStatusName(statusCode: Int): String {
        return when (statusCode) {
            PlaybackStateCompat.STATE_NONE -> "NONE"
            PlaybackStateCompat.STATE_STOPPED -> "STOPPED"
            PlaybackStateCompat.STATE_PAUSED -> "PAUSED"
            PlaybackStateCompat.STATE_PLAYING -> "PLAYING"
            PlaybackStateCompat.STATE_FAST_FORWARDING -> "FAST_FORWARDING"
            PlaybackStateCompat.STATE_REWINDING -> "REWINDING"
            PlaybackStateCompat.STATE_BUFFERING -> "BUFFERING"
            PlaybackStateCompat.STATE_ERROR -> "ERROR"
            PlaybackStateCompat.STATE_CONNECTING -> "CONNECTING"
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> "SKIPPING_TO_PREV"
            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> "SKIPPING_TO_NEXT"
            PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM -> "SKIPPING_TO_Q_ITEM"
            else -> {
                statusCode.toString()
            }
        }
    }
}