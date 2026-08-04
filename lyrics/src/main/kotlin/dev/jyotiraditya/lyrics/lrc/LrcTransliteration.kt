package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.TimedText

/** The script a line is written in, which is what tells a reading apart from the words it reads. */
private enum class Script { LATIN, CJK, ARABIC, CYRILLIC }

/**
 * Returns [this] with a line that reads another one folded into it.
 *
 * A file gives a reading as a line of its own that starts when the line it reads does, so two
 * lines that start together and are written in different scripts are one line and its reading.
 *
 * @return The lines, with a reading held by the line it belongs to rather than following it.
 */
internal fun List<LyricLine>.pairTransliterations(): List<LyricLine> {
    val out = mutableListOf<LyricLine>()

    forEach { line ->
        val last = out.lastOrNull()

        when {
            last != null &&
                last.startMs == line.startMs &&
                last.transliteration == null &&
                line.transliteration == null &&
                isTransliterationOf(last.text, line.text) -> {
                val (main, translit) = if (scriptOf(line.text) == Script.LATIN) {
                    last to line
                } else {
                    line to last
                }

                out[out.size - 1] = main.copy(
                    endMs = maxOf(last.endMs, line.endMs),
                    transliteration = TimedText(
                        text = translit.text,
                        words = translit.words,
                    ),
                )
            }

            last != null &&
                last.startMs == line.startMs &&
                last.transliteration != null &&
                line.transliteration == null -> {
                out[out.size - 1] = last.copy(
                    endMs = maxOf(last.endMs, line.endMs),
                    translation = last.translation +
                        TimedText(text = line.text, words = line.words),
                )
            }

            else -> out += line
        }
    }

    return out
}

/** Returns the script [text] is written in, or null if it is written in none this knows. */
private fun scriptOf(text: String): Script? =
    text.firstNotNullOfOrNull { c ->
        when (Character.UnicodeScript.of(c.code)) {
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HAN,
            -> Script.CJK

            Character.UnicodeScript.ARABIC -> Script.ARABIC
            Character.UnicodeScript.CYRILLIC -> Script.CYRILLIC
            Character.UnicodeScript.LATIN -> Script.LATIN
            else -> null
        }
    }

/** Whether [bgText] reads [mainText], which holding a different script is taken to mean. */
internal fun isTransliterationOf(mainText: String, bgText: String): Boolean {
    val main = scriptOf(mainText)
    val bg = scriptOf(bgText)

    return main != null && bg != null && main != bg
}
