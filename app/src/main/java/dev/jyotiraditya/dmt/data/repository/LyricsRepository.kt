package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.library.MetadataReader
import dev.jyotiraditya.dmt.util.allFilesAccess
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.LyricsParser
import dev.jyotiraditya.lyrics.LyricsTags
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val SIDECAR_EXTENSIONS = listOf("ttml", "lrc", "txt")

/** Reads the lyrics that a track carries or that sit beside it. */
@Singleton
class LyricsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * Returns the lyrics of the track at [path] from [source], or null if that source has none.
     *
     * [LyricsSource.LRCLIB] is fetched over the network rather than read, so it is never tried here.
     */
    suspend fun lyricsFor(path: String, source: LyricsSource): Lyrics? = when (source) {
        LyricsSource.EMBEDDED -> embeddedLyrics(path)
        LyricsSource.LOCAL -> sidecarLyrics(path)
        LyricsSource.LRCLIB -> null
    }

    @OptIn(UnstableApi::class)
    private suspend fun embeddedLyrics(path: String): Lyrics? =
        LyricsTags.bestOf(MetadataReader.readTags(context, Uri.fromFile(File(path))))
            ?.let(LyricsParser::parse)

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
