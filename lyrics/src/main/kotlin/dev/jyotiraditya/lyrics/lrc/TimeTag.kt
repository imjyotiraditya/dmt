package dev.jyotiraditya.lyrics.lrc

private const val MS_PER_SECOND = 1_000L
private const val MS_PER_MINUTE = 60_000L
private const val SECONDS_DIGITS = 2
private const val FRACTION_DIGITS = 3

/** Milliseconds per unit of a fraction written with one, two or three digits. */
private val FRACTION_UNIT_MS = longArrayOf(100, 10, 1)

/** The brackets of a time tag: square for line times, angle for enhanced LRC word times. */
internal enum class TagStyle(val open: Char, val close: Char) {
    SQUARE('[', ']'),
    ANGLE('<', '>'),
}

/**
 * A time tag in a line, from its opening bracket at [start] to just past its closing one at [end].
 */
internal data class TimeTag(val ms: Long, val start: Int, val end: Int)

/** The `mm:ss.xx` time tag in [style] opening at [index], or null if none does. */
internal fun String.timeTagAt(index: Int, style: TagStyle): TimeTag? {
    if (getOrNull(index) != style.open) return null
    val minutes = digitsAt(index + 1, Int.MAX_VALUE) ?: return null
    if (getOrNull(minutes.end) != ':') return null
    val seconds = digitsAt(minutes.end + 1, SECONDS_DIGITS) ?: return null

    var cursor = seconds.end
    var fractionMs = 0L
    if (getOrNull(cursor) == '.' || getOrNull(cursor) == ':') {
        val fraction = digitsAt(cursor + 1, FRACTION_DIGITS) ?: return null
        fractionMs = fraction.value * FRACTION_UNIT_MS[fraction.count - 1]
        cursor = fraction.end
    }
    if (getOrNull(cursor) != style.close) return null

    val ms = minutes.value * MS_PER_MINUTE + seconds.value * MS_PER_SECOND + fractionMs
    return TimeTag(ms = ms, start = index, end = cursor + 1)
}

internal fun String.timeTags(style: TagStyle): List<TimeTag> {
    val tags = mutableListOf<TimeTag>()
    var cursor = indexOf(style.open)
    while (cursor >= 0) {
        val tag = timeTagAt(cursor, style)
        if (tag != null) tags += tag
        cursor = indexOf(style.open, tag?.end ?: (cursor + 1))
    }
    return tags
}

internal fun String.containsTimeTag(style: TagStyle): Boolean {
    var cursor = indexOf(style.open)
    while (cursor >= 0) {
        if (timeTagAt(cursor, style) != null) return true
        cursor = indexOf(style.open, cursor + 1)
    }
    return false
}

/** The square tags a line opens with, with only whitespace before and between them. */
internal fun String.leadingTimeTags(): List<TimeTag> {
    val tags = mutableListOf<TimeTag>()
    var cursor = 0
    while (true) {
        while (cursor < length && this[cursor].isWhitespace()) cursor++
        val tag = timeTagAt(cursor, TagStyle.SQUARE) ?: return tags
        tags += tag
        cursor = tag.end
    }
}

private data class Digits(val value: Long, val count: Int, val end: Int)

/** The run of at most [limit] digits starting at [index], or null if no digit is there. */
private fun String.digitsAt(index: Int, limit: Int): Digits? {
    var cursor = index
    var value = 0L
    while (cursor < length && cursor - index < limit && this[cursor].isAsciiDigit()) {
        value = value * 10 + (this[cursor] - '0')
        cursor++
    }
    val count = cursor - index
    return if (count == 0) null else Digits(value, count, cursor)
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
