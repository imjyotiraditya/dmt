package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.BuildConfig
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine

private val COVER_COLS_STEPS = listOf(48, 64, 80, 96)

@Composable
fun SettingsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val settings = state.settings
    val on = stringResource(R.string.on)
    val off = stringResource(R.string.off)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Caption(stringResource(R.string.config))

        SettingRow(
            label = stringResource(R.string.set_wave),
            value = if (settings.wave) on else off,
        ) {
            dispatch(DmtAction.Config(settings.copy(wave = !settings.wave)))
        }
        SettingRow(
            label = stringResource(R.string.set_detail),
            value = stringResource(R.string.set_detail_value, settings.cols),
        ) {
            val currentIndex = COVER_COLS_STEPS.indexOf(settings.cols)
            val next = COVER_COLS_STEPS[(currentIndex + 1).mod(COVER_COLS_STEPS.size)]
            dispatch(DmtAction.Config(settings.copy(cols = next)))
        }
        SettingRow(
            label = stringResource(R.string.set_raw),
            value = if (settings.rawArt) on else off,
        ) {
            dispatch(DmtAction.Config(settings.copy(rawArt = !settings.rawArt)))
        }
        SettingRow(
            label = stringResource(R.string.set_lyrics_script),
            value = stringResource(
                if (settings.romanizedLyrics) {
                    R.string.lyrics_script_romanized
                } else {
                    R.string.lyrics_script_original
                },
            ),
        ) {
            dispatch(
                DmtAction.Config(
                    settings.copy(romanizedLyrics = !settings.romanizedLyrics),
                ),
            )
        }
        SettingRow(
            label = stringResource(R.string.set_specs),
            value = if (settings.listSpecs) on else off,
        ) {
            dispatch(DmtAction.Config(settings.copy(listSpecs = !settings.listSpecs)))
        }
        Caption(stringResource(R.string.tools))
        SettingRow(
            label = stringResource(R.string.set_eq),
            value = stringResource(R.string.set_eq_open),
        ) {
            dispatch(DmtAction.OpenEqualizer)
        }
        SettingRow(
            label = stringResource(R.string.stats),
            value = stringResource(R.string.stat_view),
        ) {
            dispatch(DmtAction.Show(DmtView.STATS))
        }
        SettingRow(
            label = stringResource(R.string.set_rescan),
            value = stringResource(R.string.run),
        ) {
            dispatch(DmtAction.Rescan)
        }

        Caption(stringResource(R.string.about))
        Text(
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.bodyMedium,
            color = TuiFg,
        )
        Text(
            text = stringResource(R.string.about_body),
            style = MaterialTheme.typography.labelSmall,
            color = TuiDim,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            modifier = Modifier.padding(top = 6.dp),
        )

        val uriHandler = LocalUriHandler.current
        val creditUrl = stringResource(R.string.credit_url)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 12.dp)
                .tuiClickable { runCatching { uriHandler.openUri(creditUrl) } },
        ) {
            Text(
                text = "▪ ",
                style = MaterialTheme.typography.labelMedium,
                color = LocalAccent.current,
            )
            Text(
                text = stringResource(R.string.credit),
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
            )
            Text(
                text = " ↗",
                style = MaterialTheme.typography.labelMedium,
                color = LocalAccent.current,
            )
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TuiFg,
            )
            TuiKey(
                label = "[ $value ]",
                onClick = onClick,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TuiLine),
        )
    }
}
