package dev.jyotiraditya.dmt.presentation.player

import android.os.Bundle
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.jyotiraditya.dmt.core.base.StateStore
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.playback.PlaybackCommands
import dev.jyotiraditya.dmt.util.suspendRunCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal val SPEED_RANGE = 0.5f..2f

internal const val SPEED_STEP = 0.05f

internal val SLEEP_RANGE = 5..180

internal const val SLEEP_STEP = 5

/** [speed] snapped to a [SPEED_STEP] and moved [steps] steps, within [SPEED_RANGE]. */
internal fun steppedSpeed(speed: Float, steps: Int = 0): Float {
    // Counts in hundredths so repeated steps don't drift.
    val hundredths = ((speed / SPEED_STEP).roundToInt() + steps) * (SPEED_STEP * 100).roundToInt()
    return (hundredths / 100f).coerceIn(SPEED_RANGE)
}

internal fun steppedSleepMinutes(minutes: Int, steps: Int): Int =
    (minutes + steps * SLEEP_STEP).coerceIn(SLEEP_RANGE)

/** These milliseconds in whole minutes, rounded up. */
internal fun Long.ceilMinutes(): Int = ceil(milliseconds / 1.minutes).toInt()

/** The sleep timer and playback speed, shown through [store] and kept by the service. */
internal class SleepAndSpeed(
    private val preferences: PreferencesRepository,
    private val scope: CoroutineScope,
    private val store: StateStore<DmtState>,
) {

    /** Reads back the service's sleep timer and applies the saved speed to [controller]. */
    suspend fun restore(controller: MediaController) {
        controller.addListener(
            object : Player.Listener {
                // The service pauses playback when the timer ends.
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (!playWhenReady && store.current.sleepOn) scope.launch { sync(controller) }
                }
            },
        )
        sync(controller)
        val keepPitch = preferences.settings.first().keepPitch
        controller.playbackParameters = parameters(preferences.savedSpeed(), keepPitch)
    }

    fun setSpeed(controller: MediaController, speed: Float) {
        val stepped = steppedSpeed(speed)
        if (stepped == steppedSpeed(controller.playbackParameters.speed)) return
        controller.playbackParameters = parameters(stepped, store.current.settings.keepPitch)
        scope.launch { preferences.saveSpeed(stepped) }
    }

    fun setSleep(controller: MediaController, minutes: Int) {
        val endAt =
            if (minutes == 0) {
                0L
            } else {
                System.currentTimeMillis() + minutes.minutes.inWholeMilliseconds
            }
        send(controller, endAt, store.current.settings.sleepFinishSong)
    }

    fun sleepAtSongEnd(controller: MediaController) {
        send(controller, System.currentTimeMillis(), finishSong = true)
    }

    /** Applies a changed pitch or finish-the-song setting to what is running. */
    fun onSettings(controller: MediaController, old: DmtSettings, settings: DmtSettings) {
        if (old.keepPitch != settings.keepPitch) {
            controller.playbackParameters =
                parameters(controller.playbackParameters.speed, settings.keepPitch)
        }
        val endAt = store.current.sleepEndAt
        if (old.sleepFinishSong != settings.sleepFinishSong && endAt > System.currentTimeMillis()) {
            send(controller, endAt, settings.sleepFinishSong)
        }
    }

    /** Reads the sleep timer from the service into [store]. */
    suspend fun sync(controller: MediaController) {
        suspendRunCatching {
            val extras =
                controller
                    .sendCustomCommand(PlaybackCommands.SLEEP_GET, Bundle.EMPTY)
                    .await()
                    .extras
            store.reduce {
                it.copy(
                    sleepEndAt = extras.getLong(PlaybackCommands.KEY_END_AT),
                    sleepFinishSong = extras.getBoolean(PlaybackCommands.KEY_FINISH_SONG),
                )
            }
        }
    }

    private fun send(controller: MediaController, endAt: Long, finishSong: Boolean) {
        controller.sendCustomCommand(
            PlaybackCommands.SLEEP_SET,
            Bundle().apply {
                putLong(PlaybackCommands.KEY_END_AT, endAt)
                putBoolean(PlaybackCommands.KEY_FINISH_SONG, finishSong)
            },
        )
        store.reduce { it.copy(sleepEndAt = endAt, sleepFinishSong = finishSong) }
    }

    private fun parameters(speed: Float, keepPitch: Boolean) =
        PlaybackParameters(speed, if (keepPitch) 1f else speed)
}
