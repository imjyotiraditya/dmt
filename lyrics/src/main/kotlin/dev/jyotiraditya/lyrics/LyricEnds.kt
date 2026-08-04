package dev.jyotiraditya.lyrics

/** How long a line is shown for when it is the last one and nothing says when it ends. */
private const val LAST_LINE_MS = 10_000L

/**
 * Returns [this] with an end given to every line that was written without one.
 *
 * A file need only write when a line starts, so a line runs until the next one does, and the last
 * line of all runs for as long as a line is worth showing.
 *
 * @return The lines, each with an end later than its start.
 */
fun List<LyricLine>.fillLineEnds(): List<LyricLine> =
    mapIndexed { index, line ->
        if (line.endMs > line.startMs) {
            line
        } else {
            val nextStart = getOrNull(index + 1)?.startMs
            line.copy(endMs = nextStart ?: (line.startMs + LAST_LINE_MS))
        }
    }
