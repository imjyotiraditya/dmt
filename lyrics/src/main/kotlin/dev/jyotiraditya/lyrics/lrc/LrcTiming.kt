package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricWord

/*
 * The times a file writes, which say when a line starts and, when it was written with them, when
 * every word of that line does.
 */

/** The `[mm:ss.SS]` a line opens with. */
internal val LINE_TIME = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/** The `<mm:ss.SS>` a word opens with, which only an enhanced file writes. */
internal val WORD_TIME = Regex("""<(\d+):(\d{1,2})(?:[.:](\d{1,3}))?>""")

/** The [LINE_TIME] a line opens with, anchored so that only a leading one is found. */
internal val LEADING_TIME = Regex("""^\[\d+:\d{1,2}(?:[.:]\d{1,3})?]""")

/**
 * Returns the time [this] holds in milliseconds.
 *
 * A file may write the fraction of a second in tenths, hundredths or thousandths, so what it
 * means depends on how many digits were written.
 *
 * @return The time, or -1 if the match holds no minutes and seconds.
 */
internal fun MatchResult.toMs(): Long {
    val minutes = groupValues[1].toLongOrNull() ?: return -1L
    val seconds = groupValues[2].toLongOrNull() ?: return -1L

    val fraction = groupValues[3]
    val fractionMs = when (fraction.length) {
        0 -> 0L
        1 -> fraction.toLong() * 100
        2 -> fraction.toLong() * 10
        else -> fraction.take(3).toLong()
    }

    return minutes * 60_000 + seconds * 1_000 + fractionMs
}

/**
 * Returns the words of [text] and the text left once their times are taken out of it.
 *
 * A word runs until the next word starts, so the last time of a line closes the word before it
 * rather than opening one of its own.
 *
 * @param text The line to read, times and all.
 * @return The line as it reads, and the words it holds times for.
 */
internal fun parseWordTags(text: String): Pair<String, List<LyricWord>> {
    val tags = WORD_TIME.findAll(text).toList()
    if (tags.isEmpty()) return text to emptyList()

    val plain = StringBuilder()
    val words = mutableListOf<LyricWord>()

    plain.append(text, 0, tags.first().range.first)

    tags.forEachIndexed { index, tag ->
        val gapEnd = tags.getOrNull(index + 1)?.range?.first ?: text.length
        val gap = text.substring(tag.range.last + 1, gapEnd)

        val wordStart = plain.length
        plain.append(gap)

        val next = tags.getOrNull(index + 1)
        val trimmedLen = gap.trimEnd().length

        if (next != null && trimmedLen > 0) {
            words += LyricWord(
                startMs = tag.toMs(),
                endMs = next.toMs(),
                start = wordStart,
                end = wordStart + trimmedLen,
                background = false,
            )
        }
    }

    return plain.toString().trimEnd() to words
}
