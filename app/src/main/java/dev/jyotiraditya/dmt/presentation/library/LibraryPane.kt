package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.SearchRow
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.util.asTime

@Composable
fun LibraryPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    if (state.tracks.isEmpty()) {
        Caption(stringResource(R.string.no_audio, state.settings.sourceMode.label))
        return
    }

    Column {
        SearchRow(
            query = state.query,
            hint = pluralStringResource(
                R.plurals.search_tracks_hint,
                state.tracks.size,
                state.tracks.size,
            ),
            shown = state.filtered.size,
            onQuery = { dispatch(DmtAction.Query(it)) },
            sort = state.settings.librarySort.label,
            onSort = {
                dispatch(
                    DmtAction.Config(
                        state.settings.copy(
                            librarySort = state.settings.librarySort.next(
                                state.settings.sourceMode,
                            ),
                        ),
                    ),
                )
            },
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
                    onLongClick = { dispatch(DmtAction.Enqueue(listOf(track), track.title)) },
                )
            }
        }
    }
}
