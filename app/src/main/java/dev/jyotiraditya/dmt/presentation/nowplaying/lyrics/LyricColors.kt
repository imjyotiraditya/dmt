package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import dev.jyotiraditya.lyrics.LyricLine

private const val UNSUNG_ALPHA = 0.32f
private const val BACKING_ALPHA = 0.78f

/** Colours of the lyrics view; dimmed colours are solid so overlapping glyphs don't brighten. */
@Immutable
internal data class LyricColors(
    val lead: Color,
    val unsung: Color,
    val backing: Color,
    val group: Color,
    val singers: List<Color>,
    /** Whether each singer is sung in their own colour, else all in [lead]. */
    val bySinger: Boolean,
) {
    /** The sung colour of [line]'s singer, so duets stay distinguishable. */
    fun singer(line: LyricLine): Color = when {
        !bySinger || line.interlude -> lead
        line.singer < 0 -> group
        line.singer == 0 -> lead
        else -> singers[(line.singer - 1) % singers.size]
    }

    companion object {
        fun on(ink: Color, background: Color, bySinger: Boolean) = LyricColors(
            lead = ink,
            unsung = ink.copy(alpha = UNSUNG_ALPHA).compositeOver(background),
            backing = ink.copy(alpha = BACKING_ALPHA).compositeOver(background),
            group = Color(0xFFE9DFC9),
            singers =
                listOf(
                    Color(0xFF9CC3E0),
                    Color(0xFFC3DE9C),
                    Color(0xFFE0A9C9),
                    Color(0xFF9CDCCB),
                    Color(0xFFB9AEE6),
                    Color(0xFFE6B49C),
                ),
            bySinger = bySinger,
        )
    }
}
