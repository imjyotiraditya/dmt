package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jyotiraditya.dmt.BuildConfig
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import dev.jyotiraditya.dmt.ui.icons.DmtIcons

/** Settings, each page inside it opened through [onOpen]; [cover] is the colour playing now. */
@Composable
fun SettingsScreen(
    cover: Color,
    onOpen: (Destination) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    var pickingAccent by remember { mutableStateOf(false) }
    val openEqualizer = rememberEqualizerLauncher()

    if (pickingAccent) {
        AccentSheet(
            current = settings.accent,
            cover = cover,
            onPick = { accent -> viewModel.update { it.copy(accent = accent) } },
            onDismiss = { pickingAccent = false },
        )
    }

    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item { GroupHeading(stringResource(R.string.settings_section_library)) }
        item {
            LinkRow(
                label = stringResource(R.string.sources_title),
                value = settings.server?.name ?: stringResource(R.string.source_kind_local),
                onClick = { onOpen(Destination.Sources) },
            )
        }
        item {
            RescanRow(
                scanning = state.scanning,
                last = state.lastScan,
                onRescan = viewModel::rescan,
            )
        }
        item {
            LinkRow(
                label = stringResource(R.string.permissions_title),
                value = null,
                onClick = { onOpen(Destination.Permissions) },
            )
        }
        if (settings.sourceMode == SourceMode.LOCAL) {
            item {
                val hidden = settings.hiddenFolders.size
                LinkRow(
                    label = stringResource(R.string.folders_title),
                    value =
                        if (hidden == 0) {
                            stringResource(R.string.folders_all_shown)
                        } else {
                            pluralStringResource(R.plurals.folders_hidden_count, hidden, hidden)
                        },
                    onClick = { onOpen(Destination.LibraryFolders) },
                )
            }
        }

        item { GroupHeading(stringResource(R.string.settings_section_playback)) }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_volume_levelling),
                hint = stringResource(R.string.settings_volume_levelling_hint),
                checked = settings.normalizeVolume,
                onToggle = { viewModel.update { it.copy(normalizeVolume = !it.normalizeVolume) } },
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_stop_on_dismiss),
                hint = stringResource(R.string.settings_stop_on_dismiss_hint),
                checked = settings.stopOnDismiss,
                onToggle = { viewModel.update { it.copy(stopOnDismiss = !it.stopOnDismiss) } },
            )
        }
        if (openEqualizer != null) {
            item {
                LinkRow(
                    label = stringResource(R.string.settings_equalizer),
                    value = null,
                    onClick = openEqualizer,
                    icon = DmtIcons.ArrowOut,
                )
            }
        }

        item { GroupHeading(stringResource(R.string.settings_section_look)) }
        item {
            LinkRow(
                label = stringResource(R.string.settings_accent),
                value = stringResource(settings.accent.title),
                onClick = { pickingAccent = true },
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_animated_art),
                hint = stringResource(R.string.settings_animated_art_hint),
                checked = settings.animatedArt,
                onToggle = { viewModel.update { it.copy(animatedArt = !it.animatedArt) } },
                info = stringResource(R.string.settings_animated_art_info),
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_romanized_lyrics),
                hint = stringResource(R.string.settings_romanized_lyrics_hint),
                checked = settings.romanizedLyrics,
                onToggle = { viewModel.update { it.copy(romanizedLyrics = !it.romanizedLyrics) } },
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_singer_colors),
                hint = stringResource(R.string.settings_singer_colors_hint),
                checked = settings.singerColors,
                onToggle = { viewModel.update { it.copy(singerColors = !it.singerColors) } },
            )
        }

        item { About(onOpenLicenses = { onOpen(Destination.Licenses) }) }
    }
}

/** The app's version, who made it, and the open-source licenses. */
@Composable
private fun About(onOpenLicenses: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val creditUrl = stringResource(R.string.about_credit_url)
    GroupHeading(stringResource(R.string.settings_section_about))
    InfoRow(label = stringResource(R.string.about_version), value = BuildConfig.VERSION_NAME)
    LinkRow(
        label = stringResource(R.string.about_developer),
        value = stringResource(R.string.about_credit_name),
        onClick = { runCatching { uriHandler.openUri(creditUrl) } },
        icon = DmtIcons.ArrowOut,
    )
    LinkRow(
        label = stringResource(R.string.licenses_title),
        value = null,
        onClick = onOpenLicenses,
    )
}
