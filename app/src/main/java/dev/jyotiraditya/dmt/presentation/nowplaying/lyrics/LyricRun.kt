package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import androidx.compose.ui.text.intl.Locale
import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.LyricWord
import dev.jyotiraditya.lyrics.TimedText

/** A stretch of a line sung by the lead or the backing vocals, with its words' timing. */
internal data class LyricRun(val background: Boolean, val text: String, val words: List<LyricWord>)

/** The line split into lead and backing runs, each word's offsets made relative to its run. */
internal fun buildRuns(line: LyricLine): List<LyricRun> {
    if (line.words.isEmpty()) {
        return listOf(
            LyricRun(
                background = false,
                text = line.text,
                words = emptyList(),
            ),
        )
    }

    val groups = mutableListOf<MutableList<LyricWord>>()
    line.words
        .sortedBy { it.start }
        .forEach { word ->
            val current = groups.lastOrNull()
            if (current != null && current.last().background == word.background) {
                current += word
            } else {
                groups += mutableListOf(word)
            }
        }

    val runs = mutableListOf<LyricRun>()
    var boundary = 0
    groups.forEachIndexed { index, group ->
        val runStart = boundary
        val runEnd =
            if (index == groups.lastIndex) {
                line.text.length
            } else {
                groups[index + 1].first().start
            }
        val raw = line.text.substring(runStart, runEnd)
        val leading = raw.takeWhile(Char::isWhitespace).length
        val trimmed = raw.trim()
        val shift = runStart + leading
        val words = group.map { word ->
            word.copy(
                start = (word.start - shift).coerceIn(0, trimmed.length),
                end = (word.end - shift).coerceIn(0, trimmed.length),
            )
        }
        runs +=
            LyricRun(
                background = group.first().background,
                text = trimmed,
                words = words,
            )
        boundary = runEnd
    }

    return runs
}

/** The reading and translations shown under the line, leaving out ones that repeat it. */
internal fun secondaryRunsFor(line: LyricLine, runs: List<LyricRun>): List<LyricRun> = buildList {
    line.transliteration?.let { translit ->
        add(
            LyricRun(
                background = true,
                text = translit.text,
                words = translit.words,
            ),
        )
    }

    val translations = line.translation.preferredTranslation()
    val originals =
        if (runs.size == translations.size) {
            runs.map { it.text }
        } else {
            null
        }

    translations.forEachIndexed { i, segment ->
        val original = originals?.get(i) ?: line.text
        if (!segment.text.equals(original, ignoreCase = true)) {
            add(
                LyricRun(
                    background = true,
                    text = segment.text,
                    words = segment.words,
                ),
            )
        }
    }
}

/** One translation in the device's language, else English, when several languages are given. */
private fun List<TimedText>.preferredTranslation(): List<TimedText> {
    val distinctLangs = mapNotNull { it.lang }.distinct()
    if (distinctLangs.size <= 1) return this

    val deviceLang = Locale.current.language
    val match = firstOrNull { it.lang == deviceLang } ?: firstOrNull { it.lang == "en" } ?: first()

    return listOf(match)
}
