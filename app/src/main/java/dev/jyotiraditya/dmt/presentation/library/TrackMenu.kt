package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.SheetAction
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted

/** Where a track menu can take the user: an album by its id, an artist by name. */
data class TrackMenuNavigation(val openAlbum: (String) -> Unit, val openArtist: (String) -> Unit)

/** Hosts the long-press menu for single tracks and returns the function that opens it. */
@Composable
fun rememberTrackMenu(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    navigation: TrackMenuNavigation,
): (Track) -> Unit {
    var shown by remember { mutableStateOf<Track?>(null) }
    shown?.let { track ->
        TrackSheet(track, state, dispatch, navigation, onDismiss = { shown = null })
    }
    return remember { { track -> shown = track } }
}

@Composable
private fun TrackSheet(
    track: Track,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    navigation: TrackMenuNavigation,
    onDismiss: () -> Unit,
) {
    var pickingPlaylist by remember { mutableStateOf(false) }
    if (pickingPlaylist) {
        PlaylistPickSheet(track, state, dispatch, onDismiss = onDismiss)
        return
    }
    val album = state.library.albumOf(track)
    val artist = state.library.artistOf(track)

    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = track.title, meta = trackSummary(track))
        SheetAction(
            DmtIcons.PlayNext,
            stringResource(R.string.action_play_next),
            onClick = {
                dispatch(DmtAction.PlayNext(listOf(track), track.title))
                dismiss()
            },
        )
        SheetAction(
            DmtIcons.AddToQueue,
            stringResource(R.string.action_add_to_queue),
            onClick = {
                dispatch(DmtAction.Enqueue(listOf(track), track.title))
                dismiss()
            },
        )
        // Playlists are files on the device and can only hold local tracks.
        if (track.path.isNotEmpty()) {
            SheetAction(
                DmtIcons.Playlist,
                stringResource(R.string.action_add_to_playlist),
                onClick = { pickingPlaylist = true },
            )
        }
        album?.let {
            SheetAction(
                DmtIcons.Disc,
                stringResource(R.string.action_go_to_album),
                onClick = {
                    navigation.openAlbum(it.id)
                    dismiss()
                },
            )
        }
        artist?.let {
            SheetAction(
                DmtIcons.Person,
                stringResource(R.string.action_go_to_artist),
                onClick = {
                    navigation.openArtist(it.name)
                    dismiss()
                },
            )
        }
    }
}

@Composable
private fun PlaylistPickSheet(
    track: Track,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onDismiss: () -> Unit,
) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = stringResource(R.string.playlist_pick_title), meta = track.title)
        if (state.playlists.isEmpty()) {
            Text(
                text = stringResource(R.string.playlist_pick_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                modifier = Modifier.padding(horizontal = Margin, vertical = 12.dp),
            )
        }
        state.playlists.forEach { playlist ->
            val contains = playlist.tracks.any { it.path == track.path }
            SheetAction(
                icon = if (contains) DmtIcons.Check else DmtIcons.Playlist,
                label = playlist.name,
                enabled = !contains,
                onClick = {
                    dispatch(DmtAction.AddToPlaylist(playlist.name, track))
                    dismiss()
                },
            )
        }
    }
}

/** Track menu navigation that opens albums and artists in the library. */
fun libraryNavigation(dispatch: (DmtAction) -> Unit) =
    TrackMenuNavigation(openAlbum = libraryAlbum(dispatch), openArtist = libraryArtist(dispatch))

private fun libraryAlbum(dispatch: (DmtAction) -> Unit): (String) -> Unit = { name ->
    dispatch(DmtAction.Query(""))
    dispatch(DmtAction.Show(DmtView.ALBUMS))
    dispatch(DmtAction.OpenAlbum(name))
}

private fun libraryArtist(dispatch: (DmtAction) -> Unit): (String) -> Unit = { name ->
    dispatch(DmtAction.Query(""))
    dispatch(DmtAction.Show(DmtView.ARTISTS))
    dispatch(DmtAction.OpenAlbum(null))
    dispatch(DmtAction.OpenArtist(name))
}
