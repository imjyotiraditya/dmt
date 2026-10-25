package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.rowFill

/** Lists the kinds of server that can be added as a source. */
@Composable
fun SourceKindsPage(dispatch: (DmtAction) -> Unit) {
    Column(
        modifier =
            Modifier.verticalScroll(rememberScrollState())
                .padding(bottom = LocalContentBottom.current),
    ) {
        PageHead(
            title = stringResource(R.string.sources_add),
            modifier = Modifier.padding(horizontal = Margin),
        )
        Text(
            text = stringResource(R.string.sources_add_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(horizontal = Margin).padding(bottom = 20.dp),
        )
        serverKinds.forEachIndexed { index, kind ->
            val title = stringResource(kind.title)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .background(index.rowFill)
                        .pressable(
                            onClick = { dispatch(DmtAction.OpenSourceForm(kind)) },
                            sink = 0.985f,
                            label = title,
                        )
                        .padding(horizontal = Margin, vertical = 18.dp),
            ) {
                DmtIcon(icon = kind.icon, tint = Ink, size = 26.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text(
                        text = stringResource(kind.line),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                DmtIcon(icon = DmtIcons.ChevronRight, tint = Muted, size = 20.dp)
            }
        }
    }
}
