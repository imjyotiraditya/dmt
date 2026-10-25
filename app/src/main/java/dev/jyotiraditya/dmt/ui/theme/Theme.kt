package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/** The app's dark theme, with [accent] for buttons, toggles and the now-playing marks. */
@Composable
fun DMTTheme(accent: Accent = GreenAccent, content: @Composable () -> Unit) {
    val colors =
        remember(accent) {
            darkColorScheme(
                primary = accent.fill,
                onPrimary = accent.onFill,
                secondary = Ink,
                onSecondary = Ground,
                background = Ground,
                onBackground = Ink,
                surface = Ground,
                onSurface = Ink,
                surfaceVariant = Surface,
                onSurfaceVariant = Muted,
                surfaceContainerLow = Ground,
                surfaceContainer = Surface,
                surfaceContainerHigh = Surface2,
                outline = Surface2,
                outlineVariant = Surface2,
                error = Danger,
            )
        }
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(
            colorScheme = colors,
            shapes = Shapes,
            typography = Typography,
            content = content,
        )
    }
}
