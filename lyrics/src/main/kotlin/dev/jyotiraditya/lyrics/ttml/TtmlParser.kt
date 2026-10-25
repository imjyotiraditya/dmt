package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.LyricWord
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.TimedText
import dev.jyotiraditya.lyrics.assignSides
import dev.jyotiraditya.lyrics.fillLineEnds
import dev.jyotiraditya.lyrics.markInstrumentalLines
import dev.jyotiraditya.lyrics.mergeSimultaneousDuplicates
import dev.jyotiraditya.lyrics.stripBackgroundBrackets
import dev.jyotiraditya.lyrics.withInterludes
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.newGenericReader
import nl.adaptivity.xmlutil.xmlStreaming

/**
 * Parses Apple-style lyric TTML: `<p>` lines in `<div>` sections with words timed by `<span>`,
 * singers from `ttm:agent`, backing vocals as `x-bg` spans, and translations and readings either
 * inline or in blocks keyed by `itunes:key`.
 */
object TtmlParser {

    /** The lyrics in [raw], or null if it is not well-formed or holds no lines. */
    fun parse(raw: String): Lyrics? = try {
        TtmlDocumentReader(xmlStreaming.newGenericReader(raw)).read()
    } catch (_: XmlException) {
        null
    }
}

/** A `<p>` as read, before the translations its key names are attached. */
private data class Paragraph(
    val line: LyricLine,
    val key: String?,
    val inlineTranslations: List<TimedText>,
    val inlineReading: TimedText?,
)

private class OpenSpan(
    val beginMs: Long,
    val endMs: Long,
    val textStart: Int,
    val background: Boolean,
) {
    var hasChildSpans = false
}

private class TtmlDocumentReader(private val xml: XmlReader) {
    private val singers = Singers()
    private val paragraphs = mutableListOf<Paragraph>()
    private val translations = HashMap<String, MutableList<TimedText>>()
    private val readings = HashMap<String, TimedText>()

    private var sectionAgent: String? = null
    private var sectionStarting = false

    fun read(): Lyrics? {
        while (xml.hasNext()) {
            if (xml.next() != EventType.START_ELEMENT) continue
            when (xml.localName) {
                TtmlNames.AGENT ->
                    singers.declare(
                        attribute(TtmlNames.ID),
                        attribute(TtmlNames.TYPE),
                    )

                TtmlNames.DIV -> {
                    sectionStarting = true
                    sectionAgent = attribute(TtmlNames.AGENT)
                }

                TtmlNames.P -> readParagraph()?.let(paragraphs::add)

                TtmlNames.TRANSLATION -> readTranslationBlock(attribute(TtmlNames.LANG))

                TtmlNames.TRANSLITERATION -> readTransliterationBlock()
            }
        }

        val lines = paragraphs.map { it.resolved() }
        if (lines.isEmpty()) return null
        if (lines.any { it.startMs < 0 }) return Lyrics(lines.assignSides(), synced = false)
        return Lyrics(
            lines =
                lines
                    .sortedBy { it.startMs }
                    .markInstrumentalLines()
                    .stripBackgroundBrackets()
                    .fillLineEnds()
                    .mergeSimultaneousDuplicates()
                    .assignSides()
                    .withInterludes(),
            synced = true,
        )
    }

    /** The line with the translations and reading its key names, falling back to inline ones. */
    private fun Paragraph.resolved(): LyricLine = line.copy(
        translation = key?.let(translations::get).orEmpty() + inlineTranslations,
        transliteration =
            (key?.let(readings::get) ?: inlineReading)?.takeUnless {
                it.text.isSameTextAs(line.text)
            },
    )

    /** Reads the `<p>` the reader is at through its end, or null if it holds no text. */
    private fun readParagraph(): Paragraph? {
        val beginMs = parseClockTime(attribute(TtmlNames.BEGIN))
        val endMs = parseClockTime(attribute(TtmlNames.END))
        val key = attribute(TtmlNames.KEY)
        val agent = attribute(TtmlNames.AGENT) ?: sectionAgent
        val voice = singers.voiceOf(agent)
        val singer = singers.indexOf(agent)
        val sectionStart = sectionStarting
        sectionStarting = false

        val text = SpokenText()
        val words = mutableListOf<LyricWord>()
        val spans = ArrayDeque<OpenSpan>()
        val inlineTranslations = mutableListOf<TimedText>()
        var inlineReading: TimedText? = null

        readContent(
            onText = text::append,
            onStart = { name ->
                val role = if (name == TtmlNames.SPAN) attribute(TtmlNames.ROLE) else null
                when {
                    name == TtmlNames.BR -> {
                        text.breakLine()
                        false
                    }

                    name != TtmlNames.SPAN -> false

                    role == TtmlNames.ROLE_TRANSLATION -> {
                        val lang = attribute(TtmlNames.LANG)
                        val content = readTimedText().text
                        if (content.isNotBlank()) {
                            inlineTranslations += TimedText(content, lang = lang)
                        }
                        true
                    }

                    role == TtmlNames.ROLE_ROMANIZATION -> {
                        val content = readTimedText().text
                        if (content.isNotBlank()) inlineReading = TimedText(content)
                        true
                    }

                    else -> {
                        val parent = spans.lastOrNull()
                        parent?.hasChildSpans = true
                        text.flushSpace()
                        spans.addLast(
                            OpenSpan(
                                beginMs = parseClockTime(attribute(TtmlNames.BEGIN)),
                                endMs = parseClockTime(attribute(TtmlNames.END)),
                                textStart = text.length,
                                background =
                                    parent?.background == true || role == TtmlNames.ROLE_BACKGROUND,
                            ),
                        )
                        false
                    }
                }
            },
            onEnd = { name ->
                if (name == TtmlNames.SPAN) {
                    val span = spans.removeLastOrNull()
                    // Only innermost timed spans are words; a span around others only groups them.
                    if (
                        span != null &&
                        !span.hasChildSpans &&
                        span.beginMs >= 0 &&
                        text.length > span.textStart
                    ) {
                        words +=
                            LyricWord(
                                span.beginMs,
                                span.endMs,
                                span.textStart,
                                text.length,
                                span.background,
                            )
                    }
                }
            },
        )

        val lineText = text.toString()
        if (lineText.isEmpty()) return null
        return Paragraph(
            line =
                LyricLine(
                    startMs = minOf(beginMs, words.minOfOrNull { it.startMs } ?: beginMs),
                    endMs = maxOf(endMs, words.maxOfOrNull { it.endMs } ?: endMs),
                    text = lineText,
                    words = words,
                    voice = voice,
                    singer = singer,
                    sectionStart = sectionStart,
                ),
            key = key,
            inlineTranslations = inlineTranslations,
            inlineReading = inlineReading,
        )
    }

