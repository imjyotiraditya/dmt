package dev.jyotiraditya.dmt.domain.model

/**
 * Where the lyrics of a track come from.
 *
 * The sources are declared in the order that a track is read when the listener has not picked one,
 * so that what a track carries is preferred over what has to be fetched.
 */
enum class LyricsSource {
    EMBEDDED,
    LOCAL,
    LRCLIB,
    ;

    companion object {
        /** The source to read from until the listener picks another. */
        val DEFAULT: LyricsSource = EMBEDDED
    }
}
