package dev.jyotiraditya.lyrics

/*
 * Who sings a line, and which side of a duet that puts it on, once the lines of every singer a
 * file names have been read into one list.
 */

/**
 * Returns [this] with lines sung at once by more than one singer folded into one.
 *
 * A file writes such a line once per singer, so two lines holding the same words while both are
 * being sung are one line that everyone sings. A singer the file named as standing for many is
 * kept, so that the same one always reads the same way rather than becoming a nameless group the
 * moment two soloists happen to overlap.
 *
 * @return The lines, with the ones sung together held as a single [Voice.GROUP] line.
 */
fun List<LyricLine>.mergeSimultaneousDuplicates(): List<LyricLine> {
    val out = mutableListOf<LyricLine>()

    forEach { line ->
        val last = out.lastOrNull()

        if (last != null &&
            !last.interlude &&
            last.text == line.text &&
            line.startMs < last.endMs
        ) {
            val singer = when {
                last.voice == Voice.GROUP -> last.singer
                line.voice == Voice.GROUP -> line.singer
                else -> -1
            }

            out[out.size - 1] = last.copy(
                endMs = maxOf(last.endMs, line.endMs),
                voice = Voice.GROUP,
                singer = singer,
            )
        } else {
            out += line
        }
    }

    return out
}

/**
 * Returns [this] with every line put on the side of the singer it belongs to.
 *
 * A singer keeps the same side for the whole song, rather than the side changing every time the
 * singer does, which put a singer on either side depending on who sang before them.
 *
 * @return The lines, each on the side its singer sings from.
 */
fun List<LyricLine>.alternateVoices(): List<LyricLine> =
    map { line ->
        if (line.voice == Voice.GROUP || line.interlude || line.singer < 0) return@map line

        val side = if (line.singer % 2 == 0) Voice.PRIMARY else Voice.SECONDARY
        line.copy(voice = side)
    }
