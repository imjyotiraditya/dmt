package dev.jyotiraditya.dmt.presentation.nowplaying

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.SLEEP_RANGE
import dev.jyotiraditya.dmt.presentation.player.ceilMinutes
import dev.jyotiraditya.dmt.presentation.player.sleepOn
import dev.jyotiraditya.dmt.presentation.player.steppedSleepMinutes
import dev.jyotiraditya.dmt.presentation.settings.ToggleRow
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.Stepper
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.WordToggle
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import java.util.Date
import kotlin.time.Duration.Companion.minutes

private val SLEEP_PRESETS = listOf(15, 30, 45, 60)

/** Picks a sleep timer length, or the end of the song, and starts it on the button. */
@Composable
internal fun SleepSheet(state: DmtState, dispatch: (DmtAction) -> Unit, onDismiss: () -> Unit) {
    val settings = state.settings
    val left = sleepLeft(state)
    var minutes by rememberSaveable {
        mutableIntStateOf(left?.takeIf { it > 0L }?.ceilMinutes() ?: settings.sleepCustomMinutes)
    }
    var songEnd by rememberSaveable { mutableStateOf(left == 0L) }
    val minutesLabel = sleepLength(minutes)
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(
            title = stringResource(R.string.sleep_title),
            meta =
                when {
                    left == null -> stringResource(R.string.sleep_off)
                    left > 0L -> stringResource(R.string.sleep_on_left, left.ceilMinutes())
                    else -> stringResource(R.string.sleep_on_song_end)
                },
        )
        Stepper(
            value = if (songEnd) stringResource(R.string.sleep_song_end) else minutesLabel,
            onStep = { step ->
                minutes = if (songEnd) minutes else steppedSleepMinutes(minutes, step)
                songEnd = false
            },
            lessLabel = stringResource(R.string.sleep_shorter),
            moreLabel = stringResource(R.string.sleep_longer),
            canLess = songEnd || minutes > SLEEP_RANGE.first,
            canMore = songEnd || minutes < SLEEP_RANGE.last,
            modifier = Modifier.padding(horizontal = Margin, vertical = 8.dp),
        )
        Text(
            text =
                when {
                    songEnd -> stringResource(R.string.sleep_stops_at_song_end)

                    settings.sleepFinishSong ->
                        stringResource(R.string.sleep_stops_after_song, stopTime(minutes))

                    else -> stringResource(R.string.sleep_stops_at, stopTime(minutes))
                },
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Margin),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = Margin, vertical = 16.dp),
        ) {
            SLEEP_PRESETS.forEach { preset ->
                WordToggle(
                    label = sleepLength(preset),
                    on = !songEnd && minutes == preset,
                    onClick = {
                        minutes = preset
                        songEnd = false
                    },
                    field = Ground,
                )
            }
            WordToggle(
                label = stringResource(R.string.sleep_song_end),
                on = songEnd,
                onClick = { songEnd = true },
                field = Ground,
            )
        }
        ToggleRow(
            label = stringResource(R.string.sleep_finish_song),
            hint =
                stringResource(
                    if (settings.sleepFinishSong) {
                        R.string.sleep_finish_song_hint
                    } else {
                        R.string.sleep_fades_hint
                    },
                ),
            checked = settings.sleepFinishSong,
            onToggle = {
                dispatch(
                    DmtAction.Config(settings.copy(sleepFinishSong = !settings.sleepFinishSong)),
                )
            },
        )
        Block(
            label =
                stringResource(
                    if (state.sleepOn) R.string.sleep_update else R.string.sleep_start,
                    if (songEnd) stringResource(R.string.sleep_song_end) else minutesLabel,
                ),
            onClick = {
                if (songEnd) {
                    dispatch(DmtAction.SleepAtSongEnd)
                } else {
                    dispatch(DmtAction.SetSleep(minutes))
                    dispatch(DmtAction.Config(settings.copy(sleepCustomMinutes = minutes)))
                }
                dismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Margin, vertical = 8.dp),
        )
        if (state.sleepOn) {
            TextLink(
                label = stringResource(R.string.sleep_turn_off),
                onClick = {
                    dispatch(DmtAction.SetSleep(0))
                    dismiss()
                },
                modifier = Modifier.padding(horizontal = Margin),
            )
        }
    }
}

@Composable
private fun sleepLength(minutes: Int): String = if (minutes == 60) {
    stringResource(R.string.sleep_hour)
} else {
    stringResource(R.string.sleep_minutes, minutes)
}

/** The clock time [minutes] from now, in the user's 12 or 24 hour format. */
@Composable
private fun stopTime(minutes: Int): String = DateFormat.getTimeFormat(LocalContext.current)
    .format(Date(System.currentTimeMillis() + minutes.minutes.inWholeMilliseconds))
