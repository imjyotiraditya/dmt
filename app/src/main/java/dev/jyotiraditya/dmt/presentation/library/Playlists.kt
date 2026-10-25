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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberWantsPlayback
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
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
internal fun LazyListScope.playlists(state: DmtState, dispatch: (DmtAction) -> Unit) {
    item(key = "new") { NewPlaylist(dispatch) }
    if (state.playlists.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.playlist_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
                modifier = Modifier.padding(horizontal = Margin, vertical = 12.dp),
            )
        }
    }
    items(state.playlists, key = { it.name }) { playlist ->
        PlaylistLine(playlist, dispatch)
    }
}

@Composable
private fun NewPlaylist(dispatch: (DmtAction) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateSheet(
            onCreate = { dispatch(DmtAction.CreatePlaylist(it)) },
            onDismiss = { creating = false },
        )
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
private fun PlaylistLine(playlist: Playlist, dispatch: (DmtAction) -> Unit) {
    var acting by remember { mutableStateOf(false) }
    if (acting) {
        ActionSheet(
            title = playlist.name,
            tracks = playlist.tracks,
            dispatch = dispatch,
            onDismiss = { acting = false },
            onDelete = { dispatch(DmtAction.DeletePlaylist(playlist.name)) },
        )
    }
    CollectionRow(
        title = playlist.name,
        detail = tracksSummary(playlist.tracks),
        onClick = { dispatch(DmtAction.OpenPlaylist(playlist.name)) },
        onLongClick = { acting = true },
    )
}

@Composable
internal fun PlaylistDetail(playlist: Playlist, state: DmtState, dispatch: (DmtAction) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        PickerSheet(playlist, state, dispatch) { picking = false }
    }
    val playing = rememberWantsPlayback()
    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item {
            DetailHead(
                title = playlist.name,
                meta = stringResource(R.string.collection_playlist),
                tracks = playlist.tracks,
                dispatch = dispatch,
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
                onClick = { dispatch(DmtAction.PlayAt(playlist.tracks, index)) },
                trailing = {
                    IconTap(
                        icon = DmtIcons.Close,
                        description = stringResource(R.string.playlist_remove),
                        iconSize = 18.dp,
                        onClick = {
                            dispatch(
                                DmtAction.RemoveFromPlaylist(
                                    playlist.name,
                                    track.path,
                                ),
                            )
                        },
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
    dispatch: (DmtAction) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    DmtSheet(onDismiss = onDismiss) {
        val act = { action: DmtAction ->
            dispatch(action)
            dismiss()
        }
        SheetHeader(title = title, meta = tracksSummary(tracks))
        if (tracks.isNotEmpty()) {
            SheetAction(
                DmtIcons.Play,
                stringResource(R.string.action_play),
                onClick = {
                    act(DmtAction.PlayAt(tracks, 0))
                },
            )
            SheetAction(
                DmtIcons.Shuffle,
                stringResource(R.string.player_shuffle),
                onClick = {
                    act(DmtAction.ShufflePlay(tracks))
                },
            )
            SheetAction(
                DmtIcons.PlayNext,
                stringResource(R.string.action_play_next),
                onClick = {
                    act(DmtAction.PlayNext(tracks, title))
                },
            )
            SheetAction(
                DmtIcons.AddToQueue,
                stringResource(R.string.action_add_to_queue),
                onClick = {
                    act(DmtAction.Enqueue(tracks, title))
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
    var name by remember { mutableStateOf("") }
    DmtSheet(onDismiss = onDismiss) {
        val submit = {
            if (name.isNotBlank()) {
                onCreate(name.trim())
                dismiss()
            }
        }
        SheetHeader(title = stringResource(R.string.playlist_new_title))
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Margin)
                    .background(Surface)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            decorationBox = { inner ->
                if (name.isEmpty()) {
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
            enabled = name.isNotBlank(),
            modifier = Modifier.padding(horizontal = Margin, vertical = 16.dp),
        )
    }
}

@Composable
private fun PickerSheet(
    playlist: Playlist,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidates =
        remember(playlist, state.tracks) {
            val inPlaylist = playlist.tracks.mapTo(HashSet()) { it.path }
            state.tracks.filter { it.path.isNotEmpty() && it.path !in inPlaylist }
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
                    onClick = { dispatch(DmtAction.AddToPlaylist(playlist.name, track)) },
                    trailing = {
                        IconTap(
                            icon = DmtIcons.Plus,
                            description = stringResource(R.string.playlist_add),
                            onClick = { dispatch(DmtAction.AddToPlaylist(playlist.name, track)) },
                        )
                    },
                )
            }
        }
    }
}
