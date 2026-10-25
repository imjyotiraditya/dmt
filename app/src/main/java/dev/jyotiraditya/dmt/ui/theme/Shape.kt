package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val Square = RoundedCornerShape(0.dp)

/** Square corners at every size. */
val Shapes =
    Shapes(
        extraSmall = Square,
        small = Square,
        medium = Square,
        large = Square,
        extraLarge = Square,
    )
