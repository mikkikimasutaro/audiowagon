/*
SPDX-FileCopyrightText: 2021 MoleMan1024 <moleman1024dev@gmail.com>
SPDX-License-Identifier: GPL-3.0-or-later
*/

package de.moleman1024.audiowagon.player

enum class AudioSessionChangeType {
    ON_PLAY,
    ON_PLAY_FROM_MEDIA_ID,
    ON_PLAY_FROM_SEARCH,
    ON_SKIP_TO_QUEUE_ITEM,
    ON_STOP,
    ON_PAUSE,
    ON_ENABLE_LOG_TO_USB,
    ON_DISABLE_LOG_TO_USB,
    ON_ENABLE_EQUALIZER,
    ON_DISABLE_EQUALIZER,
    ON_SET_EQUALIZER_PRESET,
    ON_EJECT
}