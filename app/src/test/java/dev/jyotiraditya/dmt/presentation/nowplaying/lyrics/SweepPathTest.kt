package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import dev.jyotiraditya.lyrics.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FixedWidthGeometry(private val text: String, private val lineStarts: List<Int>) :
    TextGeometry {
    override val lineCount: Int
        get() = lineStarts.size

    override fun lineOf(offset: Int): Int = lineStarts.indexOfLast { it <= offset }

    override fun lineStart(line: Int): Int = lineStarts[line]

    override fun lineVisibleEnd(line: Int): Int {
        var end = if (line + 1 < lineStarts.size) lineStarts[line + 1] else text.length
        while (end > lineStarts[line] && text[end - 1].isWhitespace()) end--
        return end
    }

    override fun extent(start: Int, end: Int): Extent {
        val lineStart = lineStarts[lineOf(start)]
        return Extent((start - lineStart) * 10f, (end - lineStart) * 10f)
    }
}

class SweepPathTest {

    private fun word(startMs: Long, endMs: Long, start: Int, end: Int) =
        LyricWord(startMs = startMs, endMs = endMs, start = start, end = end, background = false)

    @Test
    fun nothingBeforeTheFirstWord() {
        val text = "hello world"
        val path =
            SweepPath.of(
                text,
                listOf(word(1000, 2000, 0, 5)),
                FixedWidthGeometry(text, listOf(0)),
            )
        assertNull(path.frontAt(999))
    }

    @Test
    fun movesAcrossAWordAndWaitsAfterIt() {
        val text = "hello world"
        val path =
            SweepPath.of(
                text,
                listOf(word(0, 1000, 0, 5), word(2000, 3000, 6, 11)),
                FixedWidthGeometry(text, listOf(0)),
            )
        assertEquals(SweepFront(0, 25f), path.frontAt(500))
        assertEquals(SweepFront(0, 50f), path.frontAt(1500))
        assertEquals(SweepFront(0, 110f), path.frontAt(3000))
    }

    @Test
    fun aPhraseThatWrapsContinuesOnTheNextLine() {
        val text = "hello world foo"
        val path =
            SweepPath.of(
                text,
                listOf(word(0, 800, 6, 15)),
                FixedWidthGeometry(text, listOf(0, 12)),
            )
        assertEquals(SweepFront(0, 60f), path.frontAt(0))
        assertEquals(SweepFront(0, 85f), path.frontAt(250))
        assertEquals(SweepFront(1, 0f), path.frontAt(500))
        assertEquals(1, path.frontAt(600)!!.line)
        assertEquals(SweepFront(1, 30f), path.frontAt(800))
    }

    @Test
    fun spacesAtTheWrapAreSkipped() {
        val text = "hello world"
        val path =
            SweepPath.of(
                text,
                listOf(word(0, 1000, 0, 5), word(1000, 2000, 5, 11)),
                FixedWidthGeometry(text, listOf(0, 6)),
            )
        assertEquals(SweepFront(1, 25f), path.frontAt(1500))
    }

    @Test
    fun neverStepsBack() {
        val text = "one two three four five six"
        val words =
            listOf(
                word(0, 400, 0, 3),
                word(350, 800, 4, 7),
                word(800, 1300, 8, 13),
                word(1300, 1700, 14, 18),
                word(1700, 2100, 19, 23),
                word(2100, 2500, 24, 27),
            )
        val path = SweepPath.of(text, words, FixedWidthGeometry(text, listOf(0, 8, 19)))
        var last = SweepFront(0, Float.NEGATIVE_INFINITY)
        for (ms in 0L..2600L step 10) {
            val front = path.frontAt(ms) ?: continue
            val forward = front.line > last.line || (front.line == last.line && front.x >= last.x)
            assertTrue("stepped back at $ms: $last -> $front", forward)
            last = front
        }
    }
}
