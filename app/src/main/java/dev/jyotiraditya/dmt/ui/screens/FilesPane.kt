package dev.jyotiraditya.dmt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.data.Folder
import dev.jyotiraditya.dmt.player.asTime
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtState
import dev.jyotiraditya.dmt.ui.components.Caption
import dev.jyotiraditya.dmt.ui.components.ListRow
import dev.jyotiraditya.dmt.ui.components.SearchRow
import dev.jyotiraditya.dmt.ui.components.tuiClickable
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface

@Composable
fun FilesPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
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

    Column {
        SearchRow(
            query = state.query,
            hint = stringResource(R.string.search_folders_hint, state.folders.size),
            shown = state.filteredFolders.size,
            onQuery = { dispatch(DmtAction.Query(it)) }
        )
        if (state.filteredFolders.isEmpty()) {
            Caption(stringResource(R.string.no_match))
        }
        LazyColumn {
            itemsIndexed(state.filteredFolders, key = { _, f -> f.path }) { index, f ->
                ListRow(
                    index = index,
                    line1 = f.name,
                    line2 = "${f.tracks.size} trk",
                    current = false,
                    onClick = { dispatch(DmtAction.OpenFolder(f.path)) },
                    onLongClick = { dispatch(DmtAction.Enqueue(f.tracks, f.name)) }
                )
            }
        }
    }
}

@Composable
private fun FolderDetail(folder: Folder, state: DmtState, dispatch: (DmtAction) -> Unit) {
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
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
            Text(
                text = folder.name.lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TuiDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        itemsIndexed(folder.tracks) { index, track ->
            ListRow(
                index = index,
                line1 = track.title,
                line2 = "${track.artist} · ${track.durationMs.asTime()}".lowercase(),
                current = track.id.toString() == state.nowPlayingId,
                onClick = { dispatch(DmtAction.PlayAt(folder.tracks, index)) }
            )
        }
    }
}
