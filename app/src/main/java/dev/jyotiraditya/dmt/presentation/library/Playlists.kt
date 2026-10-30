package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.presentation.common.TrackListActions
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberWantsPlayback
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.CollectionRow
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.IconTap
import dev.jyotiraditya.dmt.ui.components.SheetAction
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.TrackRow
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface

/** The playlist shelf: a button to make one, then every saved playlist. */
internal fun LazyListScope.playlists(
    playlists: List<Playlist>,
    viewModel: LibraryViewModel,
    onOpen: (Destination) -> Unit,
) {
    item(key = "new") { NewPlaylist(onCreate = viewModel::createPlaylist) }
    if (playlists.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.playlist_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
                modifier = Modifier.padding(horizontal = Margin, vertical = 12.dp),
            )
        }
    }
    items(playlists, key = { it.name }) { playlist ->
        PlaylistLine(
            playlist = playlist,
            actions = viewModel,
            onOpen = { onOpen(Destination.Playlist(playlist.name)) },
            onDelete = { viewModel.deletePlaylist(playlist.name) },
        )
    }
}

@Composable
private fun NewPlaylist(onCreate: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateSheet(onCreate = onCreate, onDismiss = { creating = false })
    }
    Block(
        label = stringResource(R.string.playlist_new_title),
        icon = DmtIcons.Plus,
        fill = Ink,
        ink = Surface,
        onClick = { creating = true },
        modifier = Modifier.padding(horizontal = Margin, vertical = 8.dp),
    )
}

@Composable
private fun PlaylistLine(
    playlist: Playlist,
    actions: TrackListActions,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    var acting by remember { mutableStateOf(false) }
    if (acting) {
        ActionSheet(
            title = playlist.name,
            tracks = playlist.tracks,
            actions = actions,
            onDismiss = { acting = false },
            onDelete = onDelete,
        )
    }
    CollectionRow(
        title = playlist.name,
        detail = tracksSummary(playlist.tracks),
        onClick = onOpen,
        onLongClick = { acting = true },
    )
}

@Composable
internal fun PlaylistDetail(
    playlist: Playlist,
    state: CollectionUiState,
    viewModel: CollectionViewModel,
) {
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        PickerSheet(
            playlist = playlist,
            library = state.tracks,
            onAdd = { viewModel.addToPlaylist(playlist.name, it) },
            onDismiss = { picking = false },
        )
    }
    val playing = rememberWantsPlayback()
    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item {
            DetailHead(
                title = playlist.name,
                meta = stringResource(R.string.collection_playlist),
                tracks = playlist.tracks,
                actions = viewModel,
                action = { ink ->
                    TextLink(
                        label = stringResource(R.string.playlist_add),
                        color = ink,
                        onClick = { picking = true },
                    )
                },
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        itemsIndexed(playlist.tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track = track,
                meta = trackSummary(track, album = false),
                current = track.id.toString() == state.nowPlayingId,
                playing = playing,
                onClick = { viewModel.play(playlist.tracks, index) },
                trailing = {
                    IconTap(
                        icon = DmtIcons.Close,
                        description = stringResource(R.string.playlist_remove),
                        iconSize = 18.dp,
                        onClick = { viewModel.removeFromPlaylist(playlist.name, track) },
                    )
                },
            )
        }
    }
}

/** Long-press actions for a collection of tracks. */
@Composable
internal fun ActionSheet(
    title: String,
    tracks: List<Track>,
    actions: TrackListActions,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = title, meta = tracksSummary(tracks))
        if (tracks.isNotEmpty()) {
            SheetAction(
                DmtIcons.Play,
                stringResource(R.string.action_play),
                onClick = {
                    actions.play(tracks, 0)
                    dismiss()
                },
            )
            SheetAction(
                DmtIcons.Shuffle,
                stringResource(R.string.player_shuffle),
                onClick = {
                    actions.shuffle(tracks)
                    dismiss()
                },
            )
            SheetAction(
                DmtIcons.PlayNext,
                stringResource(R.string.action_play_next),
                onClick = {
                    actions.playNext(tracks, title)
                    dismiss()
                },
            )
            SheetAction(
                DmtIcons.AddToQueue,
                stringResource(R.string.action_add_to_queue),
                onClick = {
                    actions.enqueue(tracks, title)
                    dismiss()
                },
            )
        }
        if (onDelete != null) {
            SheetAction(
                DmtIcons.Trash,
                stringResource(R.string.action_delete),
                onClick = {
                    onDelete()
                    dismiss()
                },
            )
        }
    }
}

@Composable
private fun CreateSheet(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    val name = rememberTextFieldState()
    DmtSheet(onDismiss = onDismiss) {
        val submit = {
            if (name.text.isNotBlank()) {
                onCreate(name.text.trim().toString())
                dismiss()
            }
        }
        SheetHeader(title = stringResource(R.string.playlist_new_title))
        BasicTextField(
            state = name,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = { submit() },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Margin)
                    .background(Surface)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            decorator = { inner ->
                if (name.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.playlist_name_hint),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Muted,
                    )
                }
                inner()
            },
        )
        Block(
            label = stringResource(R.string.playlist_create),
            onClick = submit,
            enabled = name.text.isNotBlank(),
            modifier = Modifier.padding(horizontal = Margin, vertical = 16.dp),
        )
    }
}

/** Every library track the playlist lacks, each added with a tap. */
@Composable
private fun PickerSheet(
    playlist: Playlist,
    library: List<Track>,
    onAdd: (Track) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidates =
        remember(playlist, library) {
            val inPlaylist = playlist.tracks.mapTo(HashSet()) { it.path }
            library.filter { it.path.isNotEmpty() && it.path !in inPlaylist }
        }
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(
            title = stringResource(R.string.playlist_add_tracks_title),
            meta = playlist.name,
        )
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            items(candidates, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    meta = trackSummary(track, album = false),
                    current = false,
                    playing = false,
                    onClick = { onAdd(track) },
                    trailing = {
                        IconTap(
                            icon = DmtIcons.Plus,
                            description = stringResource(R.string.playlist_add),
                            onClick = { onAdd(track) },
                        )
                    },
                )
            }
        }
    }
}
