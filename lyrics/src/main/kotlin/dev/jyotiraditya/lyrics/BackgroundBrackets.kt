package dev.jyotiraditya.lyrics

private const val OPENING_BRACKETS = "(（"
private const val CLOSING_BRACKETS = ")）"

/** These lines without the brackets around backing vocals, as in `(ooh ooh)`. */
internal fun List<LyricLine>.stripBackgroundBrackets(): List<LyricLine> = map {
    it.withoutBackgroundBrackets()
}

private fun LyricLine.withoutBackgroundBrackets(): LyricLine {
    val cuts = backgroundRuns().flatMap { text.wrappingBrackets(it) }.sorted().toIntArray()
    if (cuts.isEmpty()) return this

    // Each offset moves left by the number of cuts before it.
    fun moved(offset: Int): Int = offset - cuts.countBelow(offset)

    return copy(
        text = text.filterIndexed { index, _ -> cuts.binarySearch(index) < 0 },
        words =
            words
                .map { it.copy(start = moved(it.start), end = moved(it.end)) }
                .filter { it.end > it.start },
    )
}

private fun LyricLine.backgroundRuns(): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    var runStart = -1
    var runEnd = -1
    for (word in words.sortedBy { it.start }) {
        when {
            word.background && runStart < 0 -> {
                runStart = word.start
                runEnd = word.end
            }

            word.background -> runEnd = word.end

            runStart >= 0 -> {
                runs += runStart until runEnd
                runStart = -1
            }
        }
    }
    if (runStart >= 0) runs += runStart until runEnd
    return runs
}

/** The indices of a bracket pair wrapping exactly the visible text of [range], or none. */
private fun String.wrappingBrackets(range: IntRange): List<Int> {
    val open = range.firstOrNull { !this[it].isWhitespace() } ?: return emptyList()
    val close = range.lastOrNull { !this[it].isWhitespace() } ?: return emptyList()
    return if (open < close && isOnePair(open, close)) listOf(open, close) else emptyList()
}

private fun String.isOnePair(open: Int, close: Int): Boolean {
    if (this[open] !in OPENING_BRACKETS || this[close] !in CLOSING_BRACKETS) return false
    var depth = 0
    for (index in open..close) {
        when (this[index]) {
            in OPENING_BRACKETS -> depth++
            in CLOSING_BRACKETS -> if (--depth == 0) return index == close
        }
    }
    return false
}

/** How many elements of this sorted array are smaller than [value]. */
private fun IntArray.countBelow(value: Int): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid] < value) low = mid + 1 else high = mid
    }
    return low
}
