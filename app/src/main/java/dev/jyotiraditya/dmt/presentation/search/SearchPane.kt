package dev.jyotiraditya.dmt.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.SearchRow
import dev.jyotiraditya.dmt.presentation.library.trackLine2
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.theme.TuiDim

private const val GROUP_LIMIT = 4

@Composable
fun SearchPane(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    Column {
        SearchRow(
            query = state.query,
            hint = stringResource(R.string.search_hint),
            shown = state.filtered.size,
            onQuery = { dispatch(DmtAction.Query(it)) },
        )
        if (state.query.isBlank()) {
            Caption(stringResource(R.string.search_empty))
            return@Column
        }

        val albums = state.filteredAlbums.take(GROUP_LIMIT)
        val artists = state.filteredArtists.take(GROUP_LIMIT)
        if (albums.isEmpty() && artists.isEmpty() && state.filtered.isEmpty()) {
            Caption(stringResource(R.string.no_match))
            return@Column
        }

        LazyColumn {
            if (albums.isNotEmpty()) {
                item { GroupLabel(stringResource(R.string.auto_albums)) }
                itemsIndexed(albums, key = { _, album -> "a:${album.name}" }) { index, album ->
                    ListRow(
                        index = index,
                        line1 = album.name,
                        line2 = "${album.artist} · ${album.tracks.size} trk".lowercase(),
                        current = false,
                        onClick = { onOpenAlbum(album.name) },
                    )
                }
            }
            if (artists.isNotEmpty()) {
                item { GroupLabel(stringResource(R.string.auto_artists)) }
                itemsIndexed(artists, key = { _, artist -> "r:${artist.name}" }) { index, artist ->
                    ListRow(
                        index = index,
                        line1 = artist.name,
                        line2 = "${artist.albums} alb · ${artist.tracks.size} trk",
                        current = false,
                        onClick = { onOpenArtist(artist.name) },
                    )
                }
            }
            if (state.filtered.isNotEmpty()) {
                item { GroupLabel(stringResource(R.string.auto_tracks)) }
                itemsIndexed(state.filtered, key = { _, track -> track.id }) { index, track ->
                    ListRow(
                        index = index,
                        line1 = track.title,
                        line2 = trackLine2(track),
                        current = track.id.toString() == state.nowPlayingId,
                        onClick = { dispatch(DmtAction.PlayAt(state.filtered, index)) },
                        onLongClick = { dispatch(DmtAction.Enqueue(listOf(track), track.title)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TuiDim,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}
