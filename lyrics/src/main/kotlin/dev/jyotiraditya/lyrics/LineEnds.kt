package dev.jyotiraditya.lyrics

/** How long the last line shows when its end is not given. */
private const val LAST_LINE_MS = 10_000L

/** These lines with missing ends filled in, each until the next starts. Expects them sorted. */
fun List<LyricLine>.fillLineEnds(): List<LyricLine> = mapIndexed { index, line ->
    if (line.endMs > line.startMs) {
        line
    } else {
        line.copy(endMs = getOrNull(index + 1)?.startMs ?: (line.startMs + LAST_LINE_MS))
    }
}
