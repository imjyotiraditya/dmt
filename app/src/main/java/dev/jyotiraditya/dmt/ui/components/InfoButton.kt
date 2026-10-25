package dev.jyotiraditya.dmt.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.InlineIconSize
import dev.jyotiraditya.dmt.ui.theme.InlineIconTarget
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted

/** An info icon beside a label that opens a sheet explaining it. */
@Composable
fun InfoButton(title: String, text: String, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    IconTap(
        icon = DmtIcons.Info,
        description = stringResource(R.string.info_button_description, title),
        onClick = { open = true },
        ink = Muted,
        size = InlineIconTarget,
        iconSize = InlineIconSize,
        modifier = modifier,
    )
    if (open) InfoSheet(title, text, onDismiss = { open = false })
}

@Composable
fun InfoSheet(title: String, text: String, onDismiss: () -> Unit) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = title)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(start = Margin, end = Margin, bottom = 32.dp),
        )
    }
}
