package dev.jyotiraditya.dmt.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.AccentColor
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Accent
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.accentFor
import dev.jyotiraditya.dmt.ui.theme.wallpaperAccentAvailable

@get:StringRes
internal val AccentColor.title: Int
    get() =
        when (this) {
            AccentColor.GREEN -> R.string.accent_green
            AccentColor.BLUE -> R.string.accent_blue
            AccentColor.RED -> R.string.accent_red
            AccentColor.ORANGE -> R.string.accent_orange
            AccentColor.PURPLE -> R.string.accent_purple
            AccentColor.MONO -> R.string.accent_mono
            AccentColor.WALLPAPER -> R.string.accent_wallpaper
            AccentColor.COVER -> R.string.accent_cover
        }

@get:StringRes
private val AccentColor.hint: Int?
    get() =
        when (this) {
            AccentColor.WALLPAPER -> R.string.accent_wallpaper_hint
            AccentColor.COVER -> R.string.accent_cover_hint
            else -> null
        }

/** The accents this device can show; the wallpaper accent needs Android 12. */
private val offeredAccents: List<AccentColor>
    get() = AccentColor.entries.filter { it != AccentColor.WALLPAPER || wallpaperAccentAvailable }

/** Every accent with a swatch of its colours; [cover] previews the cover accent. */
@Composable
internal fun AccentSheet(
    current: AccentColor,
    cover: Color,
    onPick: (AccentColor) -> Unit,
    onDismiss: () -> Unit,
) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = stringResource(R.string.settings_accent))
        offeredAccents.forEach { choice ->
            AccentRow(
                choice = choice,
                colors = accentFor(choice, cover),
                chosen = choice == current,
                onClick = {
                    onPick(choice)
                    dismiss()
                },
            )
        }
    }
}

@Composable
private fun AccentRow(
    choice: AccentColor,
    colors: Accent,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(choice.title)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .pressable(onClick = onClick, sink = 0.985f, label = label)
                .padding(horizontal = Margin, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.size(width = 44.dp, height = 32.dp)) {
            Box(modifier = Modifier.weight(2f).fillMaxHeight().background(colors.fill))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colors.highlight))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = Ink)
            choice.hint?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(modifier = Modifier.size(22.dp)) {
            if (chosen) DmtIcon(icon = DmtIcons.Check, tint = Ink, size = 22.dp)
        }
    }
}
