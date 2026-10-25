package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricWord
import dev.jyotiraditya.lyrics.TimedText

/** How long a last word lasts when it has no closing tag and no earlier words to average. */
private const val LONE_WORD_MS = 500L

/**
 * [text] without its word time tags, and the words they time. Angle tags win over square ones, and
 * a last word with no closing tag lasts as long as the words before it on average.
 */
internal fun readWordTimes(text: String): TimedText {
    val tags = text.timeTags(TagStyle.ANGLE).ifEmpty { text.timeTags(TagStyle.SQUARE) }
    if (tags.isEmpty()) return TimedText(text)

    val plain = StringBuilder(text.length).append(text, 0, tags.first().start)
    val words = ArrayList<LyricWord>(tags.size)
    var sungMs = 0L
    var sungCount = 0

    tags.forEachIndexed { index, tag ->
        val next = tags.getOrNull(index + 1)
        val gapEnd = next?.start ?: text.length
        val wordStart = plain.length
        plain.append(text, tag.end, gapEnd)
        val wordEnd = wordStart + text.visibleLength(tag.end, gapEnd)
        if (wordEnd == wordStart) return@forEachIndexed

        val endMs = next?.ms ?: (tag.ms + if (sungCount == 0) LONE_WORD_MS else sungMs / sungCount)
        words +=
            LyricWord(
                startMs = tag.ms,
                endMs = endMs,
                start = wordStart,
                end = wordEnd,
                background = false,
            )
        if (endMs > tag.ms) {
            sungMs += endMs - tag.ms
            sungCount++
        }
    }
    return TimedText(plain.toString().trimEnd(), words)
}

/**
 * These words moved onto a line starting at [startMs]. Words that all end before the line's first
 * start, [firstStartMs], are timed relative to the line.
 */
internal fun List<LyricWord>.shiftedTo(startMs: Long, firstStartMs: Long): List<LyricWord> {
    if (isEmpty()) return this
    val relative = last().endMs <= firstStartMs
    val offset = if (relative) startMs else startMs - firstStartMs
    if (offset == 0L) return this
    return map { it.copy(startMs = it.startMs + offset, endMs = it.endMs + offset) }
}

/** The length from [start] to [end] without trailing whitespace. */
private fun String.visibleLength(start: Int, end: Int): Int {
    var last = end
    while (last > start && this[last - 1].isWhitespace()) last--
    return last - start
}
