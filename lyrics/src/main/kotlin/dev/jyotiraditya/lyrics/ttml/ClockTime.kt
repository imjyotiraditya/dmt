package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.LyricLine
import kotlin.math.roundToLong

private const val MS_PER_SECOND = 1_000.0
private const val SECONDS_PER_MINUTE = 60
private const val MS_SUFFIX = "ms"
private const val SECONDS_SUFFIX = "s"

/** A TTML offset or clock time in milliseconds, or [LyricLine.UNTIMED] if missing or malformed. */
internal fun parseClockTime(value: String?): Long {
    val time = value?.trim().orEmpty()
    if (time.isEmpty()) return LyricLine.UNTIMED

    val seconds =
        when {
            time.endsWith(MS_SUFFIX) -> time.numberBefore(MS_SUFFIX)?.div(MS_PER_SECOND)
            time.endsWith(SECONDS_SUFFIX) -> time.numberBefore(SECONDS_SUFFIX)
            else -> time.clockSeconds()
        }
    if (seconds == null || seconds < 0) return LyricLine.UNTIMED
    return (seconds * MS_PER_SECOND).roundToLong()
}

private fun String.numberBefore(suffix: String): Double? = removeSuffix(suffix).toDoubleOrNull()

private fun String.clockSeconds(): Double? {
    val parts = split(':')
    if (parts.size > 3) return null
    val seconds = parts.last().toDoubleOrNull() ?: return null
    val whole =
        parts.dropLast(1).fold(0L) { total, part ->
            total * SECONDS_PER_MINUTE + (part.toLongOrNull() ?: return null)
        }
    return whole * SECONDS_PER_MINUTE + seconds
}
