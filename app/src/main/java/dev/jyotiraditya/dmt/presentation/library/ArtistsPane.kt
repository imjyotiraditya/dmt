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
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.SheetHeader
import dev.jyotiraditya.dmt.presentation.player.TuiSheet
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.util.asTime

@Composable
fun ArtistsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val artist: Artist? = state.artists.find { it.name == state.openArtist }

    if (artist == null) {
        ArtistList(state, dispatch)
    } else {
        ArtistDetail(artist, state, dispatch)
    }
}

@Composable
private fun ArtistList(state: DmtState, dispatch: (DmtAction) -> Unit) {
    if (state.artists.isEmpty()) {
        Caption(stringResource(R.string.no_artists))
        return
    }

    var sheetArtist by remember { mutableStateOf<Artist?>(null) }
    sheetArtist?.let { a ->
        TuiSheet(onDismiss = { sheetArtist = null }) {
            SheetHeader(title = a.name.lowercase())
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                TuiKey(label = "[ ${stringResource(R.string.action_play)} ]") {
                    dispatch(DmtAction.PlayAt(a.tracks, 0))
                    sheetArtist = null
                }
                TuiKey(label = "[ ${stringResource(R.string.action_queue)} ]") {
                    dispatch(DmtAction.Enqueue(a.tracks, a.name))
                    sheetArtist = null
                }
            }
        }
    }

    Column {
        SearchRow(
            query = state.query,
            hint = pluralStringResource(
                R.plurals.search_artists_hint,
                state.artists.size,
                state.artists.size,
            ),
            shown = state.filteredArtists.size,
            onQuery = { dispatch(DmtAction.Query(it)) },
        )
        if (state.filteredArtists.isEmpty()) {
            Caption(stringResource(R.string.no_match))
        }
        LazyColumn {
            itemsIndexed(state.filteredArtists, key = { _, a -> a.name }) { index, a ->
                ListRow(
                    index = index,
                    line1 = a.name,
                    line2 = "${a.albums} alb · ${a.tracks.size} trk".lowercase(),
                    current = false,
                    onClick = { dispatch(DmtAction.OpenArtist(a.name)) },
                    onLongClick = { sheetArtist = a },
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
private fun ArtistDetail(
    artist: Artist,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
) {
    LazyColumn {
        item {
            SubdirHeader(
                title = artist.name,
                meta = "${artist.albums} alb · ${artist.tracks.size} trk".lowercase(),
                onBack = { dispatch(DmtAction.OpenArtist(null)) },
            )
        }
        itemsIndexed(artist.tracks) { index, track ->
            ListRow(
                index = index,
                line1 = track.title,
                line2 = "${track.album} · ${track.durationMs.asTime()}".lowercase(),
                current = track.id.toString() == state.nowPlayingId,
                onClick = { dispatch(DmtAction.PlayAt(artist.tracks, index)) },
            )
        }
    }
}
