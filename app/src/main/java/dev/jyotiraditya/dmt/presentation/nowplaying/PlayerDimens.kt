package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.compose.ui.unit.dp

/** Sizes shared by the expanded player's pages. */
internal object PlayerDimens {

    /** The largest the cover grows on big screens. */
    val MaxCover = 480.dp

    val PlayButton = 76.dp

    val PlayGlyph = 34.dp

    val SkipButton = 60.dp

    val SkipGlyph = 30.dp

    val PlayToSkip = 12.dp

    /** Space between a label and the line under it, such as title and artist. */
    val LineGap = 4.dp

    val ToggleGap = 8.dp

    val QueueTarget = 48.dp

    val QueueGlyph = 24.dp

    /** Delay before a title too long for one line starts to scroll. */
    const val MARQUEE_DELAY_MS = 2000
}
