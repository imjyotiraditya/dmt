package dev.jyotiraditya.dmt.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.player.asTime
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtState
import dev.jyotiraditya.dmt.ui.components.Caption
import dev.jyotiraditya.dmt.ui.components.ListRow
import dev.jyotiraditya.dmt.ui.components.SearchRow
import dev.jyotiraditya.dmt.ui.components.TuiKey

@Composable
fun LibraryPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    if (state.tracks.isEmpty()) {
        Caption(stringResource(R.string.no_audio))
        TuiKey(stringResource(R.string.rescan)) { dispatch(DmtAction.Rescan) }
        return
    }

    Column {
        SearchRow(
            query = state.query,
            hint = stringResource(R.string.search_tracks_hint, state.tracks.size),
            shown = state.filtered.size,
            onQuery = { dispatch(DmtAction.Query(it)) }
        )
        if (state.filtered.isEmpty()) {
            Caption(stringResource(R.string.no_match))
        }
        LazyColumn {
            itemsIndexed(state.filtered, key = { _, track -> track.id }) { index, track ->
                ListRow(
                    index = index,
                    line1 = track.title,
                    line2 = "${track.artist} · ${track.durationMs.asTime()}".lowercase(),
                    current = track.id.toString() == state.nowPlayingId,
                    onClick = { dispatch(DmtAction.PlayAt(state.filtered, index)) },
                    onLongClick = { dispatch(DmtAction.Enqueue(listOf(track), track.title)) }
                )
            }
        }
    }
}
