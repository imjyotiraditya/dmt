package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.BuildConfig
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.presentation.player.SheetHeader
import dev.jyotiraditya.dmt.presentation.player.TuiSheet
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.util.allFilesAccess

private val COVER_COLS_STEPS = listOf(48, 64, 80, 96)

fun nextCoverCols(current: Int): Int =
    COVER_COLS_STEPS[(COVER_COLS_STEPS.indexOf(current) + 1).mod(COVER_COLS_STEPS.size)]

@Composable
fun SettingsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val settings = state.settings
    val on = stringResource(R.string.on)
    val off = stringResource(R.string.off)
    var askFilesAccess by remember { mutableStateOf(false) }

    if (askFilesAccess) {
        TuiSheet(onDismiss = { askFilesAccess = false }) {
            SheetHeader(title = stringResource(R.string.lyrics_files_needed))
            Text(
                text = stringResource(R.string.lyrics_files_why),
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            TuiKey(
                label = stringResource(R.string.grant),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                askFilesAccess = false
                dispatch(DmtAction.Show(DmtView.PERMISSIONS))
            }
        }
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Caption(stringResource(R.string.section_playback))

        SettingRow(
            label = stringResource(R.string.set_normalize),
            value = if (settings.normalizeVolume) on else off,
        ) {
            dispatch(
                DmtAction.Config(
                    settings.copy(normalizeVolume = !settings.normalizeVolume),
                ),
            )
        }
        SettingRow(
            label = stringResource(R.string.set_stop_on_dismiss),
            value = if (settings.stopOnDismiss) on else off,
        ) {
            dispatch(DmtAction.Config(settings.copy(stopOnDismiss = !settings.stopOnDismiss)))
        }

        Caption(stringResource(R.string.section_lyrics))

        SettingRow(
            label = stringResource(R.string.set_lyrics_source),
            value = stringResource(
                if (settings.lyricsFromFile) {
                    R.string.lyrics_source_file
                } else {
                    R.string.lyrics_source_tag
                },
            ),
        ) {
            if (!settings.lyricsFromFile && !allFilesAccess) {
                askFilesAccess = true
            } else {
                dispatch(DmtAction.Config(settings.copy(lyricsFromFile = !settings.lyricsFromFile)))
            }
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
        Caption(stringResource(R.string.section_display))

        SettingRow(
            label = stringResource(R.string.set_wave),
            value = if (settings.wave) on else off,
        ) {
            dispatch(DmtAction.Config(settings.copy(wave = !settings.wave)))
        }
        SettingRow(
            label = stringResource(R.string.set_detail),
            value = pluralStringResource(R.plurals.set_detail_value, settings.cols, settings.cols),
        ) {
            dispatch(DmtAction.Config(settings.copy(cols = nextCoverCols(settings.cols))))
        }
        SettingRow(
            label = stringResource(R.string.set_raw),
            value = if (settings.rawArt) on else off,
        ) {
            dispatch(DmtAction.Config(settings.copy(rawArt = !settings.rawArt)))
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
        SettingRow(
            label = stringResource(R.string.perms_title),
            value = stringResource(R.string.perms_manage),
        ) {
            dispatch(DmtAction.Show(DmtView.PERMISSIONS))
        }
        if (settings.sourceMode == SourceMode.LOCAL) {
            SettingRow(
                label = stringResource(R.string.blocklist_title),
                value = if (settings.blockedFolders.isEmpty()) {
                    stringResource(R.string.blocklist_edit)
                } else {
                    pluralStringResource(
                        R.plurals.blocklist_edit_count,
                        settings.blockedFolders.size,
                        settings.blockedFolders.size,
                    )
                },
            ) {
                dispatch(DmtAction.Show(DmtView.BLOCKLIST))
            }
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
                text = "▌ ",
                style = MaterialTheme.typography.labelMedium,
                color = TuiAccent,
            )
            Text(
                text = stringResource(R.string.credit),
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
            )
            Text(
                text = " ↗",
                style = MaterialTheme.typography.labelMedium,
                color = TuiAccent,
            )
        }
    }
}

@Composable
fun SettingRow(
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
        HorizontalDivider(color = TuiLine)
    }
}
