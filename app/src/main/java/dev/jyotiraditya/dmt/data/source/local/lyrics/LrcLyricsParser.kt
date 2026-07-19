package dev.jyotiraditya.dmt.data.source.local.lyrics

import dev.jyotiraditya.dmt.domain.model.LyricLine
import dev.jyotiraditya.dmt.domain.model.LyricWord
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.TimedText

private enum class Script { LATIN, CJK, ARABIC, CYRILLIC }

object LrcLyricsParser {

    private val LINE_TIME = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val WORD_TIME = Regex("""<(\d+):(\d{1,2})(?:[.:](\d{1,3}))?>""")
    private val VOICE_PREFIX = Regex("""^v(\d+):""")
    private val BG_LINE = Regex("""^\[bg:(.*)]$""", RegexOption.IGNORE_CASE)
    private val LEADING_TIME = Regex("""^\[\d+:\d{1,2}(?:[.:]\d{1,3})?]""")

    fun matches(raw: String): Boolean = LINE_TIME.containsMatchIn(raw)

    fun parse(raw: String): Lyrics? {
        val lines = mutableListOf<LyricLine>()
        val singers = mutableMapOf<Int, Int>()

        raw.lines().forEach { line ->
            val trimmedLine = line.trim()

            val bg = BG_LINE.matchEntire(trimmedLine)
            if (bg != null) {
                parseBackgroundLine(bg.groupValues[1], lines)
                return@forEach
            }

            val stamps = LINE_TIME.findAll(line).toList()
            if (stamps.isEmpty()) return@forEach

            val (rawText, voiceTag) = stripLinePrefix(line.substring(stamps.last().range.last + 1))
            if (rawText.isEmpty()) return@forEach

            val (text, words) = parseWordTags(rawText)
            if (text.isBlank()) return@forEach

            val singer = voiceTag?.let { tag -> singers.getOrPut(tag) { singers.size } } ?: 0

            stamps.forEach { match ->
                val startMs = match.toMs()
                if (startMs < 0) return@forEach

                lines += LyricLine(
                    startMs = startMs,
                    endMs = words.lastOrNull()?.endMs ?: -1L,
                    text = text,
                    words = words,
                    singer = singer,
                )
            }
        }

        if (lines.isEmpty()) return null

        return Lyrics(
            lines = lines.sortedBy { it.startMs }
                .pairTransliterations()
                .fillLineEnds()
                .mergeSimultaneousDuplicates()
                .alternateVoices()
                .withInterludes(),
            synced = true,
        )
    }

    private fun parseBackgroundLine(content: String, lines: MutableList<LyricLine>) {
        val (stripped, _) = stripLinePrefix(content)
        val (text, words) = parseWordTags(stripped)
        if (text.isBlank() || words.isEmpty()) return

        val precedingMain = lines.lastOrNull()

        if (precedingMain != null && isTransliterationOf(precedingMain.text, text)) {
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

    private fun List<LyricLine>.pairTransliterations(): List<LyricLine> {
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

    private fun stripLinePrefix(text: String): Pair<String, Int?> {
        val withoutLeadingTime = LEADING_TIME.replaceFirst(text.trim(), "")
        val voice = VOICE_PREFIX.find(withoutLeadingTime)
        val stripped = withoutLeadingTime
            .substring(voice?.range?.let { it.last + 1 } ?: 0)
            .trimStart()

        return stripped to voice?.groupValues?.get(1)?.toIntOrNull()
    }

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

    private fun isTransliterationOf(mainText: String, bgText: String): Boolean {
        val main = scriptOf(mainText)
        val bg = scriptOf(bgText)

        return main != null && bg != null && main != bg
    }

    private fun MatchResult.toMs(): Long {
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

    private fun parseWordTags(text: String): Pair<String, List<LyricWord>> {
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
}
