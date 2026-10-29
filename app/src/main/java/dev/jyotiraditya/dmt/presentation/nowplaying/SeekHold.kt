package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.ProgressStateWithTickCount
import androidx.media3.ui.compose.state.rememberProgressStateWithTickCount
import dev.jyotiraditya.dmt.util.lingering
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.Duration.Companion.milliseconds

/** How long the hold-to-seek readout stays after the last step. */
private const val STEP_LINGER_MS = 700L

/** The steps of a hold-to-seek. */
@Stable
internal class SeekSteps {
    private val steps =
        MutableSharedFlow<Long>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /** The step in progress, kept for [STEP_LINGER_MS] after the hold ends. */
    val stepMs: Flow<Long?> = steps.lingering(STEP_LINGER_MS.milliseconds)

    fun onStep(stepMs: Long) {
        steps.tryEmit(stepMs)
    }
}

@Composable internal fun rememberSeekSteps(): SeekSteps = remember { SeekSteps() }

/** A seek bar's position: the dragged one while scrubbing, the player's otherwise. */
@OptIn(UnstableApi::class)
@Stable
internal class SeekHold(private val progress: ProgressStateWithTickCount) {
    var scrub by mutableStateOf<Float?>(null)
        private set

    val scrubbing: Boolean
        get() = scrub != null

    val fraction: Float
        get() = scrub ?: progress.currentPositionProgress

    /** [positionMs], or the time the drag would seek to while scrubbing. */
    fun shownMs(positionMs: Long): Long = scrub?.let(progress::progressToPosition) ?: positionMs

    fun onScrub(fraction: Float?) {
        scrub = fraction
    }

    fun onSeek(fraction: Float) {
        progress.updateCurrentPositionProgress(fraction)
        scrub = null
    }
}

/** A [SeekHold] for a bar [width] pixels wide, moving one step per pixel. */
@OptIn(UnstableApi::class)
@Composable
internal fun rememberSeekHold(player: Player, width: Int): SeekHold {
    val progress = rememberProgressStateWithTickCount(player, totalTickCount = width)
    return remember(progress) { SeekHold(progress) }
}
