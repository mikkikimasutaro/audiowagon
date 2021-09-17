/*
SPDX-FileCopyrightText: 2021 MoleMan1024 <moleman1024dev@gmail.com>
SPDX-License-Identifier: GPL-3.0-or-later
*/

package de.moleman1024.audiowagon.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.moleman1024.audiowagon.log.Logger
import de.moleman1024.audiowagon.player.AudioPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "NotificationReceiver"
private val logger = Logger
const val ACTION_PLAY = "de.moleman1024.audiowagon.ACTION_PLAY"
const val ACTION_PAUSE = "de.moleman1024.audiowagon.ACTION_PAUSE"
const val ACTION_NEXT = "de.moleman1024.audiowagon.ACTION_NEXT"
const val ACTION_PREV = "de.moleman1024.audiowagon.ACTION_PREV"

class NotificationReceiver(
    private val audioPlayer: AudioPlayer,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) :
    BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }
        logger.debug(TAG, "Received notification: $intent")
        when (intent.action) {
            ACTION_PLAY -> scope.launch(dispatcher) { callSafely { audioPlayer.start() } }
            ACTION_PAUSE -> scope.launch(dispatcher) { callSafely { audioPlayer.pause() } }
            ACTION_NEXT -> scope.launch(dispatcher) { callSafely { audioPlayer.skipNextTrack() } }
            ACTION_PREV -> scope.launch(dispatcher) { callSafely { audioPlayer.skipPreviousTrack() } }
            else -> {
                logger.warning(TAG, "Ignoring unhandled action: ${intent.action}")
            }
        }
    }

    private inline fun <T> callSafely(call: () -> T) {
        try {
            call()
        } catch (exc: Exception) {
            logger.exception(TAG, exc.message.toString(), exc)
        }
    }

}