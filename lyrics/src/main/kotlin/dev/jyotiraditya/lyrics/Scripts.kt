package dev.jyotiraditya.lyrics

import java.lang.Character.UnicodeScript

private val NEUTRAL_SCRIPTS =
    setOf(UnicodeScript.COMMON, UnicodeScript.INHERITED, UnicodeScript.UNKNOWN)

private val JAPANESE_SCRIPTS =
    setOf(UnicodeScript.HAN, UnicodeScript.HIRAGANA, UnicodeScript.KATAKANA)

/** The script of the first letter in [text], counting Japanese scripts as one, or null if none. */
internal fun scriptOf(text: String): UnicodeScript? {
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val script = UnicodeScript.of(codePoint)
        if (script !in NEUTRAL_SCRIPTS) {
            return if (script in JAPANESE_SCRIPTS) UnicodeScript.HAN else script
        }
        index += Character.charCount(codePoint)
    }
    return null
}

/** Whether [reading] is [original] written in another script, such as a romanization. */
internal fun isReadingOf(original: String, reading: String): Boolean {
    val originalScript = scriptOf(original) ?: return false
    val readingScript = scriptOf(reading) ?: return false
    return originalScript != readingScript
}
