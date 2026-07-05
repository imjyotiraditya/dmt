package dev.jyotiraditya.dmt.data.lyrics

import android.util.Xml
import org.xmlpull.v1.XmlPullParser

private fun XmlPullParser.attr(qualifiedName: String): String? {
    val local = qualifiedName.substringAfter(':')
    getAttributeValue(null, qualifiedName)?.let { return it }
    for (i in 0 until attributeCount) {
        val name = getAttributeName(i)
        if (name == qualifiedName || name == local || name.substringAfterLast(':') == local) {
            return getAttributeValue(i)
        }
    }
    return null
}

private fun parseTtmlTime(value: String?): Long {
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

private class SpanFrame(
    val beginMs: Long,
    val endMs: Long,
    val textStart: Int,
    val background: Boolean,
) {
    var hadChild = false
}

private fun readTimedText(parser: XmlPullParser): Pair<String, List<LyricWord>> {
    val text = StringBuilder()
    val words = mutableListOf<LyricWord>()
    val spanStack = ArrayDeque<SpanFrame>()
    var pendingSpace = false
    var pendingNewline = false

    fun flushSpace() {
        if (pendingSpace && !pendingNewline && text.isNotEmpty()) text.append(' ')
        pendingSpace = false
        pendingNewline = false
    }

    var depth = 1
    var event = parser.next()
    while (depth > 0) {
        when (event) {
            XmlPullParser.START_TAG -> {
                depth++
                if (parser.name == "span") {
                    flushSpace()
                    spanStack.addLast(
                        SpanFrame(
                            beginMs = parseTtmlTime(parser.attr("begin")),
                            endMs = parseTtmlTime(parser.attr("end")),
                            textStart = text.length,
                            background = false,
                        )
                    )
                }
            }

            XmlPullParser.TEXT -> parser.text.forEach { c ->
                if (c.isWhitespace()) {
                    pendingSpace = true
                    if (c == '\n' || c == '\r') pendingNewline = true
                } else {
                    flushSpace()
                    text.append(c)
                }
            }

            XmlPullParser.END_TAG -> {
                depth--
                if (parser.name == "span" && spanStack.isNotEmpty()) {
                    val frame = spanStack.removeLast()
                    if (frame.beginMs >= 0 && text.length > frame.textStart) {
                        words += LyricWord(frame.beginMs, frame.endMs, frame.textStart, text.length, background = false)
                    }
                }
            }
        }
        if (depth > 0) event = parser.next()
    }
    return text.toString().trim() to words
}

private fun readTranslationSegments(parser: XmlPullParser): List<String> {
    val segments = mutableListOf<String>()
    val current = StringBuilder()
    val bgStack = ArrayDeque<Boolean>()
    var currentBg = false
    var pendingSpace = false
    var pendingNewline = false

    fun flushSpace() {
        if (pendingSpace && !pendingNewline && current.isNotEmpty()) current.append(' ')
        pendingSpace = false
        pendingNewline = false
    }

    fun cutSegment() {
        val text = current.toString().trim()
        if (text.isNotEmpty()) segments += text
        current.clear()
        pendingSpace = false
        pendingNewline = false
    }

    var depth = 1
    var event = parser.next()
    while (depth > 0) {
        when (event) {
            XmlPullParser.START_TAG -> {
                depth++
                if (parser.name == "span") {
                    val isBg = currentBg || parser.attr("ttm:role") == "x-bg"
                    if (isBg != currentBg) {
                        cutSegment()
                        currentBg = isBg
                    }
                    bgStack.addLast(currentBg)
                    flushSpace()
                }
            }

            XmlPullParser.TEXT -> parser.text.forEach { c ->
                if (c.isWhitespace()) {
                    pendingSpace = true
                    if (c == '\n' || c == '\r') pendingNewline = true
                } else {
                    flushSpace()
                    current.append(c)
                }
            }

            XmlPullParser.END_TAG -> {
                depth--
                if (parser.name == "span" && bgStack.isNotEmpty()) {
                    bgStack.removeLast()
                    val outerBg = bgStack.lastOrNull() ?: false
                    if (outerBg != currentBg) {
                        cutSegment()
                        currentBg = outerBg
                    }
                }
            }
        }
        if (depth > 0) event = parser.next()
    }
    cutSegment()
    return segments
}

private class TtmlAgents {
    private val types = mutableMapOf<String, String>()
    private val order = mutableListOf<String>()

    fun register(id: String?, type: String?) {
        if (id != null && type != null) types[id] = type
    }

    fun voiceFor(agentId: String?): Int {
        if (agentId == null) return VOICE_PRIMARY
        if (types[agentId] == "group") return VOICE_GROUP
        if (agentId !in order) order += agentId
        return if (order.indexOf(agentId) % 2 == 0) VOICE_PRIMARY else VOICE_SECONDARY
    }

    fun singerFor(agentId: String?): Int {
        if (agentId == null) return 0
        if (types[agentId] == "group") return -1
        if (agentId !in order) order += agentId
        return order.indexOf(agentId)
    }
}

fun parseTtml(raw: String): Lyrics? = runCatching {
    val parser = Xml.newPullParser()
    parser.setInput(raw.reader())
    val agents = TtmlAgents()
    val lines = mutableListOf<LyricLine>()

    var inLine = false
    var lineBegin = -1L
    var lineEnd = -1L
    var lineVoice = VOICE_PRIMARY
    var lineSinger = 0
    var lineSection = false
    var newSection = false
    var divAgent: String? = null
    var pendingSpace = false
    var pendingNewline = false
    var lineKey: String? = null

    val translations = mutableMapOf<String, List<String>>()
    val transliterations = mutableMapOf<String, Transliteration>()
    var inTranslation = false
    var inTransliteration = false
    var capturedTranslation = false
    var capturedTransliteration = false

    val text = StringBuilder()
    val words = mutableListOf<LyricWord>()
    val spanStack = ArrayDeque<SpanFrame>()

    fun flushSpace() {
        if (pendingSpace && !pendingNewline && text.isNotEmpty()) text.append(' ')
        pendingSpace = false
        pendingNewline = false
    }

    fun appendLyricText(chunk: String) {
        chunk.forEach { c ->
            if (c.isWhitespace()) {
                pendingSpace = true
                if (c == '\n' || c == '\r') pendingNewline = true
            } else {
                flushSpace()
                text.append(c)
            }
        }
    }

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "ttm:agent", "agent" ->
                    agents.register(parser.attr("xml:id"), parser.attr("type"))

                "div" -> {
                    newSection = true
                    divAgent = parser.attr("ttm:agent")
                }

                "br" -> if (inLine) {
                    pendingSpace = false
                    pendingNewline = false
                    text.append('\n')
                }

                "p" -> {
                    inLine = true
                    text.clear()
                    words.clear()
                    spanStack.clear()
                    pendingSpace = false
                    pendingNewline = false
                    lineBegin = parseTtmlTime(parser.attr("begin"))
                    lineEnd = parseTtmlTime(parser.attr("end"))
                    lineKey = parser.attr("itunes:key")
                    val agentId = parser.attr("ttm:agent") ?: divAgent
                    lineVoice = agents.voiceFor(agentId)
                    lineSinger = agents.singerFor(agentId)
                    lineSection = newSection
                    newSection = false
                }

                "translation" -> if (!capturedTranslation) {
                    inTranslation = true
                    capturedTranslation = true
                }

                "transliteration" -> if (!capturedTransliteration) {
                    inTransliteration = true
                    capturedTransliteration = true
                }

                "text" -> {
                    val forKey = parser.attr("for")
                    if (forKey != null && inTranslation) {
                        val segments = readTranslationSegments(parser)
                        if (segments.isNotEmpty()) translations[forKey] = segments
                    } else if (forKey != null && inTransliteration) {
                        val (content, spanWords) = readTimedText(parser)
                        if (content.isNotBlank()) transliterations[forKey] = Transliteration(content, spanWords)
                    }
                }

                "span" -> if (inLine) {
                    spanStack.lastOrNull()?.hadChild = true
                    flushSpace()
                    val role = parser.attr("ttm:role")
                    val parentBackground = spanStack.lastOrNull()?.background == true
                    spanStack.addLast(
                        SpanFrame(
                            beginMs = parseTtmlTime(parser.attr("begin")),
                            endMs = parseTtmlTime(parser.attr("end")),
                            textStart = text.length,
                            background = parentBackground || role == "x-bg",
                        )
                    )
                }
            }

            XmlPullParser.TEXT -> if (inLine) appendLyricText(parser.text)

            XmlPullParser.END_TAG -> when (parser.name) {
                "translation" -> inTranslation = false
                "transliteration" -> inTransliteration = false

                "span" -> if (inLine && spanStack.isNotEmpty()) {
                    val frame = spanStack.removeLast()
                    val isWord = !frame.hadChild &&
                        frame.beginMs >= 0 &&
                        text.length > frame.textStart
                    if (isWord) {
                        words += LyricWord(
                            startMs = frame.beginMs,
                            endMs = frame.endMs,
                            start = frame.textStart,
                            end = text.length,
                            background = frame.background,
                        )
                    }
                }

                "p" -> if (inLine) {
                    inLine = false
                    val lineText = text.toString()
                    if (lineText.isNotEmpty()) {
                        val bounded = words
                            .map { word ->
                                word.copy(
                                    start = word.start.coerceIn(0, lineText.length),
                                    end = word.end.coerceIn(0, lineText.length),
                                )
                            }
                            .filter { it.end > it.start }
                        lines += LyricLine(
                            startMs = lineBegin,
                            endMs = lineEnd,
                            text = lineText,
                            words = bounded,
                            voice = lineVoice,
                            singer = lineSinger,
                            sectionStart = lineSection,
                            translation = lineKey?.let { translations[it] } ?: emptyList(),
                            transliteration = lineKey?.let { transliterations[it] },
                        )
                    }
                }
            }
        }
        event = parser.next()
    }

    if (lines.isEmpty()) return null
    val synced = lines.all { it.startMs >= 0 }
    if (!synced) return Lyrics(lines, false)
    Lyrics(
        lines = lines.sortedBy { it.startMs }
            .fillLineEnds()
            .mergeSimultaneousDuplicates()
            .withInterludes(),
        synced = true,
    )
}.getOrNull()
