package dev.jyotiraditya.dmt.domain.model

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

val SPEED_RANGE = 0.5f..2f

const val SPEED_STEP = 0.05f

val SLEEP_RANGE = 5..180

const val SLEEP_STEP = 5

/** What the player is on: the item, its library track when it has one, and the queue. */
data class NowPlaying(
    /** The player's id for the item, or null when nothing is queued. */
    val mediaId: String? = null,
    val track: Track? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    /** The queue in play order. */
    val queue: List<QueueEntry> = emptyList(),
    /** The playing item's place in [queue], or -1 when it has none. */
    val queuePosition: Int = -1,
    val fault: PlaybackFault? = null,
)

/** Why the item on the player cannot play. */
sealed interface PlaybackFault {
    /** The item holds no audio this device can decode. */
    data object Unsupported : PlaybackFault

    /** Playback failed with the player error named [code]. */
    data class Failed(val code: String) : PlaybackFault
}

/** A queued item; [index] is its place in the player's item list, not the play order. */
data class QueueEntry(val index: Int, val mediaId: String, val title: String, val artist: String)

/** The sleep timer; [endAt] is in epoch milliseconds and 0 while it is off. */
data class SleepState(val endAt: Long = 0L, val finishSong: Boolean = false) {
    val on: Boolean
        get() = endAt > 0L
}

/** [speed] snapped to a [SPEED_STEP] and moved [steps] steps, within [SPEED_RANGE]. */
fun steppedSpeed(speed: Float, steps: Int = 0): Float {
    // Counts in hundredths so repeated steps don't drift.
    val hundredths = ((speed / SPEED_STEP).roundToInt() + steps) * (SPEED_STEP * 100).roundToInt()
    return (hundredths / 100f).coerceIn(SPEED_RANGE)
}

fun steppedSleepMinutes(minutes: Int, steps: Int): Int =
    (minutes + steps * SLEEP_STEP).coerceIn(SLEEP_RANGE)

/** These milliseconds in whole minutes, rounded up. */
fun Long.ceilMinutes(): Int = ceil(milliseconds / 1.minutes).toInt()
