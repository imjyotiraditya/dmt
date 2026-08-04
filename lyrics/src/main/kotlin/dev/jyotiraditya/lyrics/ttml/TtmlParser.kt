package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.LyricWord
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.TimedText
import dev.jyotiraditya.lyrics.Voice
import dev.jyotiraditya.lyrics.alternateVoices
import dev.jyotiraditya.lyrics.fillLineEnds
import dev.jyotiraditya.lyrics.markInstrumentalLines
import dev.jyotiraditya.lyrics.mergeSimultaneousDuplicates
import dev.jyotiraditya.lyrics.withInterludes
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.newGenericReader
import nl.adaptivity.xmlutil.xmlStreaming

/**
 * Parses the Apple Music lyric-TTML dialect, the kind amll/syncedlyrics-style
 * community uploads use: `<p>` lines under `<div>` sections, word spans timed
 * with `begin`/`end`, plus a few extra conventions:
 *
 * - `ttm:agent` in `<head><metadata>` declares each voice. A `<p ttm:agent="...">`
 *   (or its enclosing `<div ttm:agent="...">`) assigns that line a stable
 *   [LyricLine.singer] index in first-seen order. An agent of `type="group"`
 *   (ensemble/choir) maps to [Voice.GROUP] instead of a singer index.
 * - `ttm:role="x-bg"` on a span marks backing/adlib vocals.
 * - There are two ways a source attaches a reading or translation to a line,
 *   and we handle both. The block-level way is a `<translation>`/`<transliteration>`
 *   section with `<text for="#lineKey">` entries: one `<translation>` block per
 *   language, tagged with its own `xml:lang`, and only the first `<transliteration>`
 *   block per line is kept since these files never seem to carry more than one
 *   romanization. The more common way in community (amll-style) uploads is
 *   `ttm:role="x-translation"` and `ttm:role="x-roman"` spans sitting right inside
 *   the `<p>`, next to the timed word spans: one `x-translation` span per language,
 *   at most one `x-roman` span. Either way the result ends up in
 *   [LyricLine.translation] or [LyricLine.transliteration], never mixed into the
 *   line's sung [LyricLine.text].
 */
object TtmlParser {

    /**
     * Returns the lyrics [raw] holds.
     *
     * @param raw The document to read.
     * @return The lyrics, or null if the document cannot be read.
     */
    fun parse(raw: String): Lyrics? =
        runCatching {
            val parser = xmlStreaming.newGenericReader(raw)

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

            val translations = mutableMapOf<String, List<TimedText>>()
            val transliterations = mutableMapOf<String, TimedText>()
            var inTranslation = false
            var inTransliteration = false
            var currentTranslationLang: String? = null

            val text = StringBuilder()
            val words = mutableListOf<LyricWord>()
            val spanStack = ArrayDeque<SpanFrame>()
            val pendingTranslations = mutableListOf<TimedText>()
            var pendingTransliteration: TimedText? = null

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

            var event = parser.next()
            while (event != EventType.END_DOCUMENT) {
                when (event) {
                    EventType.START_ELEMENT -> when (parser.localName) {
                        "agent" ->
                            agents.register(parser.attr("id"), parser.attr("type"))

                        "div" -> {
                            newSection = true
                            divAgent = parser.attr("agent")
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
                            pendingTranslations.clear()
                            pendingTransliteration = null

                            lineBegin = parseTime(parser.attr("begin"))
                            lineEnd = parseTime(parser.attr("end"))
                            lineKey = parser.attr("key")

                            val agentId = parser.attr("agent") ?: divAgent
                            lineVoice = agents.voiceFor(agentId)
                            lineSinger = agents.singerFor(agentId)

                            lineSection = newSection
                            newSection = false
                        }

                        "translation" -> {
                            inTranslation = true
                            currentTranslationLang = parser.attr("lang")
                        }

                        "transliteration" -> inTransliteration = true

                        "text" -> {
                            val forKey = parser.attr("for")

                            if (forKey != null && inTranslation) {
                                val segments = readTranslationSegments(parser)
                                if (segments.isNotEmpty()) {
                                    val lang = currentTranslationLang
                                    translations[forKey] = (translations[forKey] ?: emptyList()) +
                                        segments.map { TimedText(text = it, lang = lang) }
                                }
                            } else if (forKey != null && inTransliteration) {
                                val (content, spanWords) = readTimedText(parser)

                                if (content.isNotBlank() && forKey !in transliterations) {
                                    transliterations[forKey] = TimedText(
                                        text = content,
                                        words = spanWords,
                                    )
                                }
                            }
                        }

                        "span" -> if (inLine) {
                            val role = parser.attr("role")

                            when (role) {
                                ROLE_TRANSLATION -> {
                                    val lang = parser.attr("lang")
                                    val (content, _) = readTimedText(parser)
                                    if (content.isNotBlank()) {
                                        pendingTranslations += TimedText(text = content, lang = lang)
                                    }
                                }

                                ROLE_ROMANIZATION -> {
                                    val (content, _) = readTimedText(parser)
                                    if (content.isNotBlank()) {
                                        pendingTransliteration = TimedText(text = content)
                                    }
                                }

                                else -> {
                                    spanStack.lastOrNull()?.hadChild = true
                                    flushSpace()

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
                        }
                    }

                    EventType.TEXT, EventType.IGNORABLE_WHITESPACE -> if (inLine) appendLyricText(parser.text)

                    EventType.END_ELEMENT -> when (parser.localName) {
                        "translation" -> {
                            inTranslation = false
                            currentTranslationLang = null
                        }

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
                                    translation = (lineKey?.let { translations[it] } ?: emptyList()) +
                                        pendingTranslations,
                                    transliteration = lineKey?.let { transliterations[it] }
                                        ?: pendingTransliteration,
                                )
                            }
                        }
                    }

                    else -> Unit
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
                    .markInstrumentalLines()
                    .fillLineEnds()
                    .mergeSimultaneousDuplicates()
                    .alternateVoices()
                    .withInterludes(),
                synced = true,
            )
        }.getOrNull()
}
