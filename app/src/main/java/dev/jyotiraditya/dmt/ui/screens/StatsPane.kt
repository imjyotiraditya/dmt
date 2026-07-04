package dev.jyotiraditya.dmt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.data.Track
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtState
import dev.jyotiraditya.dmt.ui.DmtView
import dev.jyotiraditya.dmt.ui.components.Caption
import dev.jyotiraditya.dmt.ui.components.TuiPanel
import dev.jyotiraditya.dmt.ui.components.tuiClickable
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface

@Composable
fun StatsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val top = state.stats.counts.entries
        .sortedByDescending { it.value }
        .take(10)
        .mapNotNull { entry ->
            state.tracks.find { it.id == entry.key }?.let { track -> track to entry.value }
        }
    val maxCount = (top.firstOrNull()?.second ?: 1).coerceAtLeast(1)

    LazyColumn {
        item {
            Text(
                text = stringResource(R.string.back),
                style = MaterialTheme.typography.labelMedium,
                color = TuiFg,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .border(1.dp, TuiLine)
                    .background(TuiSurface.copy(alpha = 0.6f))
                    .tuiClickable { dispatch(DmtAction.Show(DmtView.SETTINGS)) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )

            TuiPanel(modifier = Modifier.padding(top = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatBlock(
                        label = stringResource(R.string.stat_time),
                        value = formatListenTime(state.stats.totalMs),
                        modifier = Modifier.weight(1f)
                    )
                    StatBlock(
                        label = stringResource(R.string.stat_plays),
                        value = "${state.stats.counts.values.sum()}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Caption(stringResource(R.string.stat_library))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock(
                    label = stringResource(R.string.stat_tracks),
                    value = "${state.tracks.size}",
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = stringResource(R.string.stat_albums),
                    value = "${state.albums.size}",
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = stringResource(R.string.stat_folders),
                    value = "${state.folders.size}",
                    modifier = Modifier.weight(1f)
                )
            }

            Caption(stringResource(R.string.stat_top))
            if (top.isEmpty()) {
                Text(
                    text = stringResource(R.string.stat_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TuiFaint
                )
            }
        }
        itemsIndexed(top, key = { _, (track, _) -> track.id }) { index, (track, count) ->
            TopTrackRow(
                index = index,
                track = track,
                count = count,
                fraction = count.toFloat() / maxCount,
                onClick = { dispatch(DmtAction.PlayAt(listOf(track), 0)) }
            )
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = TuiBright
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TuiDim
        )
    }
}

@Composable
private fun TopTrackRow(
    index: Int,
    track: Track,
    count: Int,
    fraction: Float,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tuiClickable(onClick)
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "%02d  %s".format(index + 1, track.title),
                style = MaterialTheme.typography.bodyMedium,
                color = TuiFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .height(5.dp)
                .background(TuiLine)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(5.dp)
                    .background(accent)
            )
        }
    }
}

private fun formatListenTime(ms: Long): String {
    val minutes = ms / 60_000L
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}
