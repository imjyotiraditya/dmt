package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import dev.jyotiraditya.lyrics.LyricLine

internal enum class LineState {
    UPCOMING,
    ACTIVE,
    PASSED,
}

/** The index of the line playing at [positionMs], or -1 before the first; lines sort by start. */
internal fun List<LyricLine>.indexAt(positionMs: Long): Int {
    val found = binarySearch { it.startMs.compareTo(positionMs) }
    return if (found >= 0) found else -found - 2
}

internal fun LyricLine.stateAt(positionMs: Long, synced: Boolean): LineState = when {
    !synced || startMs < 0 -> LineState.UPCOMING
    positionMs >= endMs -> LineState.PASSED
    positionMs >= startMs -> LineState.ACTIVE
    else -> LineState.UPCOMING
}
