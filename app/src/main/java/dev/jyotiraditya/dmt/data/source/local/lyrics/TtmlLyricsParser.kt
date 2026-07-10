package dev.jyotiraditya.dmt.data.source.local.lyrics

import android.util.Xml
import dev.jyotiraditya.dmt.domain.model.LyricLine
import dev.jyotiraditya.dmt.domain.model.LyricWord
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.Transliteration
import dev.jyotiraditya.dmt.domain.model.Voice
import org.xmlpull.v1.XmlPullParser

object TtmlLyricsParser {

    private const val ROLE_BACKGROUND = "x-bg"
    private const val AGENT_TYPE_GROUP = "group"

    fun parse(raw: String): Lyrics? =
        runCatching {
            val parser = Xml.newPullParser()
            parser.setInput(raw.reader())

            val agents = Agents()
            val lines = mutableListOf<LyricLine>()

            var inLine = false
            var lineBegin = -1L
            var lineEnd = -1L
            var lineVoice = Voice.PRIMARY
            var lineSinger = 0
            var lineSection = false
            var newSection = false
            var divAgent: String? = null
            var pendingSpace = false
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
                if (pendingSpace && text.isNotEmpty() && text.last() != '\n') text.append(' ')
                pendingSpace = false
            }

            fun appendLyricText(chunk: String) {
                if (isFormattingOnly(chunk)) return

                chunk.forEach { c ->
                    if (c.isWhitespace()) {
                        pendingSpace = true
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
                            text.append('\n')
                        }

                        "p" -> {
                            inLine = true
                            text.clear()
                            words.clear()
                            spanStack.clear()
                            pendingSpace = false

                            lineBegin = parseTime(parser.attr("begin"))
                            lineEnd = parseTime(parser.attr("end"))
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

                                if (content.isNotBlank()) {
                                    transliterations[forKey] = Transliteration(
                                        text = content,
                                        words = spanWords,
                                    )
                                }
                            }
                        }

                        "span" -> if (inLine) {
                            spanStack.lastOrNull()?.hadChild = true
                            flushSpace()

                            val role = parser.attr("ttm:role")
                            val parentBackground = spanStack.lastOrNull()?.background == true

                            spanStack.addLast(
                                SpanFrame(
                                    beginMs = parseTime(parser.attr("begin")),
                                    endMs = parseTime(parser.attr("end")),
                                    textStart = text.length,
                                    background = parentBackground || role == ROLE_BACKGROUND,
                                ),
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
            if (!synced) {
                return Lyrics(
                    lines = lines.alternateVoices(),
                    synced = false,
                )
            }

            Lyrics(
                lines = lines.sortedBy { it.startMs }
                    .fillLineEnds()
                    .mergeSimultaneousDuplicates()
                    .alternateVoices()
                    .withInterludes(),
                synced = true,
            )
        }.getOrNull()

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

    private fun parseTime(value: String?): Long {
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

    private fun isFormattingOnly(chunk: String): Boolean =
        chunk.isNotEmpty() &&
                chunk.all { it.isWhitespace() } &&
                chunk.any { it == '\n' || it == '\r' }

    private fun readTimedText(parser: XmlPullParser): Pair<String, List<LyricWord>> {
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
                XmlPullParser.START_TAG -> {
                    depth++

                    if (parser.name == "span") {
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

                XmlPullParser.TEXT -> if (!isFormattingOnly(parser.text)) {
                    parser.text.forEach { c ->
                        if (c.isWhitespace()) {
                            pendingSpace = true
                        } else {
                            flushSpace()
                            text.append(c)
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    depth--

                    if (parser.name == "span" && spanStack.isNotEmpty()) {
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
                XmlPullParser.START_TAG -> {
                    depth++

                    if (parser.name == "span") {
                        val isBg = currentBg || parser.attr("ttm:role") == ROLE_BACKGROUND

                        if (isBg != currentBg) {
                            cutSegment()
                            currentBg = isBg
                        }

                        bgStack.addLast(currentBg)
                        flushSpace()
                    }
                }

                XmlPullParser.TEXT -> if (!isFormattingOnly(parser.text)) {
                    parser.text.forEach { c ->
                        if (c.isWhitespace()) {
                            pendingSpace = true
                        } else {
                            flushSpace()
                            current.append(c)
                        }
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

    private class Agents {

        private val types = mutableMapOf<String, String>()
        private val order = mutableListOf<String>()

        fun register(id: String?, type: String?) {
            if (id == null) return
            if (type != null) types[id] = type
            if (types[id] != AGENT_TYPE_GROUP && id !in order) order += id
        }

        fun voiceFor(agentId: String?): Voice {
            if (agentId == null) return Voice.PRIMARY
            return if (types[agentId] == AGENT_TYPE_GROUP) Voice.GROUP else Voice.PRIMARY
        }

        fun singerFor(agentId: String?): Int {
            if (agentId == null) return 0
            if (types[agentId] == AGENT_TYPE_GROUP) return -1

            if (agentId !in order) order += agentId

            return order.indexOf(agentId)
        }
    }
}
