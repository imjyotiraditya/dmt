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
import dev.jyotiraditya.dmt.BuildConfig
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.library.BackLink
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.ui.components.ScrollMemory
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.motion.Depth

/** The way back out of a page inside Settings, or null at its top. */
@Composable
fun settingsBack(state: DmtState): BackLink? = when (state.view) {
    DmtView.SOURCE_KINDS ->
        BackLink(
            stringResource(R.string.sources_title),
            listOf(DmtAction.Show(DmtView.SOURCES)),
        )

    DmtView.SOURCE_FORM -> {
        val backTo = state.sourceForm.backTo
        BackLink(
            stringResource(
                if (backTo == DmtView.SOURCE_KINDS) {
                    R.string.sources_add
                } else {
                    R.string.sources_title
                },
            ),
            listOf(DmtAction.Show(backTo)),
        )
    }

    DmtView.SOURCES,
    DmtView.PERMISSIONS,
    DmtView.LIBRARY_FOLDERS,
    DmtView.LICENSES,
    ->
        BackLink(
            stringResource(R.string.nav_settings),
            listOf(DmtAction.Show(DmtView.SETTINGS)),
        )

    else -> null
}

private val SETTINGS_PAGES =
    setOf(
        DmtView.SOURCES,
        DmtView.SOURCE_KINDS,
        DmtView.SOURCE_FORM,
        DmtView.PERMISSIONS,
        DmtView.LIBRARY_FOLDERS,
        DmtView.LICENSES,
    )

/** Settings and the pages inside it; [cover] is the colour of the cover playing now. */
@Composable
fun SettingsPage(state: DmtState, cover: Color, dispatch: (DmtAction) -> Unit) {
    val page = state.view.takeIf { it in SETTINGS_PAGES } ?: DmtView.SETTINGS
    Depth(
        target = page,
        keyOf = { it },
        depthOf = {
            when (it) {
                DmtView.SOURCE_KINDS -> 2
                DmtView.SOURCE_FORM -> 3
                DmtView.SETTINGS -> 0
                else -> 1
            }
        },
    ) { shown ->
        when (shown) {
            DmtView.SOURCES -> ScrollMemory("sources") { SourcesPage(state, dispatch) }

            DmtView.SOURCE_KINDS -> ScrollMemory("kinds") { SourceKindsPage(dispatch) }

            DmtView.SOURCE_FORM -> SourceFormPage(state, dispatch)

            DmtView.PERMISSIONS -> ScrollMemory("perms") { PermissionsPage(state, dispatch) }

            DmtView.LIBRARY_FOLDERS ->
                ScrollMemory("folders") {
                    LibraryFoldersPage(state, dispatch)
                }

            DmtView.LICENSES -> ScrollMemory("licenses") { LicensesPage() }

            else -> ScrollMemory("settings") { SettingsHome(state, cover, dispatch) }
        }
    }
}

@Composable
private fun SettingsHome(state: DmtState, cover: Color, dispatch: (DmtAction) -> Unit) {
    val settings = state.settings
    fun save(next: DmtSettings) = dispatch(DmtAction.Config(next))
    var pickingAccent by remember { mutableStateOf(false) }
    val openEqualizer = rememberEqualizerLauncher()

    if (pickingAccent) {
        AccentSheet(
            current = settings.accent,
            cover = cover,
            onPick = { save(settings.copy(accent = it)) },
            onDismiss = { pickingAccent = false },
        )
    }

    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item { GroupHeading(stringResource(R.string.settings_section_library)) }
        item {
            LinkRow(
                label = stringResource(R.string.sources_title),
                value = settings.server?.name ?: stringResource(R.string.source_kind_local),
                onClick = { dispatch(DmtAction.Show(DmtView.SOURCES)) },
            )
        }
        item {
            RescanRow(
                scanning = state.scanning,
                last = state.lastScan,
                onRescan = { dispatch(DmtAction.Rescan) },
            )
        }
        item {
            LinkRow(
                label = stringResource(R.string.permissions_title),
                value = null,
                onClick = { dispatch(DmtAction.Show(DmtView.PERMISSIONS)) },
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
                    onClick = { dispatch(DmtAction.Show(DmtView.LIBRARY_FOLDERS)) },
                )
            }
        }

        item { GroupHeading(stringResource(R.string.settings_section_playback)) }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_volume_levelling),
                hint = stringResource(R.string.settings_volume_levelling_hint),
                checked = settings.normalizeVolume,
                onToggle = { save(settings.copy(normalizeVolume = !settings.normalizeVolume)) },
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_stop_on_dismiss),
                hint = stringResource(R.string.settings_stop_on_dismiss_hint),
                checked = settings.stopOnDismiss,
                onToggle = { save(settings.copy(stopOnDismiss = !settings.stopOnDismiss)) },
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
                onToggle = { save(settings.copy(animatedArt = !settings.animatedArt)) },
                info = stringResource(R.string.settings_animated_art_info),
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_romanized_lyrics),
                hint = stringResource(R.string.settings_romanized_lyrics_hint),
                checked = settings.romanizedLyrics,
                onToggle = { save(settings.copy(romanizedLyrics = !settings.romanizedLyrics)) },
            )
        }
        item {
            ToggleRow(
                label = stringResource(R.string.settings_singer_colors),
                hint = stringResource(R.string.settings_singer_colors_hint),
                checked = settings.singerColors,
                onToggle = { save(settings.copy(singerColors = !settings.singerColors)) },
            )
        }

        item { About(dispatch) }
    }
}

/** The app's version, who made it, and the open-source licenses. */
@Composable
private fun About(dispatch: (DmtAction) -> Unit) {
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
        onClick = { dispatch(DmtAction.Show(DmtView.LICENSES)) },
    )
}
