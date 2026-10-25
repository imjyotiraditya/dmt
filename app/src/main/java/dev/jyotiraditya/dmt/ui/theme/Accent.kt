package dev.jyotiraditya.dmt.ui.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import dev.jyotiraditya.dmt.domain.model.AccentColor
import android.graphics.Color as PlatformColor

/** An accent: [fill] for buttons and toggles, [onFill] on it, [highlight] on the ground. */
@Immutable data class Accent(val fill: Color, val onFill: Color, val highlight: Color)

val GreenAccent = Accent(fill = Green, onFill = GreenInk, highlight = Mint)

private val FixedAccents: Map<AccentColor, Accent> =
    mapOf(
        AccentColor.GREEN to GreenAccent,
        AccentColor.BLUE to Accent(Color(0xFF1F4F8F), Color.White, Color(0xFFC4D8F5)),
        AccentColor.RED to Accent(Color(0xFFA3312B), Color.White, Color(0xFFF6C9C4)),
        AccentColor.ORANGE to Accent(Color(0xFFB4561D), Color.White, Color(0xFFF7D4BC)),
        AccentColor.PURPLE to Accent(Color(0xFF5B3A9A), Color.White, Color(0xFFDDCFF5)),
        AccentColor.MONO to Accent(Ink, Dark, Ink),
    )

val LocalAccent = compositionLocalOf { GreenAccent }

/** Whether this device can take its accent from the wallpaper. */
@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
val wallpaperAccentAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The colours of [choice]; [cover] is the colour of the cover playing now. */
@Composable
fun accentFor(choice: AccentColor, cover: Color): Accent = when (choice) {
    AccentColor.GREEN,
    AccentColor.BLUE,
    AccentColor.RED,
    AccentColor.ORANGE,
    AccentColor.PURPLE,
    AccentColor.MONO,
    -> FixedAccents.getValue(choice)

    AccentColor.WALLPAPER -> wallpaperAccent()

    AccentColor.COVER -> remember(cover) { Accent(cover, cover.inkOn(), cover.pale()) }
}

@Composable
private fun wallpaperAccent(): Accent {
    if (!wallpaperAccentAvailable) return GreenAccent
    val context = LocalContext.current
    return remember(context) {
        val scheme = dynamicDarkColorScheme(context)
        Accent(fill = scheme.primary, onFill = scheme.onPrimary, highlight = scheme.primary)
    }
}

/** A pale tint of this colour's hue, readable on the dark ground. */
private fun Color.pale(): Color {
    val hsv = FloatArray(3)
    PlatformColor.colorToHSV(toArgb(), hsv)
    hsv[1] = hsv[1].coerceAtMost(0.28f)
    hsv[2] = 0.94f
    return Color(PlatformColor.HSVToColor(hsv))
}
