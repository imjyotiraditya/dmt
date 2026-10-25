package dev.jyotiraditya.dmt.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository
import dev.jyotiraditya.dmt.presentation.home.ListenPane
import dev.jyotiraditya.dmt.presentation.library.BackLink
import dev.jyotiraditya.dmt.presentation.library.LibraryPage
import dev.jyotiraditya.dmt.presentation.library.libraryBack
import dev.jyotiraditya.dmt.presentation.nowplaying.LocalPlayer
import dev.jyotiraditya.dmt.presentation.nowplaying.LyricsSourceSheet
import dev.jyotiraditya.dmt.presentation.nowplaying.MiniPlayerHeight
import dev.jyotiraditya.dmt.presentation.nowplaying.NowPlayingSurface
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.presentation.player.nowPlayingTrack
import dev.jyotiraditya.dmt.presentation.settings.SettingsPage
import dev.jyotiraditya.dmt.presentation.settings.settingsBack
import dev.jyotiraditya.dmt.ui.adaptive.windowShape
import dev.jyotiraditya.dmt.ui.components.LocalArtwork
import dev.jyotiraditya.dmt.ui.components.Notice
import dev.jyotiraditya.dmt.ui.motion.FADE_MS
import dev.jyotiraditya.dmt.ui.theme.Ground
import kotlinx.coroutines.launch

/** Bottom padding that keeps the last row of a list clear of what sits below it. */
val LocalContentBottom = compositionLocalOf { 0.dp }

private val LIBRARY_VIEWS =
    setOf(
        DmtView.LIBRARY,
        DmtView.ALBUMS,
        DmtView.ARTISTS,
        DmtView.GENRES,
        DmtView.FOLDERS,
        DmtView.PLAYLISTS,
    )

/**
 * The app's main screen, with the player in a bottom sheet over it; [field] is the colour of the
 * cover playing now, and [player] is null until playback connects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmtScreen(
    state: DmtState,
    player: Player?,
    field: Color,
    dispatch: (DmtAction) -> Unit,
    artwork: ArtworkRepository,
) {
    var place by rememberSaveable { mutableStateOf(Place.LISTEN) }
    val sheet = rememberStandardBottomSheetState()
    val scope = rememberCoroutineScope()
    val expanded = sheet.targetValue == SheetValue.Expanded

    fun expand(open: Boolean) {
        scope.launch { if (open) sheet.expand() else sheet.partialExpand() }
    }

    fun go(next: Place, view: DmtView? = null) {
        place = next
        view?.let { dispatch(DmtAction.Show(it)) }
    }

    val back: BackLink? =
        when (place) {
            Place.LIBRARY -> libraryBack(state)
            Place.SETTINGS -> settingsBack(state)
            Place.LISTEN -> null
        }

    fun goBack() {
        when {
            back != null -> back.actions.forEach(dispatch)
            state.query.isNotEmpty() -> dispatch(DmtAction.Query(""))
            else -> place = Place.LISTEN
        }
    }
    BackHandler(enabled = expanded) { expand(false) }
    BackHandler(
        enabled = !expanded && (back != null || state.query.isNotEmpty() || place != Place.LISTEN),
    ) {
        goBack()
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val showing = player != null && state.nowPlayingId != null
    val peek = if (showing) MiniPlayerHeight + navBottom else 0.dp
    LaunchedEffect(showing) { if (!showing) sheet.partialExpand() }

    CompositionLocalProvider(LocalArtwork provides artwork, LocalPlayer provides player) {
        BottomSheetScaffold(
            sheetContent = {
                if (player != null && showing) {
                    NowPlayingSurface(
                        state = state,
                        player = player,
                        sheet = sheet,
                        peek = peek,
                        field = field,
                        dispatch = dispatch,
                        onExpand = ::expand,
                        onOpenArtist = {
                            val track = state.nowPlayingTrack
                            val artist = track?.let {
                                state.library.artist(it.artist)
                                    ?: state.library.artistOf(it)
                                    ?: state.artists.find { artist ->
                                        it.artist.startsWith(artist.name)
                                    }
                            }
                            if (artist != null) {
                                go(Place.LIBRARY, DmtView.ARTISTS)
                                dispatch(DmtAction.OpenAlbum(null))
                                dispatch(DmtAction.OpenArtist(artist.name))
                                expand(false)
                            }
                        },
                    )
                }
            },
            scaffoldState = rememberBottomSheetScaffoldState(sheet),
            sheetPeekHeight = peek,
            sheetMaxWidth = Dp.Unspecified,
            sheetContainerColor = Color.Transparent,
            sheetShadowElevation = 0.dp,
            sheetDragHandle = null,
            sheetSwipeEnabled = showing,
            containerColor = Ground,
        ) { sheetPadding ->
            val rail = windowShape().rail
            val openPlace = { next: Place ->
                go(
                    next,
                    when (next) {
                        Place.LIBRARY -> if (state.view in LIBRARY_VIEWS) null else DmtView.LIBRARY
                        Place.SETTINGS -> DmtView.SETTINGS
                        Place.LISTEN -> null
                    },
                )
            }
            Column(modifier = Modifier.fillMaxSize()) {
                if (!rail) {
                    TopBar(place = place, back = back, onBack = ::goBack, onPlace = openPlace)
                }
                Row(modifier = Modifier.weight(1f)) {
                    if (rail) {
                        SideRail(place = place, onPlace = openPlace)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (rail) {
                            Spacer(
                                modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars),
                            )
                            AnimatedContent(
                                targetState = back?.label,
                                transitionSpec = {
                                    fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
                                },
                                label = "railBack",
                            ) { label ->
                                if (label != null) BackRow(label = label, onBack = ::goBack)
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = place,
                                transitionSpec = {
                                    fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                                },
                                label = "place",
                            ) { shown ->
                                CompositionLocalProvider(LocalContentBottom provides 24.dp) {
                                    when (shown) {
                                        Place.LISTEN ->
                                            ListenPane(
                                                state = state,
                                                dispatch = dispatch,
                                                onOpenAlbum = { name ->
                                                    go(Place.LIBRARY, DmtView.ALBUMS)
                                                    dispatch(DmtAction.OpenAlbum(name))
                                                },
                                                onOpenArtist = { name ->
                                                    go(Place.LIBRARY, DmtView.ARTISTS)
                                                    dispatch(DmtAction.OpenAlbum(null))
                                                    dispatch(DmtAction.OpenArtist(name))
                                                },
                                                onOpenLibrary = {
                                                    go(Place.LIBRARY, DmtView.LIBRARY)
                                                },
                                                onOpenSources = {
                                                    go(Place.SETTINGS, DmtView.SOURCES)
                                                },
                                            )

                                        Place.LIBRARY ->
                                            LibraryPage(
                                                state = state,
                                                dispatch = dispatch,
                                            )

                                        Place.SETTINGS ->
                                            SettingsPage(
                                                state = state,
                                                cover = field,
                                                dispatch = dispatch,
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
                Notice(error = state.error, notice = state.notice)
                Spacer(
                    modifier =
                        Modifier.height(
                            sheetPadding.calculateBottomPadding().coerceAtLeast(navBottom),
                        ),
                )
            }
        }

        if (state.lyricsSourcesOpen) {
            LyricsSourceSheet(
                trackTitle = state.title,
                current = state.lyricsShowingFrom ?: state.lyricsSource,
                sources = state.lyricsSources,
                onPick = { dispatch(DmtAction.SetLyricsSource(it)) },
                onDismiss = { dispatch(DmtAction.CloseLyricsSources) },
            )
        }
    }
}
