package dev.jyotiraditya.dmt.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface2

/** The primary button: a flat block in the accent. */
@Composable
fun Block(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = LocalAccent.current.fill,
    ink: Color = LocalAccent.current.onFill,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .heightIn(min = 52.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
                .pressable(onClick = onClick, enabled = enabled, label = label)
                .background(fill)
                .padding(horizontal = 22.dp),
    ) {
        if (icon != null) {
            DmtIcon(icon = icon, tint = ink, size = 20.dp)
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1)
    }
}

/** A secondary action shown as underlined text. */
@Composable
fun TextLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Ink,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
        color = color,
        maxLines = 1,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .pressable(onClick = onClick, label = label)
                .padding(vertical = 14.dp),
    )
}

/** A text toggle, filled with [ink] while [on]. */
@Composable
fun WordToggle(
    label: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = Ink,
    field: Color = Color.Black,
) {
    val fill by animateColorAsState(if (on) ink else ink.copy(alpha = 0.1f), label = "wordFill")
    val text by animateColorAsState(if (on) field else ink, label = "wordText")
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = text,
        maxLines = 1,
        modifier =
            modifier
                .heightIn(min = 40.dp)
                .pressable(onClick = onClick, label = label)
                .background(fill)
                .padding(horizontal = 12.dp, vertical = 11.dp),
    )
}

/** A square switch, drawn only; the row holding it handles the tap. */
@Composable
fun Toggle(checked: Boolean, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val track by animateColorAsState(if (checked) accent.fill else Surface2, label = "toggleTrack")
    val thumb by animateColorAsState(if (checked) accent.onFill else Muted, label = "toggleThumb")
    Switch(
        checked = checked,
        onCheckedChange = null,
        modifier = modifier.background { track },
        thumbContent = { Box(modifier = Modifier.size(24.dp).background { thumb }) },
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = Color.Transparent,
                checkedTrackColor = Color.Transparent,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.Transparent,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent,
            ),
    )
}

/** An icon with its [value] when set, filled with [ink] while [on]. */
@Composable
fun IconChip(
    @DrawableRes icon: Int,
    description: String,
    value: String?,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = Ink,
    field: Color = Color.Black,
) {
    val fill by animateColorAsState(if (on) ink else Color.Transparent, label = "chipFill")
    val content by animateColorAsState(if (on) field else ink, label = "chipContent")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            modifier
                .heightIn(min = 40.dp)
                .pressable(onClick = onClick, label = description)
                .background(fill)
                .animateContentSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        DmtIcon(icon = icon, tint = content, size = 20.dp, description = description)
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
            )
        }
    }
}
