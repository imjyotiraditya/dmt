package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.ui.graphics.Color

// A dark-only palette; surfaces are told apart by tone, never by borders or shadows.

val Ground = Color(0xFF111111)

/** A raised surface: fields and selected rows. */
val Surface = Color(0xFF1C1C1C)

/** A surface placed on [Surface]. */
val Surface2 = Color(0xFF2A2A2A)

val Ink = Color(0xFFF2F2F2)

val Muted = Color(0xFF9C9C9C)

val Dark = Color(0xFF0A0A0A)

val Mint = Color(0xFFC3EDCE)

val Pink = Color(0xFFF8C7C9)

val Green = Color(0xFF1F6B3A)

val GreenInk = Color(0xFFFFFFFF)

val Night = Color(0xFF000000)

val NightInk = Color(0xFFF2F2F2)

val Danger = Color(0xFFFF8A80)

/** The fill of the row at this position in a list, alternating [Surface] and [Surface2]. */
val Int.rowFill: Color
    get() = if (this % 2 == 0) Surface else Surface2

/** The text colour with the most contrast against this background. */
fun Color.inkOn(): Color =
    if ((red * 0.2126f + green * 0.7152f + blue * 0.0722f) > 0.42f) Dark else Ink
