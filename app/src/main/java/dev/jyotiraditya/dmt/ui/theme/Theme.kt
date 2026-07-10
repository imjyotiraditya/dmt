package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAccent = staticCompositionLocalOf { TuiAccent }

private val TuiColorScheme = darkColorScheme(
    primary = TuiBright,
    onPrimary = TuiBg,
    secondary = TuiFg,
    onSecondary = TuiBg,
    tertiary = TuiDim,
    onTertiary = TuiBg,
    background = TuiBg,
    onBackground = TuiFg,
    surface = TuiBg,
    onSurface = TuiFg,
    surfaceVariant = TuiSurface,
    onSurfaceVariant = TuiDim,
    outline = TuiLine,
    error = TuiRed,
)

@Composable
fun DMTTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TuiColorScheme,
        typography = Typography,
        content = content,
    )
}
