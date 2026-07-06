package dev.jyotiraditya.dmt.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.TuiPanel
import dev.jyotiraditya.dmt.core.common.TuiTab
import dev.jyotiraditya.dmt.presentation.library.AlbumsPane
import dev.jyotiraditya.dmt.presentation.library.FilesPane
import dev.jyotiraditya.dmt.presentation.library.LibraryPane
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.presentation.player.ExpandedPlayer
import dev.jyotiraditya.dmt.presentation.player.InfoContent
import dev.jyotiraditya.dmt.presentation.player.MiniPlayer
import dev.jyotiraditya.dmt.presentation.player.QueueList
import dev.jyotiraditya.dmt.presentation.player.SheetHeader
import dev.jyotiraditya.dmt.presentation.player.TuiSheet
import dev.jyotiraditya.dmt.presentation.settings.SettingsPane
import dev.jyotiraditya.dmt.presentation.settings.StatsPane
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DmtScreen(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onRequestPermission: () -> Unit,
) {
    var showQueueSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val windowSize = LocalWindowInfo.current.containerSize
    val landscape = windowSize.width > windowSize.height
    val hideChrome = imeVisible && landscape

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.expanded, state.view, showQueueSheet, showInfoSheet) {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    LaunchedEffect(state.queue.isEmpty()) {
        if (state.queue.isEmpty()) showQueueSheet = false
    }

    val backHandled = state.expanded ||
        (state.view == DmtView.ALBUMS && state.openAlbum != null) ||
        (state.view == DmtView.FILES && state.openFolder != null) ||
        state.view != DmtView.LIBRARY
    BackHandler(enabled = backHandled) {
        when {
            state.expanded -> dispatch(DmtAction.Expand(false))

            state.view == DmtView.STATS -> dispatch(DmtAction.Show(DmtView.SETTINGS))

            state.view == DmtView.ALBUMS && state.openAlbum != null ->
                dispatch(DmtAction.OpenAlbum(null))

            state.view == DmtView.FILES && state.openFolder != null ->
                dispatch(DmtAction.OpenFolder(null))

            else -> dispatch(DmtAction.Show(DmtView.LIBRARY))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TuiBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
        ) {
            if (!hideChrome) {
                Titlebar(state, dispatch)
                TabsRow(state, dispatch)
            }

            Column(modifier = Modifier.weight(1f)) {
                when {
                    state.view == DmtView.STATS -> StatsPane(state, dispatch)
                    state.view == DmtView.SETTINGS -> SettingsPane(state, dispatch)
                    !state.hasPermission -> PermissionPane(dispatch, onRequestPermission)
                    state.scanning -> Caption(stringResource(R.string.scanning))
                    state.view == DmtView.LIBRARY -> LibraryPane(state, dispatch)
                    state.view == DmtView.ALBUMS -> AlbumsPane(state, dispatch)
                    else -> FilesPane(state, dispatch)
                }
            }

            NoticeLine(state)

            if (state.nowPlayingId != null && !imeVisible) {
                MiniPlayer(
                    state = state,
                    dispatch = dispatch,
                    onLongPress = { showQueueSheet = true },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        AnimatedVisibility(
            visible = state.expanded && state.nowPlayingId != null,
            enter = slideInVertically(tween(240)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(140)),
        ) {
            ExpandedPlayer(
                state = state,
                dispatch = dispatch,
                onInfo = { showInfoSheet = true },
                onQueue = { showQueueSheet = true },
            )
        }

        if (showQueueSheet) {
            TuiSheet(onDismiss = { showQueueSheet = false }) {
                val position = (state.queueIndex + 1).coerceAtMost(state.queue.size)
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
                SheetHeader(title = stringResource(R.string.track_info))
                InfoContent(state)
            }
        }
    }
}

@Composable
private fun Titlebar(state: DmtState, dispatch: (DmtAction) -> Unit) {
    TuiPanel(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(LocalAccent.current),
            )
            Text(
                text = " " + stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = TuiBright,
                modifier = Modifier.weight(1f),
            )
            val inConfig = state.view == DmtView.SETTINGS || state.view == DmtView.STATS
            TuiTab(
                label = stringResource(R.string.cfg),
                active = inConfig,
            ) {
                dispatch(
                    DmtAction.Show(if (inConfig) DmtView.LIBRARY else DmtView.SETTINGS),
                )
            }
        }
    }
}

@Composable
private fun TabsRow(state: DmtState, dispatch: (DmtAction) -> Unit) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        TuiTab(
            label = stringResource(R.string.tab_library),
            active = state.view == DmtView.LIBRARY,
        ) {
            dispatch(DmtAction.Show(DmtView.LIBRARY))
        }
        Spacer(modifier = Modifier.width(8.dp))
        TuiTab(
            label = stringResource(R.string.tab_albums),
            active = state.view == DmtView.ALBUMS,
        ) {
            dispatch(DmtAction.Show(DmtView.ALBUMS))
        }
        Spacer(modifier = Modifier.width(8.dp))
        TuiTab(
            label = stringResource(R.string.tab_files),
            active = state.view == DmtView.FILES,
        ) {
            dispatch(DmtAction.Show(DmtView.FILES))
        }
    }
}

@Composable
private fun NoticeLine(state: DmtState) {
    (state.error ?: state.notice)?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.labelSmall,
            color = if (state.error != null) MaterialTheme.colorScheme.error else TuiDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}
