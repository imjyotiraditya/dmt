package dev.jyotiraditya.dmt.data.source.local.lyrics

import dev.jyotiraditya.dmt.domain.model.LyricLine
import dev.jyotiraditya.dmt.domain.model.LyricWord
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.Transliteration

private val LRC_TIME = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
private val WORD_TIME = Regex("""<(\d+):(\d{1,2})(?:[.:](\d{1,3}))?>""")
private val VOICE_PREFIX = Regex("""^v\d+:""")
private val BG_LINE = Regex("""^\[bg:(.*)]$""", RegexOption.IGNORE_CASE)
private val LEADING_TIME = Regex("""^\[\d+:\d{1,2}(?:[.:]\d{1,3})?]""")

private fun stripLinePrefix(text: String): String {
    val withoutLeadingTime = LEADING_TIME.replaceFirst(text.trim(), "")
    return VOICE_PREFIX.replaceFirst(withoutLeadingTime, "").trimStart()
}

private fun scriptOf(text: String): String? =
    text.firstNotNullOfOrNull { c ->
        when {
            c.code in 0x3040..0x30FF || c.code in 0x4E00..0x9FFF -> "cjk"
            c.code in 0x0600..0x06FF -> "arabic"
            c.code in 0x0400..0x04FF -> "cyrillic"
            c.isLetter() && c.code < 128 -> "latin"
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

fun parseLrc(raw: String): Lyrics? {
    val lines = mutableListOf<LyricLine>()
    raw.lines().forEach { line ->
        val trimmedLine = line.trim()
        val bg = BG_LINE.matchEntire(trimmedLine)
        if (bg != null) {
            val (text, words) = parseWordTags(stripLinePrefix(bg.groupValues[1]))
            if (text.isNotBlank() && words.isNotEmpty()) {
                val precedingMain = lines.lastOrNull()
                if (precedingMain != null && isTransliterationOf(precedingMain.text, text)) {
                    lines[lines.size - 1] = precedingMain.copy(
                        transliteration = Transliteration(
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
                    )
                }
            }
            return@forEach
        }

        val stamps = LRC_TIME.findAll(line).toList()
        if (stamps.isEmpty()) return@forEach

        val rawText = stripLinePrefix(line.substring(stamps.last().range.last + 1))
        if (rawText.isEmpty()) return@forEach

        val (text, words) = parseWordTags(rawText)
        if (text.isBlank()) return@forEach

        stamps.forEach { match ->
            val startMs = match.toMs()
            if (startMs < 0) return@forEach
            lines += LyricLine(
                startMs = startMs,
                endMs = -1L,
                text = text,
                words = words,
            )
        }
    }
    if (lines.isEmpty()) return null
    return Lyrics(
        lines = lines.sortedBy { it.startMs }.fillLineEnds(),
        synced = true,
    )
}

fun isLrc(raw: String): Boolean = LRC_TIME.containsMatchIn(raw)
