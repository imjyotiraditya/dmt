package dev.jyotiraditya.dmt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.SectionGap

@Composable
fun PageHead(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.displaySmall,
        color = Ink,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp),
    )
}

/** The heading of a section of a page, with an optional [action] at its end. */
@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Margin)
                .padding(top = SectionGap, bottom = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/** A label and its value side by side. */
@Composable
fun FactRow(label: String, value: String, modifier: Modifier = Modifier, ink: Color = Ink) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ink.copy(alpha = 0.62f),
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Large text tabs, the selected one in full ink, scrolling when they overflow. */
@Composable
fun WordTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = Ink,
    rest: Color = Muted,
    inset: Dp = Margin,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .selectableGroup()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = inset),
    ) {
        labels.forEachIndexed { index, label ->
            val color by animateColorAsState(if (index == selected) ink else rest, label = "word")
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                maxLines = 1,
                modifier =
                    Modifier.pressableSelect(
                        selected = index == selected,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                    )
                        .padding(vertical = 10.dp),
            )
        }
    }
}
