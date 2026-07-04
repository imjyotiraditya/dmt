package dev.jyotiraditya.dmt.data.lyrics

private val LRC_TIME = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")

fun parseLrc(raw: String): Lyrics? {
    val lines = mutableListOf<LyricLine>()
    raw.lines().forEach { line ->
        val stamps = LRC_TIME.findAll(line).toList()
        if (stamps.isEmpty()) return@forEach
        val text = line.substring(stamps.last().range.last + 1).trim()
        if (text.isEmpty()) return@forEach
        stamps.forEach { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
            val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
            val fraction = match.groupValues[3]
            val fractionMs = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                else -> fraction.take(3).toLong()
            }
            lines += LyricLine(minutes * 60_000 + seconds * 1_000 + fractionMs, -1L, text)
        }
    }
    if (lines.isEmpty()) return null
    return Lyrics(lines.sortedBy { it.startMs }.fillLineEnds(), synced = true)
}

fun isLrc(raw: String): Boolean = LRC_TIME.containsMatchIn(raw)
