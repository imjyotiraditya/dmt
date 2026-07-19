package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine

@Composable
fun BlocklistPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val blocked = state.settings.blockedFolders
    val paths = remember {
        (state.folders.map { it.path } + blocked)
            .distinct()
            .sortedBy { it.lowercase() }
    }

    Column {
        Caption(stringResource(R.string.blocklist_title))
        if (paths.isEmpty()) {
            Caption(stringResource(R.string.no_files))
            return
        }
        Text(
            text = stringResource(R.string.blocklist_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LazyColumn {
            items(paths, key = { it }) { path ->
                val hidden = path in blocked
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tuiClickable {
                                dispatch(
                                    DmtAction.Config(
                                        state.settings.copy(
                                            blockedFolders =
                                                if (hidden) blocked - path else blocked + path,
                                        ),
                                    ),
                                )
                            }
                            .padding(vertical = 9.dp),
                    ) {
                        Text(
                            text = if (hidden) "[x]" else "[ ]",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hidden) TuiAccent else TuiFaint,
                        )
                        Text(
                            text = " " + path.removePrefix("/storage/emulated/0/").ifEmpty { "/" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hidden) TuiDim else TuiFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(color = TuiLine)
                }
            }
        }
    }
}
