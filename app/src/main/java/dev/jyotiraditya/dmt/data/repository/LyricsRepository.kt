package dev.jyotiraditya.dmt.data.repository

import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.util.allFilesAccess
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.LyricsParser
import dev.jyotiraditya.lyrics.LyricsTags
import dev.jyotiraditya.metadata.AudioTags
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val SIDECAR_EXTENSIONS = listOf("ttml", "lrc", "txt")

/** Reads the lyrics that a track carries or that sit beside it. */
@Singleton
class LyricsRepository @Inject constructor() {

    /**
     * Returns the lyrics of the track at [path] from [source], or null if that source has none.
     *
     * [LyricsSource.LRCLIB] is fetched over the network rather than read, so it is never tried here.
     */
    fun lyricsFor(path: String, source: LyricsSource): Lyrics? = when (source) {
        LyricsSource.EMBEDDED -> embeddedLyrics(path)
        LyricsSource.LOCAL -> sidecarLyrics(path)
        LyricsSource.LRCLIB -> null
    }

    private fun embeddedLyrics(path: String): Lyrics? =
        LyricsTags.bestOf(AudioTags.read(path))?.let(LyricsParser::parse)

    private fun sidecarLyrics(path: String): Lyrics? {
        if (!allFilesAccess) return null

        val track = File(path)

        return SIDECAR_EXTENSIONS
            .asSequence()
            .map { File(track.parentFile, "${track.nameWithoutExtension}.$it") }
            .mapNotNull { file -> runCatching { file.readText() }.getOrNull() }
            .firstNotNullOfOrNull(LyricsParser::parse)
    }
}
