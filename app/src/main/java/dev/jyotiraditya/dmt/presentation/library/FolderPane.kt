package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.ScrollMemory
import dev.jyotiraditya.dmt.core.common.SearchRow
import dev.jyotiraditya.dmt.core.common.SubdirHeader
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.domain.model.FolderNode
import dev.jyotiraditya.dmt.domain.model.allSongs
import dev.jyotiraditya.dmt.domain.model.flatten
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.SheetHeader
import dev.jyotiraditya.dmt.presentation.player.TuiSheet
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.util.asTime

/**
 * Browses the indexed music library by storage folder, one directory at a
 * time (Folders → Music → Coldplay → Parachutes → songs) instead of an
 * expandable tree. The tree itself is built once from indexed track paths
 * (see [dev.jyotiraditya.dmt.data.repository.FolderRepository]) — no folder
 * shown here is ever a raw filesystem entry.
 *
 * Browsing/search/sort state lives in [folderViewModel]; playback always
 * goes through the app's single, already-connected [dispatch] controller.
 */
@Composable
fun FoldersPane(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    folderViewModel: FolderViewModel,
) {
    val folderState by folderViewModel.state.collectAsState()

    if (folderState.tree.isEmpty()) {
        Caption(stringResource(R.string.no_files))
        return
    }

    ScrollMemory(folderState.currentPath ?: "list") {
        val openNode = folderState.openNode
        if (openNode == null) {
            FolderList(state, folderState, folderViewModel::onIntent, dispatch)
        } else {
            FolderDetail(openNode, state, folderState, folderViewModel::onIntent, dispatch)
        }
    }
}

@Composable
private fun FolderList(
    state: DmtState,
    folderState: FolderUiState,
    onFolderIntent: (FolderAction) -> Unit,
    dispatch: (DmtAction) -> Unit,
) {
    val searching = folderState.query.isNotBlank()
    val shown = remember(folderState.tree, folderState.matches, folderState.sort, searching) {
        val base = if (searching) folderState.matches else folderState.tree
        base.sortedWith(folderState.sort.comparator)
    }
    val totalFolders = remember(folderState.tree) { folderState.tree.flatten().size }

    var sheetItem by remember { mutableStateOf<FolderNode?>(null) }
    sheetItem?.let { node ->
        FolderActionsSheet(node, state, dispatch, onDismiss = { sheetItem = null })
    }

    Column {
        SearchRow(
            query = folderState.query,
            hint = pluralStringResource(
                R.plurals.search_folders_hint,
                totalFolders,
                totalFolders,
            ),
            shown = shown.size,
            onQuery = { onFolderIntent(FolderAction.Query(it)) },
            sort = folderState.sort.label,
            onSort = { onFolderIntent(FolderAction.Sort(folderState.sort.next())) },
        )
        if (shown.isEmpty()) {
            Caption(stringResource(R.string.no_match))
        }
        LazyColumn {
            itemsIndexed(shown, key = { _, node -> node.id }) { index, node ->
                ListRow(
                    index = index,
                    line1 = node.name,
                    line2 = node.listMeta(),
                    current = false,
                    onClick = { onFolderIntent(FolderAction.Open(node.absolutePath)) },
                    onLongClick = { sheetItem = node },
                    trailing = { OpenChevron() },
                )
            }
        }
    }
}

@Composable
private fun FolderDetail(
    node: FolderNode,
    state: DmtState,
    folderState: FolderUiState,
    onFolderIntent: (FolderAction) -> Unit,
    dispatch: (DmtAction) -> Unit,
) {
    val children = remember(node, folderState.sort) {
        node.children.sortedWith(folderState.sort.comparator)
    }
    val tracks = node.songs

    var sheetItem by remember { mutableStateOf<FolderNode?>(null) }
    sheetItem?.let { sheetNode ->
        FolderActionsSheet(sheetNode, state, dispatch, onDismiss = { sheetItem = null })
    }

    LazyColumn {
        item {
            SubdirHeader(
                title = node.name,
                meta = node.listMeta(),
                onBack = { onFolderIntent(FolderAction.Back) },
            )
        }
        itemsIndexed(children, key = { _, child -> child.id }) { index, child ->
            ListRow(
                index = index,
                line1 = child.name,
                line2 = child.listMeta(),
                current = false,
                onClick = { onFolderIntent(FolderAction.Open(child.absolutePath)) },
                onLongClick = { sheetItem = child },
                trailing = { OpenChevron() },
            )
        }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            ListRow(
                index = index,
                line1 = track.title,
                line2 = "${track.artist} · ${track.durationMs.asTime()}".lowercase(),
                current = track.id.toString() == state.nowPlayingId,
                onClick = { dispatch(DmtAction.PlayAt(tracks, index)) },
                onLongClick = { dispatch(DmtAction.Enqueue(listOf(track), track.title)) },
            )
        }
    }
}

/** Play / Shuffle / Play Next / Add to Queue / Add to Playlist for one folder (and its subfolders). */
@Composable
private fun FolderActionsSheet(
    node: FolderNode,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }

    if (showPlaylistPicker) {
        FolderPlaylistPicker(node, state, dispatch, onDismiss = onDismiss)
        return
    }

    TuiSheet(onDismiss = onDismiss) {
        SheetHeader(title = node.name.lowercase())
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                TuiKey(label = "[ ${stringResource(R.string.action_play)} ]") {
                    dispatch(DmtAction.PlayAt(node.allSongs(), 0))
                    onDismiss()
                }
                TuiKey(label = "[ ${stringResource(R.string.action_shuffle)} ]") {
                    dispatch(DmtAction.PlayAt(node.allSongs().shuffled(), 0))
                    onDismiss()
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                TuiKey(label = "[ ${stringResource(R.string.action_play_next)} ]") {
                    dispatch(DmtAction.PlayNext(node.allSongs(), node.name))
                    onDismiss()
                }
                TuiKey(label = "[ ${stringResource(R.string.action_queue)} ]") {
                    dispatch(DmtAction.Enqueue(node.allSongs(), node.name))
                    onDismiss()
                }
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                TuiKey(label = "[ ${stringResource(R.string.action_add_to_playlist)} ]") {
                    showPlaylistPicker = true
                }
            }
        }
    }
}

@Composable
private fun FolderPlaylistPicker(
    node: FolderNode,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onDismiss: () -> Unit,
) {
    TuiSheet(onDismiss = onDismiss) {
        SheetHeader(
            title = stringResource(R.string.folder_add_to_playlist_title),
            meta = node.name.lowercase(),
        )
        if (state.playlists.isEmpty()) {
            Caption(stringResource(R.string.no_match))
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
        ) {
            itemsIndexed(state.playlists, key = { _, playlist -> playlist.name }) { index, playlist ->
                ListRow(
                    index = index,
                    line1 = playlist.name,
                    line2 = "${playlist.tracks.size} trk",
                    current = false,
                    onClick = {
                        node.allSongs().forEach { track ->
                            dispatch(DmtAction.AddToPlaylist(playlist.name, track))
                        }
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun OpenChevron() {
    Text(
        text = stringResource(R.string.open_album),
        style = MaterialTheme.typography.labelMedium,
        color = TuiFaint,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

private fun FolderNode.listMeta(): String {
    val meta = "$songCount trk"
    val withDirs = if (childFolderCount > 0) "$meta · $childFolderCount dir" else meta
    return withDirs.lowercase()
}
