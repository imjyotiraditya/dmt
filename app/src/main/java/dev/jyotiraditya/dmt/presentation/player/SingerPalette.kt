package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.jyotiraditya.dmt.ui.theme.LocalAccent

val GroupVoice = Color(0xFFCBBFA3)

private val singerColors = listOf(
    Color(0xFF7A9BB3),
    Color(0xFF98A96F),
    Color(0xFFAF87A3),
    Color(0xFF74AB9E),
    Color(0xFF9389C4),
    Color(0xFFC08575),
    Color(0xFF80A6C4),
    Color(0xFFBD84A0),
    Color(0xFF8AB194),
    Color(0xFF8C96C0),
    Color(0xFFB97B62),
)

@Composable
fun rememberSingerPalette(): List<Color> {
    val accent = LocalAccent.current
    return remember(accent) {
        listOf(accent) + singerColors
    }
}
