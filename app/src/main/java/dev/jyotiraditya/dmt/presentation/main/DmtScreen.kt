package dev.jyotiraditya.dmt.presentation.main

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.FitScaled
import dev.jyotiraditya.dmt.core.common.ScrollMemory
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.TuiNotice
import dev.jyotiraditya.dmt.core.common.TuiTab
import dev.jyotiraditya.dmt.core.common.fitScaleFor
import dev.jyotiraditya.dmt.core.common.isLandscapeWindow
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.home.HomePane
import dev.jyotiraditya.dmt.presentation.library.AlbumsPane
import dev.jyotiraditya.dmt.presentation.library.ArtistsPane
import dev.jyotiraditya.dmt.presentation.library.FoldersPane
import dev.jyotiraditya.dmt.presentation.library.LibraryPane
import dev.jyotiraditya.dmt.presentation.library.PlaylistsPane
import dev.jyotiraditya.dmt.presentation.player.ChainContent
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.presentation.player.InfoContent
import dev.jyotiraditya.dmt.presentation.player.MiniPlayer
import dev.jyotiraditya.dmt.presentation.player.PlayerSheet
import dev.jyotiraditya.dmt.presentation.player.QueueList
import dev.jyotiraditya.dmt.presentation.player.SheetHeader
import dev.jyotiraditya.dmt.presentation.player.TuiSheet
import dev.jyotiraditya.dmt.presentation.search.SearchPane
import dev.jyotiraditya.dmt.presentation.settings.BlocklistPane
import dev.jyotiraditya.dmt.presentation.settings.PermissionsPane
import dev.jyotiraditya.dmt.presentation.settings.SettingsPane
import dev.jyotiraditya.dmt.presentation.settings.SourceLoginPane
import dev.jyotiraditya.dmt.presentation.settings.SourcesPane
import dev.jyotiraditya.dmt.presentation.settings.StatsPane
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiLine

private const val ROUTE_HOME = "home"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_SOURCES = "sources"
private const val ROUTE_CFG = "cfg"

private val LIBRARY_VIEWS = setOf(
    DmtView.LIBRARY,
    DmtView.ALBUMS,
    DmtView.ARTISTS,
    DmtView.FOLDERS,
    DmtView.PLAYLISTS,
)
private val SOURCE_VIEWS = setOf(DmtView.SOURCES, DmtView.SOURCE_LOGIN, DmtView.PERMISSIONS)
private val SOURCE_SUBVIEWS = setOf(DmtView.SOURCE_LOGIN, DmtView.PERMISSIONS)
private val CFG_SUBVIEWS = setOf(DmtView.STATS, DmtView.BLOCKLIST, DmtView.PERMISSIONS)
private val CFG_VIEWS = setOf(
    DmtView.SETTINGS,
    DmtView.STATS,
    DmtView.BLOCKLIST,
    DmtView.PERMISSIONS,
)

private fun backStep(route: String, state: DmtState): DmtAction? =
    when (route) {
        ROUTE_LIBRARY -> when (state.view) {
            DmtView.ALBUMS -> state.openAlbum?.let { DmtAction.OpenAlbum(null) }
            DmtView.ARTISTS -> state.openArtist?.let { DmtAction.OpenArtist(null) }
            DmtView.FOLDERS -> state.openFolder?.let { DmtAction.OpenFolder(null) }
            DmtView.PLAYLISTS -> state.openPlaylist?.let { DmtAction.OpenPlaylist(null) }
            else -> null
        }

        ROUTE_SOURCES ->
            DmtAction.Show(DmtView.SOURCES).takeIf { state.view in SOURCE_SUBVIEWS }

        ROUTE_CFG ->
            DmtAction.Show(DmtView.SETTINGS).takeIf { state.view in CFG_SUBVIEWS }

        else -> null
    }

private data class NavItem(val labelRes: Int, val route: String, val view: DmtView?)

