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
import dev.jyotiraditya.dmt.data.Album
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

    Column {
        SearchRow(
            query = state.query,
            hint = stringResource(R.string.search_albums_hint, state.albums.size),
            shown = state.filteredAlbums.size,
            onQuery = { dispatch(DmtAction.Query(it)) }
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
                    onLongClick = { dispatch(DmtAction.Enqueue(a.tracks, a.name)) }
                )
            }
        }
    }
}

@Composable
private fun AlbumDetail(album: Album, state: DmtState, dispatch: (DmtAction) -> Unit) {
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
                    .tuiClickable { dispatch(DmtAction.OpenAlbum(null)) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
            Text(
                text = "${album.name} · ${album.artist}".lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TuiDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        itemsIndexed(album.tracks) { index, track ->
            ListRow(
                index = index,
                line1 = track.title,
                line2 = "${track.artist} · ${track.durationMs.asTime()}".lowercase(),
                current = track.id.toString() == state.nowPlayingId,
                onClick = { dispatch(DmtAction.PlayAt(album.tracks, index)) }
            )
        }
    }
}
