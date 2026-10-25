package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.jyotiraditya.dmt.presentation.nowplaying.PlayPauseTap
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberSeekHold
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.components.SeekBar
import dev.jyotiraditya.dmt.ui.theme.Night
import dev.jyotiraditya.dmt.ui.theme.NightInk
import dev.jyotiraditya.dmt.ui.theme.Numerals
import dev.jyotiraditya.dmt.ui.theme.secondary
import dev.jyotiraditya.dmt.util.asTime
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest

/** How long the controls stay after the last touch while music plays. */
private const val CONTROLS_HOLD_MS = 3000L

/** Opacity of the ink tint that raises the controls off the lyrics background. */
private const val FIELD_TINT = 0.1f

private val CONTROLS_MAX_WIDTH = 560.dp

/** Whether the lyrics page was touched within the last [CONTROLS_HOLD_MS]. */
internal class LyricsControlsState {
    var touched by mutableStateOf(false)
        private set

    private val touches =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    fun onTouch() {
        touches.tryEmit(Unit)
    }

    suspend fun holdAfterTouches() {
        touches.collectLatest {
            touched = true
            delay(CONTROLS_HOLD_MS)
            touched = false
        }
    }
}

@Composable
internal fun rememberLyricsControlsState(): LyricsControlsState {
    val controls = remember { LyricsControlsState() }
    LaunchedEffect(controls) { controls.holdAfterTouches() }
    return controls
}

/** Reports every touch inside this element to [controls], without consuming it. */
internal fun Modifier.revealsControls(controls: LyricsControlsState): Modifier =
    pointerInput(controls) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                controls.onTouch()
            }
        }
    }

/** Play/pause, seek bar and time left floating over the lyrics, shown while paused or touched. */
@OptIn(UnstableApi::class)
@Composable
internal fun LyricsControls(
    state: DmtState,
    player: Player,
    controls: LyricsControlsState,
    modifier: Modifier = Modifier,
) {
    var width by remember { mutableIntStateOf(0) }
    val hold = rememberSeekHold(player, width)
    val clock = rememberProgressStateWithTickInterval(player, tickIntervalMs = 1000L)
    val shown = hold.shownMs(clock.currentPositionMs)
    val paused = rememberPlayPauseButtonState(player).showPlay
    val visible = paused || controls.touched || hold.scrubbing
    val field = remember { NightInk.copy(alpha = FIELD_TINT).compositeOver(Night) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier.widthIn(max = CONTROLS_MAX_WIDTH)
                    .fillMaxWidth()
                    .background(field)
                    .padding(start = 4.dp, end = 16.dp),
        ) {
            PlayPauseTap(player = player, ink = NightInk)
            SeekBar(
                fraction = { hold.fraction },
                scrubbing = hold.scrubbing,
                ink = NightInk,
                onScrub = hold::onScrub,
                onSeek = hold::onSeek,
                modifier = Modifier.weight(1f).onSizeChanged { width = it.width },
            )
            Text(
                text = "−" + (state.durationMs - shown).coerceAtLeast(0).asTime(),
                style = Numerals,
                color = NightInk.secondary(Night),
            )
        }
    }
}
