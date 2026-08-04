package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.LyricWord
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/*
 * Reading the values and the timed text of a document, apart from walking the document
 * itself, which is what the parser does with what these return.
 */

internal fun XmlReader.attr(localName: String): String? {
    for (i in 0 until attributeCount) {
        if (getAttributeLocalName(i) == localName) return getAttributeValue(i)
    }
    return null
}

internal fun parseTime(value: String?): Long {
    if (value.isNullOrBlank()) return -1L

    val trimmed = value.trim()

    return runCatching {
        when {
            trimmed.endsWith("ms") -> trimmed.dropLast(2).toDouble().toLong()

            trimmed.endsWith("s") && !trimmed.contains(':') ->
                (trimmed.dropLast(1).toDouble() * 1000).toLong()

            else -> {
                val parts = trimmed.split(':')
                val seconds = parts.last().toDouble()
                val minutes = parts.getOrNull(parts.size - 2)?.toLongOrNull() ?: 0L
                val hours = parts.getOrNull(parts.size - 3)?.toLongOrNull() ?: 0L

                (hours * 3600_000) + (minutes * 60_000) + (seconds * 1000).toLong()
            }
        }
    }.getOrDefault(-1L)
}

internal fun isFormattingOnly(chunk: String): Boolean =
    chunk.isNotEmpty() &&
            chunk.all { it.isWhitespace() } &&
            chunk.any { it == '\n' || it == '\r' }

internal fun readTimedText(parser: XmlReader): Pair<String, List<LyricWord>> {
    val text = StringBuilder()
    val words = mutableListOf<LyricWord>()
    val spanStack = ArrayDeque<SpanFrame>()
    var pendingSpace = false

    fun flushSpace() {
        if (pendingSpace && text.isNotEmpty() && text.last() != '\n') text.append(' ')
        pendingSpace = false
    }

    var depth = 1
    var event = parser.next()

    while (depth > 0) {
        when (event) {
            EventType.START_ELEMENT -> {
                depth++

                if (parser.localName == "span") {
                    flushSpace()

                    spanStack.addLast(
                        SpanFrame(
                            beginMs = parseTime(parser.attr("begin")),
                            endMs = parseTime(parser.attr("end")),
                            textStart = text.length,
                            background = false,
                        ),
                    )
                }
            }

            EventType.TEXT, EventType.IGNORABLE_WHITESPACE -> if (!isFormattingOnly(parser.text)) {
                parser.text.forEach { c ->
                    if (c.isWhitespace()) {
                        pendingSpace = true
                    } else {
                        flushSpace()
                        text.append(c)
                    }
                }
            }

            EventType.END_ELEMENT -> {
                depth--

                if (parser.localName == "span" && spanStack.isNotEmpty()) {
                    val frame = spanStack.removeLast()

                    if (frame.beginMs >= 0 && text.length > frame.textStart) {
                        words += LyricWord(
                            startMs = frame.beginMs,
                            endMs = frame.endMs,
                            start = frame.textStart,
                            end = text.length,
                            background = false,
                        )
                    }
                }
            }

            else -> Unit
        }

        if (depth > 0) event = parser.next()
    }

    return text.toString().trim() to words
}

internal fun readTranslationSegments(parser: XmlReader): List<String> {
    val segments = mutableListOf<String>()
    val current = StringBuilder()
    val bgStack = ArrayDeque<Boolean>()
    var currentBg = false
    var pendingSpace = false

    fun flushSpace() {
        if (pendingSpace && current.isNotEmpty() && current.last() != '\n') current.append(' ')
        pendingSpace = false
    }

    fun cutSegment() {
        val text = current.toString().trim()
        if (text.isNotEmpty()) segments += text

        current.clear()
        pendingSpace = false
    }

    var depth = 1
    var event = parser.next()

    while (depth > 0) {
        when (event) {
            EventType.START_ELEMENT -> {
                depth++

                if (parser.localName == "span") {
                    val isBg = currentBg || parser.attr("role") == ROLE_BACKGROUND

                    if (isBg != currentBg) {
                        cutSegment()
                        currentBg = isBg
                    }

                    bgStack.addLast(currentBg)
                    flushSpace()
                }
            }

            EventType.TEXT, EventType.IGNORABLE_WHITESPACE -> if (!isFormattingOnly(parser.text)) {
                parser.text.forEach { c ->
                    if (c.isWhitespace()) {
                        pendingSpace = true
                    } else {
                        flushSpace()
                        current.append(c)
                    }
                }
            }

            EventType.END_ELEMENT -> {
                depth--

                if (parser.localName == "span" && bgStack.isNotEmpty()) {
                    bgStack.removeLast()

                    val outerBg = bgStack.lastOrNull() ?: false
                    if (outerBg != currentBg) {
                        cutSegment()
                        currentBg = outerBg
                    }
                }
            }

            else -> Unit
        }

        if (depth > 0) event = parser.next()
    }

    cutSegment()

    return segments
}

/**
 * Tracks the order and type of `ttm:agent` declarations so each agent, solo
 * or a named group, gets a stable [LyricLine.singer] index in the order it's
 * first actually used on a line, independent of how many the document
 * declares. `-1` is reserved for lines with no declared agent at all.
 */
