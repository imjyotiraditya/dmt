package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.ListRow
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.util.asTime

@Composable
fun LibraryPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    if (state.tracks.isEmpty()) {
        Caption(stringResource(R.string.no_audio, state.settings.sourceMode.label))
        return
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val count = pluralStringResource(
                R.plurals.track_count,
                state.tracks.size,
                state.tracks.size,
            )
            Caption("$count · ${totalTime(state.tracks)}")
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "[${state.settings.librarySort.label}]",
                style = MaterialTheme.typography.labelLarge,
                color = TuiDim,
                modifier = Modifier
                    .tuiClickable {
                        dispatch(
                            DmtAction.Config(
                                state.settings.copy(
                                    librarySort = state.settings.librarySort.next(
                                        state.settings.sourceMode,
                                    ),
                                ),
                            ),
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        LazyColumn {
            itemsIndexed(state.filtered, key = { _, track -> track.id }) { index, track ->
                ListRow(
                    index = index,
                    line1 = track.title,
                    line2 = trackLine2(track),
                    current = track.id.toString() == state.nowPlayingId,
                    onClick = { dispatch(DmtAction.PlayAt(state.filtered, index)) },
                    onLongClick = { dispatch(DmtAction.Enqueue(listOf(track), track.title)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

fun trackLine2(
    track: Track,
    artist: Boolean = true,
    album: Boolean = true,
): String =
    listOfNotNull(
        track.artist.asCredit().takeIf { artist },
        track.album.takeIf { album },
        track.durationMs.asTime(),
    )
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .lowercase()

fun artistLine2(artist: Artist): String =
    "${artist.albums} alb · ${artist.tracks.size} trk"

fun totalTime(tracks: List<Track>): String {
    val minutes = tracks.sumOf { it.durationMs } / 60_000
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
