package dev.jyotiraditya.dmt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.motion.FADE_MS
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface2

private val MarkSize = 24.dp
private val MarkGlyph = 16.dp

/** One option in a single-choice list, with a square mark that fills while [selected]. */
@Composable
fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    enabled: Boolean = true,
) {
    val text = if (enabled) Ink else Muted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .pressableSelect(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled,
                    sink = 0.985f,
                )
                .padding(horizontal = Margin, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = text)
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (enabled) SelectionMark(selected, modifier = Modifier.padding(start = 16.dp))
    }
}

/** A square that fills with a check while [selected], and shows [empty] otherwise. */
@Composable
fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier, empty: Color = Surface2) {
    val fill by
        animateColorAsState(
            if (selected) Ink else empty,
            tween(FADE_MS),
            label = "choiceMark",
        )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(MarkSize).background { fill },
    ) {
        if (selected) DmtIcon(icon = DmtIcons.Check, tint = Ground, size = MarkGlyph)
    }
}
