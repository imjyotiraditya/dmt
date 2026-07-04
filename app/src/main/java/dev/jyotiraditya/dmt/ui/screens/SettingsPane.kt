package dev.jyotiraditya.dmt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtState
import dev.jyotiraditya.dmt.ui.components.Caption
import dev.jyotiraditya.dmt.ui.components.TuiKey
import dev.jyotiraditya.dmt.ui.theme.AccentPalette
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine

@Composable
fun SettingsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val settings = state.settings
    val on = stringResource(R.string.on)
    val off = stringResource(R.string.off)

    Column {
        Caption(stringResource(R.string.config))

        SettingRow(stringResource(R.string.set_wave), if (settings.wave) on else off) {
            dispatch(DmtAction.Config(settings.copy(wave = !settings.wave)))
        }
        SettingRow(
            label = stringResource(R.string.set_detail),
            value = stringResource(R.string.set_detail_value, settings.cols)
        ) {
            val next = when (settings.cols) {
                48 -> 64
                64 -> 80
                else -> 48
            }
            dispatch(DmtAction.Config(settings.copy(cols = next)))
        }
        SettingRow(stringResource(R.string.set_raw), if (settings.rawArt) on else off) {
            dispatch(DmtAction.Config(settings.copy(rawArt = !settings.rawArt)))
        }
        SettingRow(stringResource(R.string.set_specs), if (settings.listSpecs) on else off) {
            dispatch(DmtAction.Config(settings.copy(listSpecs = !settings.listSpecs)))
        }
        SettingRow(
            label = stringResource(R.string.set_accent),
            value = AccentPalette[settings.accent % AccentPalette.size].first
        ) {
            dispatch(
                DmtAction.Config(
                    settings.copy(accent = (settings.accent + 1) % AccentPalette.size)
                )
            )
        }
        SettingRow(stringResource(R.string.set_eq), stringResource(R.string.set_eq_open)) {
            dispatch(DmtAction.OpenEqualizer)
        }
        SettingRow(stringResource(R.string.set_rescan), stringResource(R.string.run)) {
            dispatch(DmtAction.Rescan)
        }

        Caption(stringResource(R.string.stats))
        StatLine(
            label = stringResource(R.string.stat_time),
            value = formatListenTime(state.stats.totalMs)
        )
        StatLine(
            label = stringResource(R.string.stat_plays),
            value = "${state.stats.counts.values.sum()}"
        )
        val top = state.stats.counts.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapNotNull { entry ->
                state.tracks.find { it.id == entry.key }?.let { it.title to entry.value }
            }
        if (top.isNotEmpty()) {
            Text(
                text = stringResource(R.string.stat_top),
                style = MaterialTheme.typography.labelSmall,
                color = TuiDim,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
            top.forEachIndexed { index, (title, count) ->
                Text(
                    text = "%02d  %s · %d".format(index + 1, title, count).lowercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TuiFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }

        Caption(stringResource(R.string.about))
        Text(
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.bodyMedium,
            color = TuiFg
        )
        Text(
            text = stringResource(R.string.about_body),
            style = MaterialTheme.typography.labelSmall,
            color = TuiDim,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = stringResource(R.string.version),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private fun formatListenTime(ms: Long): String {
    val minutes = ms / 60_000L
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}

@Composable
private fun StatLine(label: String, value: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TuiFg
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = TuiDim
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TuiLine)
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TuiFg
            )
            TuiKey("[ $value ]", onClick = onClick)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TuiLine)
        )
    }
}
