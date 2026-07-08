package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
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
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )

            Caption(stringResource(R.string.stat_listening))
            StatRow(
                label = stringResource(R.string.stat_time),
                value = formatListenTime(state.stats.totalMs),
            )
            StatRow(
                label = stringResource(R.string.stat_plays),
                value = "${state.stats.counts.values.sum()}",
            )

            Caption(stringResource(R.string.stat_library))
            StatRow(
                label = stringResource(R.string.stat_tracks),
                value = "${state.tracks.size}",
            )
            StatRow(
                label = stringResource(R.string.stat_albums),
                value = "${state.albums.size}",
            )
            StatRow(
                label = stringResource(R.string.stat_folders),
                value = "${state.folders.size}",
            )

            Caption(stringResource(R.string.stat_top))
            if (top.isEmpty()) {
                Text(
                    text = stringResource(R.string.stat_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TuiFaint,
                )
            }
        }
        itemsIndexed(top, key = { _, (track, _) -> track.id }) { index, (track, count) ->
            TopTrackRow(
                index = index,
                track = track,
                count = count,
                fraction = count.toFloat() / maxCount,
                onClick = { dispatch(DmtAction.PlayAt(listOf(track), 0)) },
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TuiFg,
        )
        Text(
            text = ".".repeat(200),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TuiBright,
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
            .padding(vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "%02d  %s".format(index + 1, track.title),
                style = MaterialTheme.typography.bodyMedium,
                color = TuiFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
            )
        }
        val cols = 28
        val filled = (fraction.coerceIn(0f, 1f) * cols).toInt().coerceAtLeast(1)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = accent)) { append("█".repeat(filled)) }
                withStyle(SpanStyle(color = TuiFaint)) { append("░".repeat(cols - filled)) }
            },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

private fun formatListenTime(ms: Long): String {
    val minutes = ms / 60_000L
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}
