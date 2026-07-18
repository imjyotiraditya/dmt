package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.SearchRow
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.SheetHeader
import dev.jyotiraditya.dmt.presentation.player.TuiSheet
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface
import dev.jyotiraditya.dmt.util.asTime

@Composable
fun FoldersPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val folder: Folder? = state.folders.find { it.path == state.openFolder }

    if (folder == null) {
        FolderList(state, dispatch)
    } else {
        FolderDetail(folder, state, dispatch)
    }
}

@Composable
private fun FolderList(state: DmtState, dispatch: (DmtAction) -> Unit) {
    if (state.folders.isEmpty()) {
        Caption(stringResource(R.string.no_files))
        return
    }

    var sheetFolder by remember { mutableStateOf<Folder?>(null) }
    sheetFolder?.let { f ->
        TuiSheet(onDismiss = { sheetFolder = null }) {
            SheetHeader(title = f.name.lowercase())
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                TuiKey(label = "[ ${stringResource(R.string.action_play)} ]") {
                    dispatch(DmtAction.PlayAt(f.tracks, 0))
                    sheetFolder = null
                }
                TuiKey(label = "[ ${stringResource(R.string.action_queue)} ]") {
                    dispatch(DmtAction.Enqueue(f.tracks, f.name))
                    sheetFolder = null
                }
            }
        }
    }

    Column {
        SearchRow(
            query = state.query,
            hint = pluralStringResource(
                R.plurals.search_folders_hint,
                state.folders.size,
                state.folders.size,
            ),
            shown = state.filteredFolders.size,
            onQuery = { dispatch(DmtAction.Query(it)) },
        )
        if (state.filteredFolders.isEmpty()) {
            Caption(stringResource(R.string.no_match))
        }
        LazyColumn {
            itemsIndexed(state.filteredFolders, key = { _, f -> f.path }) { index, f ->
                ListRow(
                    index = index,
                    line1 = f.name,
                    line2 = "${f.tracks.size} trk".lowercase(),
                    current = false,
                    onClick = { dispatch(DmtAction.OpenFolder(f.path)) },
                    onLongClick = { sheetFolder = f },
                    trailing = {
                        Text(
                            text = stringResource(R.string.open_album),
                            style = MaterialTheme.typography.labelMedium,
                            color = TuiFaint,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FolderDetail(
    folder: Folder,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
) {
    LazyColumn {
        item {
            Text(
                text = stringResource(R.string.back),
                style = MaterialTheme.typography.labelMedium,
                color = TuiFg,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .border(1.dp, TuiLine)
                    .background(TuiSurface.copy(alpha = 0.6f))
                    .tuiClickable { dispatch(DmtAction.OpenFolder(null)) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
            Text(
                text = "${folder.name} · ${folder.tracks.size} trk".lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TuiDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }
        itemsIndexed(folder.tracks) { index, track ->
            ListRow(
                index = index,
                line1 = track.title,
                line2 = "${track.artist} · ${track.durationMs.asTime()}".lowercase(),
                current = track.id.toString() == state.nowPlayingId,
                onClick = { dispatch(DmtAction.PlayAt(folder.tracks, index)) },
            )
        }
    }
}
