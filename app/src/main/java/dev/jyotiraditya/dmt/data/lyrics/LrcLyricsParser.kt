package dev.jyotiraditya.dmt.data.lyrics

private val LRC_TIME = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
private val WORD_TIME = Regex("""<(\d+):(\d{1,2})(?:[.:](\d{1,3}))?>""")
private val VOICE_PREFIX = Regex("""^v\d+:""")
private val BG_LINE = Regex("""^\[bg:(.*)]$""", RegexOption.IGNORE_CASE)

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
            words += LyricWord(tag.toMs(), next.toMs(), wordStart, wordStart + trimmedLen, background = false)
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
            val (text, words) = parseWordTags(bg.groupValues[1])
            if (text.isNotBlank() && words.isNotEmpty()) {
                val bgWords = words.map { it.copy(background = true) }
                lines += LyricLine(bgWords.first().startMs, bgWords.last().endMs, text, bgWords)
            }
            return@forEach
        }

        val stamps = LRC_TIME.findAll(line).toList()
        if (stamps.isEmpty()) return@forEach
        val rawText = VOICE_PREFIX.replaceFirst(line.substring(stamps.last().range.last + 1).trim(), "")
        if (rawText.isEmpty()) return@forEach
        val (text, words) = parseWordTags(rawText)
        if (text.isBlank()) return@forEach
        stamps.forEach { match ->
            val startMs = match.toMs()
            if (startMs < 0) return@forEach
            lines += LyricLine(startMs, -1L, text, words)
        }
    }
    if (lines.isEmpty()) return null
    return Lyrics(lines.sortedBy { it.startMs }.fillLineEnds(), synced = true)
}

fun isLrc(raw: String): Boolean = LRC_TIME.containsMatchIn(raw)
