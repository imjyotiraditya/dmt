package dev.jyotiraditya.lyrics

/*
 * The stretches of a song nobody sings, which a file either writes a line of note glyphs for or
 * leaves as a gap between the lines it does write.
 */

/** The text an interlude is shown as, whichever way the file said there was one. */
private const val INTERLUDE_MARKER = "* * *"

/** A line holding nothing but the glyphs a file writes when nobody sings. */
private val NOTE_GLYPHS_ONLY = Regex("""^[\s♪♫♩♬🎵🎶]+$""")

/** The gap between two lines that is long enough to be worth showing as an interlude. */
private const val INTERLUDE_GAP_MS = 8_000L

/**
 * Returns [this] with every line of nothing but note glyphs turned into an interlude.
 *
 * @return The lines, with a line of `♪♪♪` reading as an interlude instead.
 */
fun List<LyricLine>.markInstrumentalLines(): List<LyricLine> =
    map { line ->
        if (!line.interlude && NOTE_GLYPHS_ONLY.matches(line.text)) {
            line.copy(text = INTERLUDE_MARKER, interlude = true, singer = -1)
        } else {
            line
        }
    }

/**
 * Returns [this] with an interlude put in every gap long enough to be one.
 *
 * @return The lines, with an interlude between the ones far enough apart.
 */
fun List<LyricLine>.withInterludes(): List<LyricLine> {
    val out = mutableListOf<LyricLine>()
    var previousEnd = 0L

    forEach { line ->
        if (line.startMs - previousEnd >= INTERLUDE_GAP_MS) {
            out += LyricLine(
                startMs = previousEnd + 400,
                endMs = line.startMs - 200,
                text = INTERLUDE_MARKER,
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
