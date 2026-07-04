package dev.jyotiraditya.dmt.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.player.asTime
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtState
import dev.jyotiraditya.dmt.ui.components.Hairline
import dev.jyotiraditya.dmt.ui.components.TuiKey
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onLongPress: () -> Unit,
) {
    val fraction =
        if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TuiLine)
            .background(TuiSurface.copy(alpha = 0.85f))
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = { dispatch(DmtAction.Expand(true)) },
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Hairline(fraction)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = "${state.title}_",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TuiBright,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = "${state.artist} · ${state.positionMs.asTime()}/${state.durationMs.asTime()}"
                        .lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "^",
                style = MaterialTheme.typography.labelMedium,
                color = TuiFaint,
                modifier = Modifier.padding(end = 10.dp)
            )
            TuiKey(if (state.isPlaying) "||" else "|>") { dispatch(DmtAction.TogglePlay) }
            Spacer(modifier = Modifier.width(8.dp))
            TuiKey(">>|") { dispatch(DmtAction.Next) }
        }
    }
}
