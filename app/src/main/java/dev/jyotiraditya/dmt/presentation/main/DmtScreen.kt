package dev.jyotiraditya.dmt.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository
import dev.jyotiraditya.dmt.presentation.home.ListenPane
import dev.jyotiraditya.dmt.presentation.library.CollectionScreen
import dev.jyotiraditya.dmt.presentation.library.LibraryScreen
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import dev.jyotiraditya.dmt.presentation.navigation.Navigator
import dev.jyotiraditya.dmt.presentation.navigation.Place
import dev.jyotiraditya.dmt.presentation.navigation.Shelf
import dev.jyotiraditya.dmt.presentation.navigation.backLabel
import dev.jyotiraditya.dmt.presentation.navigation.rememberNavigator
import dev.jyotiraditya.dmt.presentation.nowplaying.LocalPlayer
import dev.jyotiraditya.dmt.presentation.nowplaying.MiniPlayerHeight
import dev.jyotiraditya.dmt.presentation.nowplaying.NowPlayingSurface
import dev.jyotiraditya.dmt.presentation.nowplaying.NowPlayingViewModel
import dev.jyotiraditya.dmt.presentation.settings.LibraryFoldersPage
import dev.jyotiraditya.dmt.presentation.settings.LicensesPage
import dev.jyotiraditya.dmt.presentation.settings.PermissionsPage
import dev.jyotiraditya.dmt.presentation.settings.SettingsScreen
import dev.jyotiraditya.dmt.presentation.settings.SourceFormPage
import dev.jyotiraditya.dmt.presentation.settings.SourceKindsPage
import dev.jyotiraditya.dmt.presentation.settings.SourcesPage
import dev.jyotiraditya.dmt.ui.adaptive.windowShape
import dev.jyotiraditya.dmt.ui.components.LocalArtwork
import dev.jyotiraditya.dmt.ui.components.Notice
import dev.jyotiraditya.dmt.ui.components.OverlayBackHandler
import dev.jyotiraditya.dmt.ui.motion.FADE_MS
import dev.jyotiraditya.dmt.ui.motion.LocalSharedTransitionScope
import dev.jyotiraditya.dmt.ui.motion.pageTransition
import dev.jyotiraditya.dmt.ui.theme.Ground
import kotlinx.coroutines.launch

/** Bottom padding that keeps the last row of a list clear of what sits below it. */
val LocalContentBottom = compositionLocalOf { 0.dp }

/**
 * The app's main screen, with the player in a bottom sheet over it; [field] is the colour of the
 * cover playing now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmtScreen(
    field: Color,
    artwork: ArtworkRepository,
    app: AppViewModel = hiltViewModel(),
    nowPlaying: NowPlayingViewModel = hiltViewModel(),
) {
    val navigator = rememberNavigator()
    val player by nowPlaying.player.collectAsStateWithLifecycle()
    val playing by nowPlaying.state.collectAsStateWithLifecycle()
    val notice by app.notice.collectAsStateWithLifecycle()
    val error by app.error.collectAsStateWithLifecycle()
    val sheet =
        rememberBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
        )
    val scope = rememberCoroutineScope()
    val expanded = sheet.targetValue == SheetValue.Expanded

    fun expand(open: Boolean) {
        scope.launch { if (open) sheet.expand() else sheet.partialExpand() }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val showing = player != null && playing.nowPlayingId != null
    val peek = if (showing) MiniPlayerHeight + navBottom else 0.dp
    LaunchedEffect(showing) {
        if (showing && sheet.currentValue == SheetValue.Hidden) sheet.partialExpand()
    }

    val route = rememberRoute(navigator, app, field)

    CompositionLocalProvider(LocalArtwork provides artwork, LocalPlayer provides player) {
        BottomSheetScaffold(
            sheetContent = {
                val connected = player
                if (connected != null && showing) {
                    NowPlayingSurface(
                        state = playing,
                        player = connected,
                        actions = nowPlaying,
                        sheet = sheet,
                        peek = peek,
                        field = field,
                        onExpand = ::expand,
                        onOpenArtist = {
                            nowPlaying.nowPlayingArtist()?.let { artist ->
                                route.openInLibrary(Shelf.ARTISTS, Destination.Artist(artist))
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
            val backLabel = navigator.backLabel()
            Column(modifier = Modifier.fillMaxSize()) {
                if (!rail) {
                    TopBar(
                        place = navigator.place,
                        backLabel = backLabel,
                        onBack = route::back,
                        onPlace = route::show,
                    )
                }
                Row(modifier = Modifier.weight(1f)) {
                    if (rail) {
                        SideRail(place = navigator.place, onPlace = route::show)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (rail) {
                            Spacer(
                                modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars),
                            )
                            AnimatedContent(
                                targetState = backLabel,
                                transitionSpec = {
                                    fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
                                },
                                label = "railBack",
                            ) { label ->
                                if (label != null) BackRow(label = label, onBack = route::back)
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CompositionLocalProvider(LocalContentBottom provides 24.dp) {
                                Places(navigator, route)
                            }
                        }
                    }
                }
                Notice(error = error, notice = notice)
                Spacer(
                    modifier =
                        Modifier.height(
                            sheetPadding.calculateBottomPadding().coerceAtLeast(navBottom),
                        ),
                )
            }
        }
    }

    BackHandler(enabled = !expanded && !navigator.nested && navigator.canGoBack) { route.back() }
    OverlayBackHandler(enabled = expanded) { expand(false) }
}

/** The screen shown in each place, every place keeping its screens while another is shown. */
@Composable
private fun Places(navigator: Navigator, route: Route) {
    val decorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
    val entries =
        Place.entries.associateWith { place ->
            key(place) {
                rememberDecoratedNavEntries(navigator.stack(place), decorators, route.entries)
            }
        }
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            AnimatedContent(
                targetState = navigator.place,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "place",
            ) { place ->
                NavDisplay(
                    entries = entries.getValue(place),
                    sharedTransitionScope = this@SharedTransitionLayout,
                    transitionSpec = { pageTransition(forward = true) },
                    popTransitionSpec = { pageTransition(forward = false) },
                    predictivePopTransitionSpec = { pageTransition(forward = false) },
                    onBack = route::back,
                )
            }
        }
    }
}

