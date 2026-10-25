package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.ui.adaptive.windowShape

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    R.font.jakarta,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

private val Tight =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )

private fun text(size: Int, weight: Int = 400, lead: Float = 1.45f, tracking: TextUnit = 0.sp) =
    TextStyle(
        fontFamily = Jakarta,
        fontWeight = FontWeight(weight),
        fontSize = size.sp,
        lineHeight = (size * lead).sp,
        letterSpacing = tracking,
        fontFeatureSettings = "tnum",
    )

private fun display(size: Int) = TextStyle(
    fontFamily = Jakarta,
    fontWeight = FontWeight(800),
    fontSize = size.sp,
    lineHeight = (size * 0.94f).sp,
    letterSpacing = (-0.035).em,
    lineHeightStyle = Tight,
)

val Typography =
    Typography(
        displayLarge = display(64),
        displayMedium = display(52),
        displaySmall = display(40),
        headlineLarge = display(34),
        headlineMedium = display(28),
        headlineSmall = text(22, 700, 1.2f, (-0.02).em),
        titleLarge = text(22, 700, 1.2f, (-0.02).em),
        titleMedium = text(17, 600, 1.3f),
        titleSmall = text(16, 600, 1.3f),
        bodyLarge = text(17),
        bodyMedium = text(16),
        bodySmall = text(14),
        labelLarge = text(16, 600, 1.2f),
        labelMedium = text(14, 500, 1.2f),
        labelSmall = text(13, 500, 1.2f),
    )

/** Times and counts. */
val Numerals = text(14, 500, 1.2f)

/** The widest window poster sizes follow, so wide windows don't get oversized text. */
private const val POSTER_MAX_WIDTH_DP = 460

/** A poster font size as a [share] of the capped window width. */
@Composable
@ReadOnlyComposable
fun posterSize(share: Float): TextUnit =
    (minOf(windowShape().width.value, POSTER_MAX_WIDTH_DP.toFloat()) * share).sp

/** Poster text: the heaviest weight with tight lines, sized by [posterSize]. */
@Composable
@ReadOnlyComposable
fun poster(share: Float = 0.15f): TextStyle {
    val size = posterSize(share)
    return TextStyle(
        fontFamily = Jakarta,
        fontWeight = FontWeight(800),
        fontSize = size,
        lineHeight = size * 0.98f,
        letterSpacing = (-0.045).em,
        lineHeightStyle = Tight,
    )
}

/** A poster share that keeps [text] within two lines, shrinking for longer titles. */
fun fitShare(text: String, max: Float): Float = when {
    text.length <= 14 -> max
    text.length <= 24 -> max * 0.82f
    text.length <= 36 -> max * 0.66f
    else -> max * 0.55f
}

val LyricType =
    TextStyle(
        fontFamily = Jakarta,
        fontWeight = FontWeight(800),
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.02).em,
    )
