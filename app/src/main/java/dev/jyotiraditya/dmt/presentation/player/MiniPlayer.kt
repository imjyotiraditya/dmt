package dev.jyotiraditya.dmt.presentation.player

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
import dev.jyotiraditya.dmt.core.common.CursorTitle
import dev.jyotiraditya.dmt.core.common.Hairline
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.util.asTime

@Composable
fun MiniPlayer(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
) {
    val fraction =
        if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Hairline(fraction)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            ) {
                CursorTitle(
                    text = state.title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                val position = state.positionMs.asTime()
                val duration = state.durationMs.asTime()
                Text(
                    text = "${state.artist} · $position/$duration".lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "^",
                style = MaterialTheme.typography.labelMedium,
                color = TuiFaint,
                modifier = Modifier.padding(end = 10.dp),
            )
            TuiKey(if (state.isPlaying) "||" else "|>") { dispatch(DmtAction.TogglePlay) }
            Spacer(modifier = Modifier.width(8.dp))
            TuiKey(">>|") { dispatch(DmtAction.Next) }
        }
    }
}
