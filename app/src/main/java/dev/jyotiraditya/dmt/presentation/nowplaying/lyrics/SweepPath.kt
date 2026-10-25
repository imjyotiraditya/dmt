package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import dev.jyotiraditya.lyrics.LyricWord
import kotlin.math.abs

/** Where the sweep has reached: a visual line and a horizontal position on it. */
internal data class SweepFront(val line: Int, val x: Float)

/** A horizontal stretch of drawn text, from where the sweep enters it to where it leaves. */
internal data class Extent(val from: Float, val to: Float)

/** The parts of a text layout the sweep needs. */
internal interface TextGeometry {
    val lineCount: Int

    fun lineOf(offset: Int): Int

    fun lineStart(line: Int): Int

    /** The end of the visible characters of [line], exclusive, without trailing spaces. */
    fun lineVisibleEnd(line: Int): Int

    /** Where the characters from [start] until [end], all on one line, are drawn. */
    fun extent(start: Int, end: Int): Extent
}

/**
 * The route the sweep takes through a line of lyrics, as time-ordered pieces each on one visual
 * line; a wrapped word's time is split between its parts by width.
 */
internal class SweepPath private constructor(private val pieces: List<Piece>) {

    private data class Piece(
        val startMs: Long,
        val endMs: Long,
        val line: Int,
        val fromX: Float,
        val toX: Float,
    )

    private val startTimes = LongArray(pieces.size) { pieces[it].startMs }

    /** The front at [positionMs], waiting at the end of the last piece between pieces. */
    fun frontAt(positionMs: Long): SweepFront? {
        val index = lastStartedAt(positionMs)
        if (index < 0) return null
        val piece = pieces[index]
        if (positionMs >= piece.endMs) return SweepFront(piece.line, piece.toX)
        val fraction = (positionMs - piece.startMs).toFloat() / (piece.endMs - piece.startMs)
        return SweepFront(piece.line, piece.fromX + (piece.toX - piece.fromX) * fraction)
    }

    /** The index of the last piece started by [positionMs], or -1 if none has. */
    private fun lastStartedAt(positionMs: Long): Int {
        var low = 0
        var high = startTimes.lastIndex
        var found = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (startTimes[mid] <= positionMs) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }

    companion object {
        val Empty = SweepPath(emptyList())

        fun of(text: String, words: List<LyricWord>, geometry: TextGeometry): SweepPath {
            val pieces = mutableListOf<Piece>()
            var previousEndMs = Long.MIN_VALUE
            for (word in words.sortedBy { it.startMs }) {
                // A piece starts no earlier than the previous one ended.
                val startMs = maxOf(word.startMs, previousEndMs)
                val endMs = maxOf(word.endMs, startMs + 1)
                pieces += word.split(text, geometry, startMs, endMs)
                previousEndMs = endMs
            }
            return SweepPath(pieces)
        }

        /** This word's visible characters, cut at each visual line, timed by width. */
        private fun LyricWord.split(
            text: String,
            geometry: TextGeometry,
            startMs: Long,
            endMs: Long,
        ): List<Piece> {
            val visible = text.visibleRange(start, end) ?: return emptyList()
            val parts = mutableListOf<Triple<Int, Float, Float>>()
            var offset = visible.first
            while (offset <= visible.last) {
                val line = geometry.lineOf(offset)
                val partEnd = minOf(visible.last + 1, geometry.lineVisibleEnd(line))
                if (partEnd > offset) {
                    val extent = geometry.extent(offset, partEnd)
                    parts += Triple(line, extent.from, extent.to)
                }
                val nextLine = line + 1
                if (nextLine >= geometry.lineCount) break
                offset = maxOf(offset + 1, geometry.lineStart(nextLine))
            }
            val widths = parts.map { (_, from, to) -> abs(to - from) }
            val total = widths.sum()
            if (parts.isEmpty() || total <= 0f) return emptyList()

            var elapsed = 0f
            return parts.mapIndexed { index, (line, from, to) ->
                val pieceStart = startMs + ((endMs - startMs) * (elapsed / total)).toLong()
                elapsed += widths[index]
                val pieceEnd = startMs + ((endMs - startMs) * (elapsed / total)).toLong()
                Piece(pieceStart, maxOf(pieceEnd, pieceStart + 1), line, from, to)
            }
        }

        private fun String.visibleRange(start: Int, end: Int): IntRange? {
            var first = start.coerceIn(0, length)
            var last = end.coerceIn(first, length) - 1
            while (first <= last && this[first].isWhitespace()) first++
            while (last >= first && this[last].isWhitespace()) last--
            return if (first <= last) first..last else null
        }
    }
}
