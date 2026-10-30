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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.model.Track
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
    navigation: TrackMenuNavigation,
    viewModel: TrackMenuViewModel = hiltViewModel(),
): (Track) -> Unit {
    var shown by remember { mutableStateOf<Track?>(null) }
    shown?.let { track ->
        TrackSheet(track, viewModel, navigation, onDismiss = { shown = null })
    }
    return remember { { track -> shown = track } }
}

@Composable
private fun TrackSheet(
    track: Track,
    viewModel: TrackMenuViewModel,
    navigation: TrackMenuNavigation,
    onDismiss: () -> Unit,
) {
    var pickingPlaylist by remember { mutableStateOf(false) }
    if (pickingPlaylist) {
        PlaylistPickSheet(track, viewModel, onDismiss = onDismiss)
        return
    }
    val album = viewModel.albumOf(track)
    val artist = viewModel.artistOf(track)

    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = track.title, meta = trackSummary(track))
        SheetAction(
            DmtIcons.PlayNext,
            stringResource(R.string.action_play_next),
            onClick = {
                viewModel.playNext(track)
                dismiss()
            },
        )
        SheetAction(
            DmtIcons.AddToQueue,
            stringResource(R.string.action_add_to_queue),
            onClick = {
                viewModel.enqueue(track)
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
private fun PlaylistPickSheet(track: Track, viewModel: TrackMenuViewModel, onDismiss: () -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = stringResource(R.string.playlist_pick_title), meta = track.title)
        if (playlists.isEmpty()) {
            Text(
                text = stringResource(R.string.playlist_pick_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Muted,
                modifier = Modifier.padding(horizontal = Margin, vertical = 12.dp),
            )
        }
        playlists.forEach { playlist ->
            val contains = playlist.tracks.any { it.path == track.path }
            SheetAction(
                icon = if (contains) DmtIcons.Check else DmtIcons.Playlist,
                label = playlist.name,
                enabled = !contains,
                onClick = {
                    viewModel.addToPlaylist(playlist.name, track)
                    dismiss()
                },
            )
        }
    }
}
