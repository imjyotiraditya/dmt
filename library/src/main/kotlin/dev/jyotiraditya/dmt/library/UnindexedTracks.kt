package dev.jyotiraditya.dmt.library

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.FileTypes
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.TimeUnit.MILLISECONDS

private const val NO_MEDIA = ".nomedia"

/** How many files to read at once, past which reading more at a time stops helping. */
private const val MAX_PARALLEL_READS = 8

private val SKIPPED_DIRS = setOf("Android/data", "Android/obb")

/**
 * Reads the tracks that the platform stores but does not index, which are the formats it cannot
 * decode itself. Their metadata is read from the files, so tracks that have not changed since the
 * last scan are taken from [known] rather than read again.
 */
@UnstableApi
object UnindexedTracks {

    suspend fun query(
        context: Context,
        blocked: Set<String>,
        known: Map<String, LibraryTrack>,
    ): List<LibraryTrack> {
        val playable = context.unclassifiedFiles()
            .filterNot { file -> SKIPPED_DIRS.any { file.absolutePath.contains(it) } }
            .filterNot { it.absolutePath.contains("/.") }
            .filterNot { it.parentFile?.absolutePath in blocked }
            .filterNot { File(it.parentFile, NO_MEDIA).exists() }
            .mapNotNull { file -> file.mime()?.let { file to it } }

        val inFlight = Semaphore(MAX_PARALLEL_READS)

        return coroutineScope {
            playable
                .map { (file, mime) ->
                    async {
                        known[file.absolutePath]?.takeIf { it.matches(file) }
                            ?: file.toTrack(
                                mime,
                                inFlight.withPermit { MetadataReader.read(context, file.toUri()) },
                            )
                    }
                }
                .awaitAll()
        }
    }

    /** Returns the files that the platform stores but could not classify. */
    private fun Context.unclassifiedFiles(): List<File> =
        contentResolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            arrayOf(MediaStore.Files.FileColumns.DATA),
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?",
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString()),
            null,
        )?.use { cursor ->
            buildList {
                val path = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    add(File(cursor.getString(path)))
                }
            }
        }.orEmpty()

    /** Returns the type of a file that an extractor can read, or null for anything else. */
    private fun File.mime(): String? = when (FileTypes.inferFileTypeFromUri(toUri())) {
        FileTypes.APE -> MimeTypes.AUDIO_APE
        FileTypes.TAK -> MimeTypes.AUDIO_TAK
        FileTypes.TTA -> MimeTypes.AUDIO_TTA
        FileTypes.WAVPACK -> MimeTypes.AUDIO_WAVPACK
        else -> null
    }

    private fun LibraryTrack.matches(file: File): Boolean =
        size == file.length() && dateModified == MILLISECONDS.toSeconds(file.lastModified())

    private fun File.toTrack(mime: String, metadata: TrackMetadata): LibraryTrack {
        val size = length()
        val modified = MILLISECONDS.toSeconds(lastModified())

        return LibraryTrack(
            id = TrackIds.of(absolutePath),
            uri = toUri(),
            title = metadata.title ?: nameWithoutExtension,
            artist = metadata.artist ?: UNKNOWN_ARTIST,
            albumArtist = "",
            album = metadata.album ?: parentFile?.name ?: UNKNOWN_ALBUM,
            path = absolutePath,
            durationMs = metadata.durationMs,
            mime = mime,
            bitrate = bitrate(size, metadata.durationMs),
            size = size,
            trackNumber = metadata.trackNumber,
            discNumber = metadata.discNumber,
            dateAdded = modified,
            dateModified = modified,
            coverUri = null,
        )
    }

    private fun bitrate(size: Long, durationMs: Long): Int =
        if (durationMs > 0) {
            (size * C.BITS_PER_BYTE * C.MILLIS_PER_SECOND / durationMs).toInt()
        } else {
            0
        }
}
