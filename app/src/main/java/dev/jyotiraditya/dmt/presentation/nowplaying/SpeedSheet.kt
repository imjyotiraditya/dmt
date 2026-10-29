package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.SPEED_RANGE
import dev.jyotiraditya.dmt.domain.model.steppedSpeed
import dev.jyotiraditya.dmt.presentation.settings.ToggleRow
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.FlatSlider
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.Stepper
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.util.asTime

private val SPEED_MARKS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

/** Sets the playback speed by step or slider, showing the song's length at that speed. */
@Composable
internal fun SpeedSheet(
    state: NowPlayingUiState,
    playbackSpeed: Float,
    actions: NowPlayingActions,
    onDismiss: () -> Unit,
) {
    val speed = steppedSpeed(playbackSpeed)
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(
            title = stringResource(R.string.speed_title),
            meta =
                stringResource(
                    when {
                        speed < 1f -> R.string.speed_slower
                        speed > 1f -> R.string.speed_faster
                        else -> R.string.speed_normal
                    },
                ),
        )
        Stepper(
            value = stringResource(R.string.speed_value, speed.speedLabel()),
            onStep = { actions.setSpeed(steppedSpeed(speed, it)) },
            lessLabel = stringResource(R.string.speed_slower),
            moreLabel = stringResource(R.string.speed_faster),
            canLess = speed > SPEED_RANGE.start,
            canMore = speed < SPEED_RANGE.endInclusive,
            valueLabel = stringResource(R.string.speed_reset),
            onValueClick = { actions.setSpeed(1f) },
            modifier = Modifier.padding(horizontal = Margin, vertical = 8.dp),
        )
        if (state.durationMs > 0L) {
            Text(
                text =
                    stringResource(
                        R.string.speed_song_length,
                        state.durationMs.asTime(),
                        (state.durationMs / speed).toLong().asTime(),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Margin),
            )
        }
        FlatSlider(
            value = speed,
            onValueChange = actions::setSpeed,
            range = SPEED_RANGE,
            label = stringResource(R.string.speed_title),
            marks =
                SPEED_MARKS.map { mark ->
                    mark to stringResource(R.string.speed_value, mark.speedLabel())
                },
            modifier = Modifier.padding(horizontal = Margin, vertical = 12.dp),
        )
        ToggleRow(
            label = stringResource(R.string.speed_keep_pitch),
            hint = stringResource(R.string.speed_keep_pitch_hint),
            checked = state.settings.keepPitch,
            onToggle = { actions.updateSettings { it.copy(keepPitch = !it.keepPitch) } },
        )
    }
}

/** This speed without trailing zeros, such as `1.25` or `2`. */
internal fun Float.speedLabel(): String =
    steppedSpeed(this).toBigDecimal().stripTrailingZeros().toPlainString()
