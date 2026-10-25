package dev.jyotiraditya.dmt.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val FADE = 10.seconds

private const val FADE_STEPS = 50

/** The volume at [progress] through the fade, falling faster at the start. */
internal fun fadeLevel(progress: Float): Float {
    val left = 1f - progress.coerceIn(0f, 1f)
    return left * left
}

/** Pauses [player] at a set time, or at the end of the song then playing, fading out first. */
@OptIn(UnstableApi::class)
internal class SleepTimer(
    private val scope: CoroutineScope,
    private val player: ExoPlayer,
    private val onLevel: (Float) -> Unit,
) {
    private var job: Job? = null
    private var fadeJob: Job? = null

    /** When the timer ends, in epoch milliseconds; null when it is off. */
    var endAt: Long? = null
        private set

    var finishSong: Boolean = false
        private set

    /** Once the time is up, refades on every seek or song change and stops at the song's end. */
    private val songEnd =
        object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) cancel()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (
                    events.containsAny(
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                    )
                ) {
                    scheduleSongFade()
                }
            }
        }

    /** Starts the timer for [endAt], or stops it when [endAt] is 0. */
    fun set(endAt: Long, finishSong: Boolean) {
        cancel()
        if (endAt <= 0L) return
        this.endAt = endAt
        this.finishSong = finishSong
        job = scope.launch {
            if (finishSong) {
                delay(untilEnd())
                player.pauseAtEndOfMediaItems = true
                player.addListener(songEnd)
                scheduleSongFade()
            } else {
                delay(untilEnd() - FADE)
                fadeOut(untilEnd().coerceIn(Duration.ZERO, FADE))
                player.pause()
                cancel()
            }
        }
    }

    fun cancel() {
        job?.cancel()
        fadeJob?.cancel()
        player.removeListener(songEnd)
        player.pauseAtEndOfMediaItems = false
        endAt = null
        finishSong = false
        onLevel(1f)
    }

    /** Restores the volume and fades out again over the last seconds of the song. */
    private fun scheduleSongFade() {
        fadeJob?.cancel()
        onLevel(1f)
        if (endAt == null || !player.isPlaying) return
        val left = songLeft() ?: return
        fadeJob = scope.launch {
            delay(left - FADE)
            fadeOut(left.coerceAtMost(FADE))
        }
    }

    private suspend fun fadeOut(length: Duration) {
        val step = length / FADE_STEPS
        for (i in 1..FADE_STEPS) {
            delay(step)
            onLevel(fadeLevel(i.toFloat() / FADE_STEPS))
        }
    }

    private fun untilEnd(): Duration = ((endAt ?: 0L) - System.currentTimeMillis()).milliseconds

    /** The time left in the playing song at the current speed; null while its length is unknown. */
    private fun songLeft(): Duration? {
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: return null
        val left = (duration - player.currentPosition).coerceAtLeast(0L)
        return (left / player.playbackParameters.speed).toLong().milliseconds
    }
}
