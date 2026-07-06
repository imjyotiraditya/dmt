package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.jyotiraditya.dmt.domain.model.Accent

val Accent.color: Color
    get() = when (this) {
        Accent.ORANGE -> TuiAccent
        Accent.MOSS -> Color(0xFF9CB56C)
        Accent.STEEL -> Color(0xFF6F9FBA)
        Accent.MONO -> TuiFg
    }

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