private val NAV_ITEMS = listOf(
    NavItem(R.string.nav_home, ROUTE_HOME, null),
    NavItem(R.string.nav_library, ROUTE_LIBRARY, DmtView.LIBRARY),
    NavItem(R.string.nav_search, ROUTE_SEARCH, null),
    NavItem(R.string.nav_sources, ROUTE_SOURCES, DmtView.SOURCES),
    NavItem(R.string.nav_cfg, ROUTE_CFG, DmtView.SETTINGS),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DmtScreen(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    art: suspend (Track) -> Bitmap,
) {
    var showQueueSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var miniAnchor by remember { mutableStateOf<Rect?>(null) }
    val imeVisible = WindowInsets.isImeVisible
    val landscape = isLandscapeWindow()

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: ROUTE_HOME

    val sheetFraction = remember { Animatable(0f) }
    LaunchedEffect(state.nowPlayingId == null) {
        if (state.nowPlayingId == null) sheetFraction.snapTo(0f)
    }

    fun navTo(item: NavItem) {
        navController.navigate(item.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        item.view?.let { dispatch(DmtAction.Show(it)) }
    }

    val viewNow = rememberUpdatedState(state.view)
    LaunchedEffect(route) {
        if (route != ROUTE_SEARCH) dispatch(DmtAction.Query(""))
        when (route) {
            ROUTE_LIBRARY ->
                if (viewNow.value !in LIBRARY_VIEWS) dispatch(DmtAction.Show(DmtView.LIBRARY))

            ROUTE_SOURCES ->
                if (viewNow.value !in SOURCE_VIEWS) dispatch(DmtAction.Show(DmtView.SOURCES))

            ROUTE_CFG ->
                if (viewNow.value !in CFG_VIEWS) dispatch(DmtAction.Show(DmtView.SETTINGS))
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.expanded, route, state.view, showQueueSheet, showInfoSheet) {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    LaunchedEffect(state.queue.isEmpty()) {
        if (state.queue.isEmpty()) showQueueSheet = false
    }

    val backAction = if (state.expanded) null else backStep(route, state)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TuiBg),
    ) {
        if (landscape) {
            FitScaled(fitScaleFor(designHeightDp = 400f, minScale = 0.85f)) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 16.dp),
                ) {
                    SideRail(route, ::navTo)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        PaneNavHost(
                            navController = navController,
                            state = state,
                            dispatch = dispatch,
                            art = art,
                            navTo = ::navTo,
                            modifier = Modifier.weight(1f),
                        )

                        TuiNotice(error = state.error, notice = state.notice)

                        if (state.nowPlayingId != null && !imeVisible) {
                            MiniPlayerAnchor(state) { miniAnchor = it }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp),
            ) {
                Titlebar(label = stringResource(pageLabel(route)))
                PaneNavHost(
                    navController = navController,
                    state = state,
                    dispatch = dispatch,
                    art = art,
                    navTo = ::navTo,
                    modifier = Modifier.weight(1f),
                )

                TuiNotice(error = state.error, notice = state.notice)

                if (state.nowPlayingId != null && !imeVisible) {
                    MiniPlayerAnchor(state) { miniAnchor = it }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                if (!imeVisible) {
                    BottomNav(
                        route = route,
                        fraction = { sheetFraction.value },
                        onNav = ::navTo,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        BackHandler(enabled = backAction != null) {
            backAction?.let(dispatch)
        }

        PlayerSheet(
            state = state,
            dispatch = dispatch,
            anchor = miniAnchor,
            hidden = imeVisible && !state.expanded,
            fraction = sheetFraction,
            onInfo = { showInfoSheet = true },
            onQueue = { showQueueSheet = true },
        )

        if (showQueueSheet) {
            TuiSheet(onDismiss = { showQueueSheet = false }) {
                val position = (state.queuePosition + 1).coerceAtLeast(1)
                SheetHeader(
                    title = stringResource(R.string.queue_title),
                    meta = "$position/${state.queue.size}",
                )
                QueueList(
                    state = state,
                    dispatch = dispatch,
                    modifier = Modifier.heightIn(max = 420.dp),
                )
            }
        }

        if (showInfoSheet) {
            TuiSheet(onDismiss = { showInfoSheet = false }) {
                var showChain by remember { mutableStateOf(false) }
                SheetHeader(title = stringResource(R.string.track_info)) {
                    TuiTab(
                        label = stringResource(R.string.tab_info),
                        active = !showChain,
                    ) {
                        showChain = false
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TuiTab(
                        label = stringResource(R.string.tab_chain),
                        active = showChain,
                    ) {
                        showChain = true
                    }
                }
                if (showChain) ChainContent(state) else InfoContent(state)
            }
        }
    }
}

@Composable
private fun PaneNavHost(
    navController: NavHostController,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    art: suspend (Track) -> Bitmap,
    navTo: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun openLibrary(view: DmtView) {
        navTo(NavItem(R.string.nav_library, ROUTE_LIBRARY, view))
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        modifier = modifier,
    ) {
        composable(ROUTE_HOME) {
            ScrollMemory(ROUTE_HOME) {
                HomePane(
                    state = state,
                    dispatch = dispatch,
                    art = art,
                    onOpenAlbum = { name ->
                        openLibrary(DmtView.ALBUMS)
                        dispatch(DmtAction.OpenAlbum(name))
                    },
                    onOpenAlbums = { openLibrary(DmtView.ALBUMS) },
                    onOpenTracks = { openLibrary(DmtView.LIBRARY) },
                    onOpenArtist = { name ->
                        openLibrary(DmtView.ARTISTS)
                        dispatch(DmtAction.OpenArtist(name))
                    },
                    onOpenArtists = { openLibrary(DmtView.ARTISTS) },
                )
            }
        }
        composable(ROUTE_LIBRARY) {
            Column {
                TabsRow(state, dispatch)
                SectionPane(
                    state = state,
                    dispatch = dispatch,
                    allowed = LIBRARY_VIEWS,
                    fallback = DmtView.LIBRARY,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        composable(ROUTE_SEARCH) {
            SearchPane(
                state = state,
                dispatch = dispatch,
                onOpenAlbum = { name ->
                    openLibrary(DmtView.ALBUMS)
                    dispatch(DmtAction.OpenAlbum(name))
                },
                onOpenArtist = { name ->
                    openLibrary(DmtView.ARTISTS)
                    dispatch(DmtAction.OpenArtist(name))
                },
            )
        }
        composable(ROUTE_SOURCES) {
            SectionPane(
                state = state,
                dispatch = dispatch,
                allowed = SOURCE_VIEWS,
                fallback = DmtView.SOURCES,
            )
        }
        composable(ROUTE_CFG) {
            SectionPane(
                state = state,
                dispatch = dispatch,
                allowed = CFG_VIEWS,
                fallback = DmtView.SETTINGS,
            )
        }
    }
}

@Composable
private fun SectionPane(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    allowed: Set<DmtView>,
    fallback: DmtView,
    modifier: Modifier = Modifier,
) {
    val view = if (state.view in allowed) state.view else fallback
    Column(modifier = modifier) {
        ScrollMemory(view.name) {
            when {
                view == DmtView.STATS -> StatsPane(state, dispatch)
                view == DmtView.BLOCKLIST -> BlocklistPane(state, dispatch)
                view == DmtView.PERMISSIONS -> PermissionsPane(state, dispatch)
                view == DmtView.SETTINGS -> SettingsPane(state, dispatch)
                view == DmtView.SOURCES -> SourcesPane(state, dispatch)
                view == DmtView.SOURCE_LOGIN -> SourceLoginPane(state.loginSource, dispatch)
                state.scanning -> Caption(stringResource(R.string.scanning))
                view == DmtView.ALBUMS -> AlbumsPane(state, dispatch)
                view == DmtView.ARTISTS -> ArtistsPane(state, dispatch)
                view == DmtView.FOLDERS -> FoldersPane(state, dispatch)
                view == DmtView.PLAYLISTS -> PlaylistsPane(state, dispatch)
                else -> LibraryPane(state, dispatch)
            }
        }
    }
}

@Composable
private fun BottomNav(
    route: String,
    fraction: () -> Float,
    onNav: (NavItem) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val f = fraction().coerceIn(0f, 1f)
                translationY = (size.height + 14.dp.toPx()) * f
                alpha = 1f - f
            },
    ) {
        NAV_ITEMS.forEach { item ->
            TuiKey(
                label = stringResource(item.labelRes),
                accent = route == item.route,
                big = true,
                fill = true,
                modifier = Modifier.weight(1f),
            ) {
                onNav(item)
            }
        }
    }
}

@Composable
private fun SideRail(route: String, onNav: (NavItem) -> Unit) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .fillMaxHeight(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(TuiAccent),
            )
            Text(
                text = " " + stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = TuiBright,
            )
        }
        HorizontalDivider(color = TuiLine, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(12.dp))
        NAV_ITEMS.forEachIndexed { index, item ->
            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
            TuiTab(
                label = stringResource(item.labelRes),
                active = route == item.route,
                modifier = Modifier.fillMaxWidth(),
            ) {
                onNav(item)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MiniPlayerAnchor(
    state: DmtState,
    onAnchor: (Rect) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onAnchor(it.boundsInRoot()) }
            .alpha(0f)
            .clearAndSetSemantics {},
    ) {
        MiniPlayer(state = state, dispatch = {})
    }
}

private fun pageLabel(route: String): Int =
    when (route) {
        ROUTE_LIBRARY -> R.string.page_library
        ROUTE_SEARCH -> R.string.page_search
        ROUTE_SOURCES -> R.string.page_sources
        ROUTE_CFG -> R.string.page_cfg
        else -> R.string.page_home
    }

@Composable
private fun Titlebar(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(TuiAccent),
        )
        Text(
            text = " " + stringResource(R.string.app_name) + " ",
            style = MaterialTheme.typography.titleMedium,
            color = TuiBright,
        )
        HorizontalDivider(color = TuiLine, modifier = Modifier.weight(1f))
        Text(
            text = " $label",
            style = MaterialTheme.typography.titleMedium,
            color = TuiBright,
        )
    }
}

@Composable
private fun libraryTabs(state: DmtState): List<Pair<String, DmtView>> =
    buildList {
        add(stringResource(R.string.tab_library) to DmtView.LIBRARY)
        add(stringResource(R.string.tab_albums) to DmtView.ALBUMS)
        add(stringResource(R.string.tab_artists) to DmtView.ARTISTS)
        if (state.folders.isNotEmpty()) {
            add(stringResource(R.string.tab_folders) to DmtView.FOLDERS)
            add(stringResource(R.string.tab_playlists) to DmtView.PLAYLISTS)
        }
    }

@Composable
private fun TabsRow(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val tabs = libraryTabs(state)
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(state.view) { requester.bringIntoView() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        tabs.forEach { (label, view) ->
            val active = state.view == view
            TuiTab(
                label = label,
                active = active,
                modifier = if (active) {
                    Modifier.bringIntoViewRequester(requester)
                } else {
                    Modifier
                },
            ) {
                dispatch(DmtAction.Show(view))
            }
        }
    }
}
