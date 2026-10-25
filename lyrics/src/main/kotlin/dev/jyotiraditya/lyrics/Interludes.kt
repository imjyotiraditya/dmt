package dev.jyotiraditya.lyrics

private const val INTERLUDE_TEXT = "* * *"

/** The shortest gap between lines that is shown as an interlude. */
private const val INTERLUDE_GAP_MS = 8_000L

/** How long after the previous line ends an interlude appears. */
private const val INTERLUDE_LEAD_IN_MS = 400L

/** How long before the next line starts an interlude ends. */
private const val INTERLUDE_LEAD_OUT_MS = 200L

private val NOTE_GLYPHS = Regex("""^[\s♪♫♩♬🎵🎶]+$""")

internal fun List<LyricLine>.markInstrumentalLines(): List<LyricLine> = map { line ->
    if (!line.interlude && NOTE_GLYPHS.matches(line.text)) {
        line.copy(text = INTERLUDE_TEXT, interlude = true, singer = LyricLine.NO_SINGER)
    } else {
        line
    }
}

fun List<LyricLine>.withInterludes(): List<LyricLine> {
    val result = ArrayList<LyricLine>(size)
    var latestEndMs = 0L
    for (line in this) {
        if (line.startMs - latestEndMs >= INTERLUDE_GAP_MS) {
            result +=
                LyricLine(
                    startMs = latestEndMs + INTERLUDE_LEAD_IN_MS,
                    endMs = line.startMs - INTERLUDE_LEAD_OUT_MS,
                    text = INTERLUDE_TEXT,
                    voice = line.voice,
                    singer = LyricLine.NO_SINGER,
                    interlude = true,
                )
        }
        result += line
        latestEndMs = maxOf(latestEndMs, line.endMs)
    }
    return result
}
