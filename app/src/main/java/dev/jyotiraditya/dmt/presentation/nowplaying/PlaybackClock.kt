package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import androidx.media3.common.util.UnstableApi
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.sleepOn
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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

/** The time left on the sleep timer, 0 while it waits for the song to end, or null when off. */
@Composable
internal fun sleepLeft(state: DmtState): Long? {
    val left = rememberTimeLeft(state.sleepEndAt)
    return when {
        !state.sleepOn -> null
        left > 0L || state.sleepFinishSong -> left
        else -> null
    }
}

/** The time left until [endAt], in epoch milliseconds, updated as each minute passes. */
@Composable
private fun rememberTimeLeft(endAt: Long): Long {
    val minute = 1.minutes.inWholeMilliseconds
    val left by
        produceState(0L, endAt) {
            while (true) {
                value = endAt - System.currentTimeMillis()
                if (value <= 0L) break
                delay(((value - 1) % minute + 1).milliseconds)
            }
        }
    return left.coerceAtLeast(0L)
}
