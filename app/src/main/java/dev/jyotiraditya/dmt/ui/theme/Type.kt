package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.jyotiraditya.dmt.R

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) =
    TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = size.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )

val Typography = Typography(
    displaySmall = mono(26, FontWeight.Bold, 1f),
    titleLarge = mono(21, FontWeight.Bold),
    titleMedium = mono(15, FontWeight.Bold),
    bodyLarge = mono(14),
    bodyMedium = mono(13),
    bodySmall = mono(11, tracking = 0.5f),
    labelLarge = mono(13, FontWeight.Bold, tracking = 1f),
    labelMedium = mono(12, tracking = 1.5f),
    labelSmall = mono(10, tracking = 1.5f),
)
