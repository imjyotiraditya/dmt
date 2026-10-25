package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
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
import dev.jyotiraditya.dmt.util.QueueEntry

private val RowArt = 44.dp

/** What plays next: tap to play, drag the handle to reorder, long press to remove. */
@OptIn(UnstableApi::class)
@Composable
internal fun QueuePage(state: DmtState, player: Player, dispatch: (DmtAction) -> Unit) {
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
                onClick = {
                    dispatch(DmtAction.ClearUpcoming)
                },
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
            dispatch = dispatch,
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
    dispatch: (DmtAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val order = remember(upcoming) { upcoming.toMutableStateList() }
    val drag = remember { DragState(listState) }
    var menuFor by remember { mutableStateOf<QueueEntry?>(null) }
    menuFor?.let { entry ->
        QueueItemSheet(entry, dispatch, onDismiss = { menuFor = null })
    }

    LaunchedEffect(firstUpcomingIndex) { listState.animateScrollToItem(0) }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(order, key = { it.index }) { entry ->
            val dragging = drag.key == entry.index
            Box(
                modifier =
                    Modifier.then(if (dragging) Modifier.zIndex(1f) else Modifier.animateItem())
                        .graphicsLayer { translationY = if (dragging) drag.offset else 0f },
            ) {
                UpcomingRow(
                    entry = entry,
                    track = library.track(entry.mediaId),
                    raised = dragging,
                    onClick = { dispatch(DmtAction.Jump(entry.index)) },
                    onLongClick = { menuFor = entry },
                    handle =
                        if (reorderable) {
                            {
                                DragHandle(
                                    Modifier.reorderDrag(entry.index, drag, order) { from, to ->
                                        dispatch(
                                            DmtAction.MoveInQueue(from, firstUpcomingIndex + to),
                                        )
                                    },
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

@Composable
private fun QueueItemSheet(
    entry: QueueEntry,
    dispatch: (DmtAction) -> Unit,
    onDismiss: () -> Unit,
) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = entry.title, meta = entry.artist.takeIf { it.isNotBlank() })
        SheetAction(
            DmtIcons.Play,
            stringResource(R.string.action_play),
            onClick = {
                dispatch(DmtAction.Jump(entry.index))
                dismiss()
            },
        )
        SheetAction(
            DmtIcons.Trash,
            stringResource(R.string.queue_remove),
            onClick = {
                dispatch(DmtAction.RemoveAt(entry.index))
                dismiss()
            },
        )
    }
}

/** Drags row [key] through [order], then reports its player index and new place to [onMove]. */
private fun Modifier.reorderDrag(
    key: Int,
    drag: DragState,
    order: MutableList<QueueEntry>,
    onMove: (from: Int, to: Int) -> Unit,
): Modifier = pointerInput(key) {
    detectDragGestures(
        onDragStart = { drag.start(key) },
        onDrag = { change, amount ->
            change.consume()
            drag.by(amount.y, order)
        },
        onDragEnd = { drag.finish(order)?.let { (from, to) -> onMove(from, to) } },
        onDragCancel = { drag.cancel() },
    )
}

/** The row being dragged, how far it has moved, and where it started. */
private class DragState(private val listState: LazyListState) {
    var key by mutableStateOf<Int?>(null)
        private set

    var offset by mutableFloatStateOf(0f)
        private set

    private var startPosition = -1

    fun start(key: Int) {
        this.key = key
        offset = 0f
        startPosition =
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.index ?: -1
    }

    /** Moves the dragged row by [dy], swapping it with the row its centre passes over. */
    fun by(dy: Float, order: MutableList<QueueEntry>) {
        val dragged = key ?: return
        offset += dy
        val items = listState.layoutInfo.visibleItemsInfo
        val info = items.firstOrNull { it.key == dragged } ?: return
        val centre = info.offset + info.size / 2f + offset
        val target =
            items.firstOrNull {
                it.key != dragged && centre.toInt() in it.offset until it.offset + it.size
            } ?: return
        val from = order.indexOfFirst { it.index == dragged }
        val to = order.indexOfFirst { it.index == target.key }
        if (from < 0 || to < 0) return
        order.add(to, order.removeAt(from))
        // The row now sits where the target was; keep it under the finger.
        offset -= target.offset - info.offset
    }

    /** Ends the drag, returning the row's player index and new place if it moved. */
    fun finish(order: List<QueueEntry>): Pair<Int, Int>? {
        val dragged = key ?: return null
        val to = order.indexOfFirst { it.index == dragged }
        val moved = to >= 0 && to != startPosition
        cancel()
        return if (moved) dragged to to else null
    }

    fun cancel() {
        key = null
        offset = 0f
        startPosition = -1
    }
}

@Composable
private fun UpcomingRow(
    entry: QueueEntry,
    track: Track?,
    raised: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    handle: (@Composable () -> Unit)?,
) {
    val endPadding = if (handle != null) 4.dp else Margin
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .background(if (raised) Surface2 else Ground)
                .pressable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    sink = 0.985f,
                    label = entry.title,
                )
                .padding(
                    start = Margin,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = endPadding,
                ),
    ) {
        Artwork(track = track, modifier = Modifier.size(RowArt))
        TrackText(entry.title, entry.artist, modifier = Modifier.weight(1f))
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
