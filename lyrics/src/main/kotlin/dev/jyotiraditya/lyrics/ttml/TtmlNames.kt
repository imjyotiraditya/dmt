package dev.jyotiraditya.lyrics.ttml

/** Element, attribute and value names of Apple-style lyric TTML, matched by local name. */
internal object TtmlNames {
    const val AGENT = "agent"
    const val DIV = "div"
    const val P = "p"
    const val SPAN = "span"
    const val BR = "br"
    const val TEXT = "text"
    const val TRANSLATION = "translation"
    const val TRANSLITERATION = "transliteration"

    const val ID = "id"
    const val TYPE = "type"
    const val BEGIN = "begin"
    const val END = "end"
    const val KEY = "key"
    const val FOR = "for"
    const val LANG = "lang"
    const val ROLE = "role"

    /** The role of a span sung behind the line. */
    const val ROLE_BACKGROUND = "x-bg"

    const val ROLE_TRANSLATION = "x-translation"

    const val ROLE_ROMANIZATION = "x-roman"

    /** The agent type of a singer that stands for many, such as a choir. */
    const val AGENT_TYPE_GROUP = "group"
}
