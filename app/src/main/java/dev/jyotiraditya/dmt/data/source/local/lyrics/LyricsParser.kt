package dev.jyotiraditya.dmt.data.source.local.lyrics

import dev.jyotiraditya.dmt.domain.model.LyricLine
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.Voice

object LyricsParser {

    fun parse(raw: String): Lyrics? {
        val trimmed = raw.trim()

        return when {
            trimmed.isEmpty() -> null

            trimmed.startsWith("<") && trimmed.contains("<tt") ->
                TtmlLyricsParser.parse(trimmed)

            LrcLyricsParser.matches(trimmed) ->
                LrcLyricsParser.parse(trimmed)

            else -> parsePlain(trimmed)
        }
    }

    private fun parsePlain(trimmed: String): Lyrics {
        val lines = trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { text ->
                LyricLine(
                    startMs = -1L,
                    endMs = -1L,
                    text = text,
                )
            }

        return Lyrics(
            lines = lines,
            synced = false,
        )
    }
}

fun List<LyricLine>.fillLineEnds(): List<LyricLine> =
    mapIndexed { index, line ->
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

        if (last != null &&
            !last.interlude &&
            last.text == line.text &&
            line.startMs < last.endMs
        ) {
            out[out.size - 1] = last.copy(
                endMs = maxOf(last.endMs, line.endMs),
                voice = Voice.GROUP,
                singer = -1,
            )
        } else {
            out += line
        }
    }

    return out
}

fun List<LyricLine>.alternateVoices(): List<LyricLine> {
    var side = Voice.SECONDARY
    var lastSinger = -1

    return map { line ->
        if (line.voice == Voice.GROUP || line.interlude) return@map line

        if (line.singer != lastSinger) {
            side = if (side == Voice.PRIMARY) Voice.SECONDARY else Voice.PRIMARY
            lastSinger = line.singer
        }

        line.copy(voice = side)
    }
}

fun List<LyricLine>.withInterludes(): List<LyricLine> {
    val out = mutableListOf<LyricLine>()
    var previousEnd = 0L

    forEach { line ->
        if (line.startMs - previousEnd >= 8_000L) {
            out += LyricLine(
                startMs = previousEnd + 400,
                endMs = line.startMs - 200,
                text = "* * *",
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
