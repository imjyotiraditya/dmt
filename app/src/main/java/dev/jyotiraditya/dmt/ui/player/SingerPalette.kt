package dev.jyotiraditya.dmt.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.jyotiraditya.dmt.ui.theme.LocalAccent

private val fixedSingerColors = listOf(
    Color(0xFF6F9FBA),
    Color(0xFF9CB56C),
    Color(0xFFBF8AAE),
    Color(0xFFC9A15E),
    Color(0xFF6FB8A8),
    Color(0xFF9C8ED6),
    Color(0xFFD68A7A),
    Color(0xFF7AAFD6),
    Color(0xFFA8A85E),
    Color(0xFFD68AA8),
    Color(0xFF8EBF9C),
    Color(0xFFD6B26F),
    Color(0xFF8E9CD6),
    Color(0xFFC97A5E),
)

@Composable
fun rememberSingerPalette(): List<Color> {
    val accent = LocalAccent.current
    return remember(accent) { listOf(accent) + fixedSingerColors }
}
