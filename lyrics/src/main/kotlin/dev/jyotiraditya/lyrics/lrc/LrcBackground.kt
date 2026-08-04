package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.TimedText

/** The `[bg: ...]` a line sung behind the one before it is written as. */
internal val BG_LINE = Regex("""^\[bg:(.*)]$""", RegexOption.IGNORE_CASE)

/**
 * Reads the line [content] holds into [lines], as a line sung behind the one before it.
 *
 * Such a line is also how a file writes a reading of the line before it, which carrying a time of
 * its own and being written in another script is taken to mean.
 *
 * @param content What the `[bg: ...]` holds.
 * @param lines The lines read so far, which the line read here is added to.
 */
internal fun parseBackgroundLine(content: String, lines: MutableList<LyricLine>) {
    val nested = LEADING_TIME.containsMatchIn(content.trim())
    val (stripped, _) = stripLinePrefix(content)
    val (text, words) = parseWordTags(stripped)
    if (text.isBlank() || words.isEmpty()) return

    val precedingMain = lines.lastOrNull()

    if (nested && precedingMain != null && isTransliterationOf(precedingMain.text, text)) {
        lines[lines.size - 1] = precedingMain.copy(
            transliteration = TimedText(
                text = text,
                words = words,
            ),
        )
    } else {
        val bgWords = words.map { it.copy(background = true) }

        lines += LyricLine(
            startMs = bgWords.first().startMs,
            endMs = bgWords.last().endMs,
            text = text,
            words = bgWords,
            singer = precedingMain?.singer ?: 0,
        )
    }
}
