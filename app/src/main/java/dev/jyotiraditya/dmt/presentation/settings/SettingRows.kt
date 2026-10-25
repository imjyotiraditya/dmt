package dev.jyotiraditya.dmt.presentation.settings

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.InfoButton
import dev.jyotiraditya.dmt.ui.components.Toggle
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.components.pressableToggle
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted

@Composable
internal fun GroupHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = Ink,
        modifier = modifier.padding(horizontal = Margin).padding(top = 44.dp, bottom = 10.dp),
    )
}

/** A setting switched by tapping the row; [info] is a longer explanation behind an icon. */
@Composable
internal fun ToggleRow(
    label: String,
    hint: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    info: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .pressableToggle(value = checked, onValueChange = { onToggle() }, sink = 0.985f)
                .padding(horizontal = Margin, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, color = Ink)
                if (info != null) InfoButton(title = label, text = info)
            }
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Toggle(checked = checked)
    }
}

/** A setting that opens a page or an outside screen, with its current value if it has one. */
@Composable
internal fun LinkRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = DmtIcons.ChevronRight,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .pressable(onClick = onClick, sink = 0.985f, label = label)
                .padding(horizontal = Margin, vertical = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        DmtIcon(icon = icon, tint = Ink, size = 20.dp)
    }
}

@Composable
internal fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = Margin, vertical = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = Muted)
    }
}
