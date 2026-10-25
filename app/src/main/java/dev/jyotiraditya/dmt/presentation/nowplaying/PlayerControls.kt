package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.PlayPauseButtonState
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.media3.ui.compose.state.rememberRepeatButtonState
import androidx.media3.ui.compose.state.rememberSeekBackButtonState
import androidx.media3.ui.compose.state.rememberSeekForwardButtonState
import androidx.media3.ui.compose.state.rememberShuffleButtonState
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.ceilMinutes
import dev.jyotiraditya.dmt.presentation.player.sleepOn
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.IconChip
import dev.jyotiraditya.dmt.ui.components.IconTap
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Numerals
import dev.jyotiraditya.dmt.ui.theme.secondary
import kotlin.math.abs

private const val PLAY_PRESS_SCALE = 0.92f

private const val NORMAL_SPEED_TOLERANCE = 0.01f

/** Play first with previous and next beside it; shuffle and repeat at the end. */
@OptIn(UnstableApi::class)
@Composable
internal fun Transport(
    player: Player,
    steps: SeekSteps,
    field: Color,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    val playPause = rememberPlayPauseButtonState(player)
    val previous = rememberPreviousButtonState(player)
    val next = rememberNextButtonState(player)
    val seekBack = rememberSeekBackButtonState(player)
    val seekForward = rememberSeekForwardButtonState(player)
    val shuffle = rememberShuffleButtonState(player)
    val repeat = rememberRepeatButtonState(player, REPEAT_MODES)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.size(PlayerDimens.PlayButton)
                    .pressable(
                        onClick = playPause::onClick,
                        enabled = playPause.isEnabled,
                        sink = PLAY_PRESS_SCALE,
                        label = stringResource(playPause.label),
                    )
                    .background(ink),
        ) {
            DmtIcon(icon = playPause.icon, tint = field, size = PlayerDimens.PlayGlyph)
        }
        Spacer(modifier = Modifier.width(PlayerDimens.PlayToSkip))
        IconTap(
            icon = DmtIcons.Prev,
            description = stringResource(R.string.player_previous),
            onClick = previous::onClick,
            onHold = {
                seekBack.onClick()
                steps.onStep(-seekBack.seekBackAmountMs)
            },
            enabled = previous.isEnabled,
            ink = ink,
            size = PlayerDimens.SkipButton,
            iconSize = PlayerDimens.SkipGlyph,
        )
        IconTap(
            icon = DmtIcons.Next,
            description = stringResource(R.string.player_next),
            onClick = next::onClick,
            onHold = {
                seekForward.onClick()
                steps.onStep(seekForward.seekForwardAmountMs)
            },
            enabled = next.isEnabled,
            ink = ink,
            size = PlayerDimens.SkipButton,
            iconSize = PlayerDimens.SkipGlyph,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconTap(
            icon = DmtIcons.Shuffle,
            description = stringResource(R.string.player_shuffle),
            active = shuffle.shuffleOn,
            onClick = shuffle::onClick,
            enabled = shuffle.isEnabled,
            ink = ink,
            field = field,
        )
        IconTap(
            icon =
                if (repeat.repeatModeState == Player.REPEAT_MODE_ONE) {
                    DmtIcons.RepeatOne
                } else {
                    DmtIcons.Repeat
                },
            description = stringResource(R.string.player_repeat),
            active = repeat.repeatModeState != Player.REPEAT_MODE_OFF,
            onClick = repeat::onClick,
            enabled = repeat.isEnabled,
            ink = ink,
            field = field,
        )
    }
}

/** The repeat modes a tap steps through, in order. */
private val REPEAT_MODES =
    listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_ONE)

/** The icon for what a tap does: play, or pause. */
@get:OptIn(UnstableApi::class)
internal val PlayPauseButtonState.icon: Int
    get() = if (showPlay) DmtIcons.Play else DmtIcons.Pause

/** The name of what a tap does. */
@get:OptIn(UnstableApi::class)
internal val PlayPauseButtonState.label: Int
    get() = if (showPlay) R.string.player_play else R.string.player_pause

/** A play or pause button for [player], in [ink]. */
@OptIn(UnstableApi::class)
@Composable
internal fun PlayPauseTap(
    player: Player,
    ink: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
) {
    val playPause = rememberPlayPauseButtonState(player)
    IconTap(
        icon = playPause.icon,
        description = stringResource(playPause.label),
        onClick = playPause::onClick,
        enabled = playPause.isEnabled,
        ink = ink,
        size = size,
        iconSize = iconSize,
        modifier = modifier,
    )
}

/** The sleep timer and speed chips, and the queue with how many tracks are left. */
@OptIn(UnstableApi::class)
@Composable
internal fun Extras(
    state: DmtState,
    player: Player,
    field: Color,
    ink: Color,
    dispatch: (DmtAction) -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheet by rememberSaveable { mutableStateOf<ExtraSheet?>(null) }
    val speed = rememberPlaybackSpeedState(player).playbackSpeed
    Row(
        horizontalArrangement = Arrangement.spacedBy(PlayerDimens.ToggleGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        IconChip(
            icon = DmtIcons.Sleep,
            description = stringResource(R.string.sleep_title),
            value =
                sleepLeft(state)?.let { left ->
                    if (left > 0L) {
                        stringResource(R.string.sleep_minutes, left.ceilMinutes())
                    } else {
                        stringResource(R.string.sleep_song_end)
                    }
                },
            on = state.sleepOn,
            onClick = { sheet = ExtraSheet.SLEEP },
            ink = ink,
            field = field,
        )
        val speedChanged = abs(speed - 1f) > NORMAL_SPEED_TOLERANCE
        val speedLabel = stringResource(R.string.speed_value, speed.speedLabel())
        IconChip(
            icon = DmtIcons.Speed,
            description = stringResource(R.string.speed_title),
            value = speedLabel.takeIf { speedChanged },
            on = speedChanged,
            onClick = { sheet = ExtraSheet.SPEED },
            ink = ink,
            field = field,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(PlayerDimens.ToggleGap, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.widthIn(min = PlayerDimens.QueueTarget)
                    .pressable(onClick = onOpenQueue, label = stringResource(R.string.queue_title))
                    .padding(vertical = PlayerDimens.ToggleGap),
        ) {
            val left = (state.queue.size - state.queuePosition - 1).coerceAtLeast(0)
            if (left > 0) Text(text = "$left", style = Numerals, color = ink.secondary(field))
            DmtIcon(icon = DmtIcons.Queue, tint = ink, size = PlayerDimens.QueueGlyph)
        }
    }
    when (sheet) {
        ExtraSheet.SLEEP -> SleepSheet(state, dispatch, onDismiss = { sheet = null })
        ExtraSheet.SPEED -> SpeedSheet(state, speed, dispatch, onDismiss = { sheet = null })
        null -> Unit
    }
}

private enum class ExtraSheet {
    SLEEP,
    SPEED,
}