/** How the main screen moves between places and screens, and what each screen shows. */
private class Route(
    private val navigator: Navigator,
    private val app: AppViewModel,
    field: () -> Color,
) {
    fun open(destination: Destination) {
        app.dismissError()
        navigator.open(destination)
    }

    fun show(place: Place) {
        app.dismissError()
        navigator.show(place)
    }

    fun back() {
        app.dismissError()
        navigator.back()
    }

    fun openInLibrary(shelf: Shelf, destination: Destination) {
        app.dismissError()
        navigator.openInLibrary(shelf, destination)
    }

    val entries =
        entryProvider<NavKey> {
            entry<Destination.Listen> {
                ListenPane(
                    onOpenAlbum = { openInLibrary(Shelf.ALBUMS, Destination.Album(it)) },
                    onOpenArtist = { openInLibrary(Shelf.ARTISTS, Destination.Artist(it)) },
                    onOpenLibrary = {
                        app.dismissError()
                        navigator.openLibrary(Shelf.TRACKS)
                    },
                    onOpenSources = {
                        app.dismissError()
                        navigator.openIn(Place.SETTINGS, Destination.Sources)
                    },
                )
            }
            entry<Destination.Library> {
                LibraryScreen(
                    shelf = navigator.shelf,
                    onShelf = navigator::showShelf,
                    onOpen = ::open,
                )
            }
            entry<Destination.Album> { CollectionScreen(it, onOpen = ::open) }
            entry<Destination.Artist> { CollectionScreen(it, onOpen = ::open) }
            entry<Destination.Genre> { CollectionScreen(it, onOpen = ::open) }
            entry<Destination.Folder> { CollectionScreen(it, onOpen = ::open) }
            entry<Destination.Playlist> { CollectionScreen(it, onOpen = ::open) }
            entry<Destination.Settings> { SettingsScreen(cover = field(), onOpen = ::open) }
            entry<Destination.Sources> { SourcesPage(onOpen = ::open) }
            entry<Destination.SourceKinds> { SourceKindsPage(onOpen = ::open) }
            entry<Destination.SourceForm> {
                SourceFormPage(
                    form = it,
                    onCancel = ::back,
                    onDone = { navigator.backTo(Destination.Sources) },
                )
            }
            entry<Destination.Permissions> { PermissionsPage() }
            entry<Destination.LibraryFolders> { LibraryFoldersPage() }
            entry<Destination.Licenses> { LicensesPage() }
        }
}

@Composable
private fun rememberRoute(navigator: Navigator, app: AppViewModel, field: Color): Route {
    val currentField by rememberUpdatedState(field)
    return remember(navigator, app) {
        Route(navigator, app) { currentField }
    }
}
