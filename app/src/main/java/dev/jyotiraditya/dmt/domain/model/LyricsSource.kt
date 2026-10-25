package dev.jyotiraditya.dmt.domain.model

/** Where lyrics come from, in fallback order; an [online] source is asked only when picked. */
enum class LyricsSource(val online: Boolean) {
    EMBEDDED(online = false),
    LOCAL(online = false),
    LRCLIB(online = true),
    LRC_RED(online = true),
    ;

    companion object {
        val DEFAULT: LyricsSource = EMBEDDED
    }
}
