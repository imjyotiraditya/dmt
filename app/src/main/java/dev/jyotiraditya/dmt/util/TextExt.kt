package dev.jyotiraditya.dmt.util

import java.text.BreakIterator

/**
 * Returns where each character of [this] between [start] and [end] begins and ends, starting at
 * [start] and ending at [end].
 *
 * A script may write one character as several code points, such as a Devanagari letter and the
 * vowel sign that hangs off it. Colouring part of such a character leaves the marks without the
 * letter they belong to, which is drawn as a dotted circle, so the parts are one entry.
 */
fun String.clusters(start: Int, end: Int): List<Int> {
    if (end <= start) return listOf(start, end)

    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(substring(start, end))

    return buildList {
        add(start)
        var boundary = iterator.next()
        while (boundary != BreakIterator.DONE) {
            add(start + boundary)
            boundary = iterator.next()
        }
    }
}

/** Returns the end of the character of [this] that [index] falls in. */
fun String.clusterEnd(index: Int): Int {
    if (index <= 0 || index >= length) return index.coerceIn(0, length)

    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)

    return if (iterator.isBoundary(index)) index else iterator.following(index)
}
