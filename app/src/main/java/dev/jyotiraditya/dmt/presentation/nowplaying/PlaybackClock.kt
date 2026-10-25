package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.media3.common.Player
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.sleepOn
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Backward corrections smaller than this are ignored to avoid visible jitter. */
private const val JITTER_MS = 300L

/**
 * [player]'s position, read every frame while [moving] and on each player event otherwise; [leadMs]
 * moves it ahead to cover display latency.
 */
@Composable
internal fun rememberPlaybackPosition(
    player: Player,
    moving: Boolean,
    leadMs: Long = 0L,
): State<Long> = produceState(player.currentPosition + leadMs, player, moving, leadMs) {
    val listener =
        object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                value = player.currentPosition + leadMs
            }
        }
    player.addListener(listener)
    try {
        while (moving) {
            withFrameMillis {
                val read = player.currentPosition + leadMs
                if (read !in value - JITTER_MS..<value) value = read
            }
        }
        awaitCancellation()
    } finally {
        player.removeListener(listener)
    }
}

/** Whether [player]'s audio is advancing right now; false while buffering or paused. */
@Composable
internal fun rememberIsPlaying(player: Player): Boolean {
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
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
        produceState(endAt - System.currentTimeMillis(), endAt) {
            while (value > 0L) {
                delay(((value - 1) % minute + 1).milliseconds)
                value = endAt - System.currentTimeMillis()
            }
        }
    return left.coerceAtLeast(0L)
}
