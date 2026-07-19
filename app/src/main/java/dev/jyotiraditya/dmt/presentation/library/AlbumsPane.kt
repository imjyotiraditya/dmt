package dev.jyotiraditya.dmt.presentation.library

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
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.SearchRow
import dev.jyotiraditya.dmt.core.common.SubdirHeader
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.SheetHeader
import dev.jyotiraditya.dmt.presentation.player.TuiSheet
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.util.asTime

@Composable
fun AlbumsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val album: Album? = state.albums.find { it.name == state.openAlbum }

    if (album == null) {
        AlbumList(state, dispatch)
    } else {
        AlbumDetail(album, state, dispatch)
    }
}

@Composable
private fun AlbumList(state: DmtState, dispatch: (DmtAction) -> Unit) {
    if (state.albums.isEmpty()) {
        Caption(stringResource(R.string.no_albums))
        return
    }

    var sheetAlbum by remember { mutableStateOf<Album?>(null) }
    sheetAlbum?.let { a ->
        TuiSheet(onDismiss = { sheetAlbum = null }) {
            SheetHeader(title = a.name.lowercase())
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                TuiKey(label = "[ ${stringResource(R.string.action_play)} ]") {
                    dispatch(DmtAction.PlayAt(a.tracks, 0))
                    sheetAlbum = null
                }
                TuiKey(label = "[ ${stringResource(R.string.action_queue)} ]") {
                    dispatch(DmtAction.Enqueue(a.tracks, a.name))
                    sheetAlbum = null
                }
            }
        }
    }

    Column {
        SearchRow(
            query = state.query,
            hint = pluralStringResource(
                R.plurals.search_albums_hint,
                state.albums.size,
                state.albums.size,
            ),
            shown = state.filteredAlbums.size,
            onQuery = { dispatch(DmtAction.Query(it)) },
        )
        if (state.filteredAlbums.isEmpty()) {
            Caption(stringResource(R.string.no_match))
        }
        LazyColumn {
            itemsIndexed(state.filteredAlbums, key = { _, a -> a.name }) { index, a ->
                ListRow(
                    index = index,
                    line1 = a.name,
                    line2 = "${a.artist} · ${a.tracks.size} trk".lowercase(),
                    current = false,
                    onClick = { dispatch(DmtAction.OpenAlbum(a.name)) },
                    onLongClick = { sheetAlbum = a },
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
private fun AlbumDetail(
    album: Album,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
) {
    LazyColumn {
        item {
            SubdirHeader(
                title = album.name,
                meta = "${album.artist} · ${album.tracks.size} trk".lowercase(),
                onBack = { dispatch(DmtAction.OpenAlbum(null)) },
            )
        }
        itemsIndexed(album.tracks) { index, track ->
            ListRow(
                index = index,
                line1 = track.title,
                line2 = "${track.artist} · ${track.durationMs.asTime()}".lowercase(),
                current = track.id.toString() == state.nowPlayingId,
                onClick = { dispatch(DmtAction.PlayAt(album.tracks, index)) },
            )
        }
    }
}
