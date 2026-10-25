package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/** Opacity of secondary text, such as an artist line or elapsed time. */
const val SecondaryAlpha = 0.78f

/** Opacity of inactive text, such as an unselected tab. */
const val RestingAlpha = 0.45f

/** This colour at secondary strength, blended over [background] into a solid colour. */
fun Color.secondary(background: Color): Color =
    copy(alpha = alpha * SecondaryAlpha).compositeOver(background)

/** This colour at inactive strength, blended over [background] into a solid colour. */
fun Color.resting(background: Color): Color =
    copy(alpha = alpha * RestingAlpha).compositeOver(background)
