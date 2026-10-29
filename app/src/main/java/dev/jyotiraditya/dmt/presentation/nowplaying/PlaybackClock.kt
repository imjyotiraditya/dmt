package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import androidx.media3.common.util.UnstableApi
import dev.jyotiraditya.dmt.core.common.rememberMinuteClock
import dev.jyotiraditya.dmt.domain.model.SleepState
import kotlin.time.Clock

/** Whether [player]'s audio is advancing right now; false while buffering or paused. */
@OptIn(UnstableApi::class)
@Composable
internal fun rememberIsPlaying(player: Player): Boolean {
    val playing by
        produceState(player.isPlaying, player) {
            player.listenTo(Player.EVENT_IS_PLAYING_CHANGED) { value = isPlaying }
        }
    return playing
}

/** The time left on [sleep], 0 while it waits for the song to end, or null when it is off. */
@Composable
internal fun sleepLeft(sleep: SleepState): Long? {
    val tick by rememberMinuteClock()
    val endAt = sleep.endAt
    val left =
        remember(tick, endAt) {
            (endAt - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0L)
        }
    return when {
        !sleep.on -> null
        left > 0L || sleep.finishSong -> left
        else -> null
    }
}