    private fun readTranslationBlock(lang: String?) = readContent(
        onStart = { name ->
            if (name != TtmlNames.TEXT) return@readContent false
            val key = attribute(TtmlNames.FOR)
            val segments = readTranslationSegments()
            if (key != null && segments.isNotEmpty()) {
                translations.getOrPut(key) { mutableListOf() } += segments.map {
                    TimedText(it, lang = lang)
                }
            }
            true
        },
    )

    /** Reads a `<transliteration>` block, keeping the first reading given for each line. */
    private fun readTransliterationBlock() = readContent(
        onStart = { name ->
            if (name != TtmlNames.TEXT) return@readContent false
            val key = attribute(TtmlNames.FOR)
            val reading = readTimedText()
            if (key != null && reading.text.isNotBlank()) readings.putIfAbsent(key, reading)
            true
        },
    )

    private fun readTimedText(): TimedText {
        val text = SpokenText()
        val words = mutableListOf<LyricWord>()
        val spans = ArrayDeque<OpenSpan>()
        readContent(
            onText = text::append,
            onStart = { name ->
                if (name == TtmlNames.SPAN) {
                    text.flushSpace()
                    spans.addLast(
                        OpenSpan(
                            beginMs = parseClockTime(attribute(TtmlNames.BEGIN)),
                            endMs = parseClockTime(attribute(TtmlNames.END)),
                            textStart = text.length,
                            background = false,
                        ),
                    )
                }
                false
            },
            onEnd = { name ->
                val span = if (name == TtmlNames.SPAN) spans.removeLastOrNull() else null
                if (span != null && span.beginMs >= 0 && text.length > span.textStart) {
                    words +=
                        LyricWord(
                            span.beginMs,
                            span.endMs,
                            span.textStart,
                            text.length,
                            background = false,
                        )
                }
            },
        )
        return TimedText(text.take(), words)
    }

    /** The translated text, cut into one segment per stretch of lead or backing vocals. */
    private fun readTranslationSegments(): List<String> {
        val segments = mutableListOf<String>()
        val text = SpokenText()
        val backgroundStack = ArrayDeque<Boolean>()
        var background = false

        fun switchTo(nextBackground: Boolean) {
            if (nextBackground == background) return
            text.take().takeIf(String::isNotEmpty)?.let(segments::add)
            background = nextBackground
        }

        readContent(
            onText = text::append,
            onStart = { name ->
                if (name == TtmlNames.SPAN) {
                    switchTo(background || attribute(TtmlNames.ROLE) == TtmlNames.ROLE_BACKGROUND)
                    backgroundStack.addLast(background)
                    text.flushSpace()
                }
                false
            },
            onEnd = { name ->
                if (name == TtmlNames.SPAN && backgroundStack.removeLastOrNull() != null) {
                    switchTo(backgroundStack.lastOrNull() ?: false)
                }
            },
        )
        text.take().takeIf(String::isNotEmpty)?.let(segments::add)
        return segments
    }

    /**
     * Walks the element the reader is at through its end tag. [onStart] returns whether it read a
     * nested element through its own end; [onEnd] is called for the rest.
     */
    private inline fun readContent(
        onText: (String) -> Unit = {},
        onStart: (name: String) -> Boolean,
        onEnd: (name: String) -> Unit = {},
    ) {
        var depth = 1
        while (depth > 0) {
            when (xml.next()) {
                EventType.START_ELEMENT -> if (!onStart(xml.localName)) depth++

                EventType.END_ELEMENT -> if (--depth > 0) onEnd(xml.localName)

                EventType.TEXT,
                EventType.IGNORABLE_WHITESPACE,
                EventType.CDSECT,
                -> onText(xml.text)

                EventType.END_DOCUMENT -> throw XmlException("Document ended inside an element")

                else -> Unit
            }
        }
    }

    private fun String.isSameTextAs(other: String): Boolean =
        filter(Char::isLetterOrDigit).equals(other.filter(Char::isLetterOrDigit), ignoreCase = true)

    /** The current element's attribute named [localName], in any namespace. */
    private fun attribute(localName: String): String? = (0 until xml.attributeCount)
        .firstOrNull { xml.getAttributeLocalName(it) == localName }
        ?.let { xml.getAttributeValue(it) }
}
