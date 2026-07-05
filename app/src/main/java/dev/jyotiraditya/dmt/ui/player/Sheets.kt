package dev.jyotiraditya.dmt.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.data.Track
import dev.jyotiraditya.dmt.player.asTime
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtState
import dev.jyotiraditya.dmt.ui.components.tuiClickable
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuiSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TuiBg,
        shape = RectangleShape,
        dragHandle = null
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TuiLine)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                content()
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun SheetHeader(title: String, meta: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(LocalAccent.current)
            )
            Text(
                text = " $title",
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim
            )
        }
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = TuiFaint
            )
        }
    }
}

@Composable
fun QueueList(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current

    LazyColumn(modifier = modifier) {
        itemsIndexed(state.queue) { index, label ->
            val current = index == state.queueIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .tuiClickable { dispatch(DmtAction.Jump(index)) }
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (current) accent else TuiFaint)
                )
                Text(
                    text = " %02d ".format(index + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiFaint
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (current) TuiBright else TuiDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = TuiFaint,
                    modifier = Modifier
                        .tuiClickable { dispatch(DmtAction.RemoveAt(index)) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun InfoContent(state: DmtState) {
    val track: Track? = state.tracks.find { it.id.toString() == state.nowPlayingId }

    InfoRow(
        label = stringResource(R.string.info_title),
        value = state.title
    )
    InfoRow(
        label = stringResource(R.string.info_artist),
        value = state.artist.lowercase()
    )
    if (state.album.isNotBlank()) {
        InfoRow(
            label = stringResource(R.string.info_album),
            value = state.album.lowercase()
        )
    }
    InfoRow(
        label = stringResource(R.string.info_duration),
        value = state.durationMs.asTime()
    )
    state.tech.forEach { spec ->
        InfoRow(
            label = spec.label.lowercase(),
            value = spec.value.lowercase()
        )
    }
    track?.let {
        InfoRow(
            label = stringResource(R.string.info_uri),
            value = it.uri.toString()
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TuiFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TuiLine)
        )
    }
}
