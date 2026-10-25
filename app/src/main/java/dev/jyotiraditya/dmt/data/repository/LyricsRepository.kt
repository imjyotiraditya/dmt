package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.net.Uri
import android.util.LruCache
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

private const val CACHE_TRACKS = 48

/** Reads embedded lyrics and lyrics files stored next to a track. */
@Singleton
class LyricsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    /** The lyrics of the track at [path] from the local [source], or null if it has none. */
    suspend fun lyricsFor(path: String, source: LyricsSource): Lyrics? {
        val track = File(path)
        val key = "$source:$path"
        return when (source) {
            LyricsSource.EMBEDDED -> cached(key, listOf(track)) { embeddedLyrics(track) }

            LyricsSource.LOCAL -> sidecarFiles(track).let { cached(key, it) { sidecarLyrics(it) } }

            LyricsSource.LRCLIB,
            LyricsSource.LRC_RED,
            -> null
        }
    }

    /** The lyrics kept under [key] while [files] stay unchanged, or else those [read] returns. */
    private inline fun cached(key: String, files: List<File>, read: () -> Lyrics?): Lyrics? {
        val stamp = files.map(File::lastModified)
        val held = cache.get(key)
        if (held != null && held.stamp == stamp) return held.lyrics
        return read().also { cache.put(key, Read(stamp, it)) }
    }

    /**
     * One read of a source; null lyrics when it had none. [stamp] holds when each file it read was
     * last changed, 0 for a file that was not there.
     */
    private data class Read(val stamp: List<Long>, val lyrics: Lyrics?)

    /** Reads by source and path, misses included, kept while their files stay unchanged. */
    private val cache = LruCache<String, Read>(CACHE_TRACKS * LyricsSource.entries.size)

    @OptIn(UnstableApi::class)
    private suspend fun embeddedLyrics(track: File): Lyrics? =
        LyricsTags.bestOf(MetadataReader.readTags(context, Uri.fromFile(track)))
            ?.let(LyricsParser::parse)

    /** The lyrics files that may sit next to [track], richest format first. */
    private fun sidecarFiles(track: File): List<File> {
        if (!allFilesAccess) return emptyList()
        return SIDECAR_EXTENSIONS.map {
            File(track.parentFile, "${track.nameWithoutExtension}.$it")
        }
    }

    private fun sidecarLyrics(files: List<File>): Lyrics? = files
        .asSequence()
        .mapNotNull { file -> runCatching { file.readText() }.getOrNull() }
        .firstNotNullOfOrNull(LyricsParser::parse)
}
