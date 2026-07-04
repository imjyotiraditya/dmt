package dev.jyotiraditya.dmt.data.lyrics

object LyricsParser {

    fun parse(raw: String): Lyrics? {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> null
            trimmed.startsWith("<") && trimmed.contains("<tt") -> parseTtml(trimmed)
            isLrc(trimmed) -> parseLrc(trimmed)
            else -> Lyrics(
                lines = trimmed.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { LyricLine(-1L, -1L, it) },
                synced = false,
            )
        }
    }
}

fun List<LyricLine>.fillLineEnds(): List<LyricLine> = mapIndexed { index, line ->
    if (line.endMs > line.startMs) {
        line
    } else {
        val nextStart = getOrNull(index + 1)?.startMs
        line.copy(endMs = nextStart ?: (line.startMs + 10_000L))
    }
}

fun List<LyricLine>.mergeSimultaneousDuplicates(): List<LyricLine> {
    val out = mutableListOf<LyricLine>()
    forEach { line ->
        val last = out.lastOrNull()
        if (last != null && !last.interlude && last.text == line.text &&
            line.startMs < last.endMs
        ) {
            out[out.size - 1] = last.copy(
                endMs = maxOf(last.endMs, line.endMs),
                voice = VOICE_GROUP,
                singer = -1,
            )
        } else {
            out += line
        }
    }
    return out
}

fun List<LyricLine>.withInterludes(): List<LyricLine> {
    val out = mutableListOf<LyricLine>()
    var previousEnd = 0L
    forEach { line ->
        if (line.startMs - previousEnd >= 8_000L) {
            out += LyricLine(
                startMs = previousEnd + 400,
                endMs = line.startMs - 200,
                text = "· · ·",
                voice = line.voice,
                singer = -1,
                interlude = true,
            )
        }
        out += line
        previousEnd = maxOf(previousEnd, line.endMs)
    }
    return out
}
