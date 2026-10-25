package dev.jyotiraditya.lyrics.ttml

/**
 * Text gathered the way it is sung: whitespace runs collapse to one space, no space leads a line,
 * and whitespace that only indents the markup is dropped.
 */
internal class SpokenText {
    private val builder = StringBuilder()
    private var spacePending = false

    val length: Int
        get() = builder.length

    fun append(chunk: String) {
        if (chunk.isIndentation()) return
        for (char in chunk) {
            if (char.isWhitespace()) {
                spacePending = true
            } else {
                flushSpace()
                builder.append(char)
            }
        }
    }

    /** Writes the pending space unless it would lead a line; call it before opening a word. */
    fun flushSpace() {
        if (spacePending && builder.isNotEmpty() && builder.last() != '\n') builder.append(' ')
        spacePending = false
    }

    fun breakLine() {
        spacePending = false
        builder.append('\n')
    }

    /** The text so far, trimmed, starting over after. */
    fun take(): String {
        val text = builder.toString().trim()
        builder.clear()
        spacePending = false
        return text
    }

    override fun toString(): String = builder.toString()

    private fun String.isIndentation(): Boolean =
        isNotEmpty() && all(Char::isWhitespace) && any { it == '\n' || it == '\r' }
}
