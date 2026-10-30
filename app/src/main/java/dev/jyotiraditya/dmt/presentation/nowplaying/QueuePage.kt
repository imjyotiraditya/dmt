package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberShuffleButtonState
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.QueueEntry
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.ui.components.Artwork
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.EmptyState
import dev.jyotiraditya.dmt.ui.components.SheetAction
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface2

private val RowArt = 44.dp

/** What plays next: tap to play, drag the handle to reorder, long press to remove. */
@OptIn(UnstableApi::class)
@Composable
internal fun QueuePage(state: NowPlayingUiState, player: Player, actions: NowPlayingActions) {
    val shuffled = rememberShuffleButtonState(player).shuffleOn
    val position = state.queuePosition.coerceAtLeast(0)
    val current = state.queue.getOrNull(position)
    val upcoming = remember(state.queue, position) { state.queue.drop(position + 1) }

    if (upcoming.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.queue_empty_title),
            body = stringResource(R.string.queue_empty_body),
            modifier = Modifier.fillMaxSize().padding(horizontal = Margin),
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = Margin, end = Margin, top = 8.dp, bottom = 4.dp),
        ) {
            Text(
                text = pluralStringResource(R.plurals.queue_up_next, upcoming.size, upcoming.size),
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                modifier = Modifier.weight(1f),
            )
            TextLink(
                label = stringResource(R.string.queue_clear),
                onClick = actions::clearUpcoming,
            )
        }
        if (shuffled && upcoming.size > 1) {
            Text(
                text = stringResource(R.string.queue_shuffled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(horizontal = Margin, vertical = 4.dp),
            )
        }
        UpcomingList(
            upcoming = upcoming,
            library = state.library,
            firstUpcomingIndex = (current?.index ?: -1) + 1,
            reorderable = !shuffled,
            actions = actions,
        )
    }
}

/** The upcoming tracks, reordered locally while dragged and sent to the player on release. */
@Composable
private fun UpcomingList(
    upcoming: List<QueueEntry>,
    library: LibrarySnapshot,
    firstUpcomingIndex: Int,
    reorderable: Boolean,
    actions: NowPlayingActions,
) {
    val listState = rememberLazyListState()
    val order = remember(upcoming) { upcoming.keyed().toMutableStateList() }
    val reorder =
        rememberReorderState(
            listState = listState,
            onMove = { from, to -> order.add(to, order.removeAt(from)) },
            onDrop = { key ->
                val to = order.indexOfFirst { it.key == key }
                val entry = order.getOrNull(to)?.entry
                if (entry != null && to != upcoming.indexOf(entry)) {
                    actions.move(entry.index, firstUpcomingIndex + to)
                }
            },
        )
    var menuFor by remember { mutableStateOf<QueueEntry?>(null) }
    menuFor?.let { entry ->
        QueueItemSheet(entry, actions, onDismiss = { menuFor = null })
    }

    LaunchedEffect(firstUpcomingIndex) { listState.animateScrollToItem(0) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(order, key = { it.key }) { item ->
            val entry = item.entry
            val raised = reorder.raisedKey == item.key
            UpcomingRow(
                entry = entry,
                track = library.track(entry.mediaId),
                raised = raised,
                onClick = { actions.jumpTo(entry.index) },
                onLongClick = { menuFor = entry },
                modifier =
                    if (raised) {
                        Modifier.zIndex(1f).graphicsLayer {
                            translationY = reorder.translation(item.key)
                        }
                    } else {
                        Modifier.animateItem()
                    },
                handle =
                    if (reorderable) {
                        { DragHandle(Modifier.reorderHandle(reorder, item.key)) }
                    } else {
                        null
                    },
            )
        }
    }
}

/** A queued [entry] with a [key] that stays the same while entries move around it. */
private data class KeyedEntry(val key: String, val entry: QueueEntry)

/** These entries keyed by media id and which copy of that item each one is. */
private fun List<QueueEntry>.keyed(): List<KeyedEntry> {
    val copies = HashMap<String, Int>()
    return map { entry ->
        val copy = copies.getOrDefault(entry.mediaId, 0)
        copies[entry.mediaId] = copy + 1
        KeyedEntry("${entry.mediaId}#$copy", entry)
    }
}

@Composable
private fun QueueItemSheet(entry: QueueEntry, actions: NowPlayingActions, onDismiss: () -> Unit) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = entry.title, meta = entry.artist.takeIf { it.isNotBlank() })
        SheetAction(
            DmtIcons.Play,
            stringResource(R.string.action_play),
            onClick = {
                actions.jumpTo(entry.index)
                dismiss()
            },
        )
        SheetAction(
            DmtIcons.Trash,
            stringResource(R.string.queue_remove),
            onClick = {
                actions.remove(entry.index)
                dismiss()
            },
        )
    }
}

@Composable
private fun UpcomingRow(
    entry: QueueEntry,
    track: Track?,
    raised: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    handle: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .background(if (raised) Surface2 else Ground)
                .padding(end = if (handle != null) 4.dp else Margin),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.weight(1f)
                    .pressable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        sink = 0.985f,
                        label = entry.title,
                    )
                    .padding(start = Margin, top = 8.dp, bottom = 8.dp),
        ) {
            Artwork(track = track, modifier = Modifier.size(RowArt))
            TrackText(entry.title, entry.artist, modifier = Modifier.weight(1f))
        }
        handle?.invoke()
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(48.dp)) {
        DmtIcon(
            icon = DmtIcons.DragHandle,
            tint = Muted,
            description = stringResource(R.string.queue_reorder),
        )
    }
}

@Composable
private fun TrackText(title: String, artist: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.padding(start = 14.dp, end = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (artist.isNotBlank()) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
