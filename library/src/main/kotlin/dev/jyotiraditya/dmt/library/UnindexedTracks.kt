package dev.jyotiraditya.dmt.library

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val NO_MEDIA = ".nomedia"

/** Files read at once; more does not read faster. */
private const val MAX_PARALLEL_READS = 8

private val SKIPPED_DIRS = setOf("Android/data", "Android/obb")

@UnstableApi
private val MIME_BY_EXTENSION =
    mapOf(
        "ape" to MimeTypes.AUDIO_APE,
        "tak" to MimeTypes.AUDIO_TAK,
        "tta" to MimeTypes.AUDIO_TTA,
        "wv" to MimeTypes.AUDIO_WAVPACK,
    )

/** Tracks in formats the media store holds but does not index, read from the files themselves. */
@UnstableApi
object UnindexedTracks {

    /** The tracks, reusing [known] metadata for files unchanged since the last read. */
    suspend fun query(context: Context, known: Map<String, LibraryTrack>): List<LibraryTrack> {
        val noMedia = mutableMapOf<String, Boolean>()
        val playable =
            context
                .unclassifiedFiles()
                .filterNot { file -> SKIPPED_DIRS.any { file.absolutePath.contains(it) } }
                .filterNot { it.absolutePath.contains("/.") }
                .filterNot { file ->
                    val parent = file.parent ?: return@filterNot false
                    noMedia.getOrPut(parent) { File(parent, NO_MEDIA).exists() }
                }
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

    // Asking the media store for files it could not classify is far faster than walking storage.
    private fun Context.unclassifiedFiles(): List<File> = contentResolver
        .query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            arrayOf(MediaStore.Files.FileColumns.DATA),
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" +
                MIME_BY_EXTENSION.keys.joinToString(
                    prefix = " AND (",
                    separator = " OR ",
                    postfix = ")",
                ) {
                    "${MediaStore.Files.FileColumns.DATA} LIKE ?"
                },
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString()) +
                MIME_BY_EXTENSION.keys.map { "%.$it" },
            null,
        )
        ?.use { cursor ->
            buildList {
                val path = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    add(File(cursor.getString(path)))
                }
            }
        }
        .orEmpty()

    private fun File.mime(): String? = MIME_BY_EXTENSION[extension.lowercase()]

    private fun LibraryTrack.matches(file: File): Boolean =
        size == file.length() && dateModified == file.lastModified().milliseconds.inWholeSeconds

    private fun File.toTrack(mime: String, metadata: TrackMetadata): LibraryTrack {
        val size = length()
        val modified = lastModified().milliseconds.inWholeSeconds

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
            genre = metadata.genre,
        )
    }

    private fun bitrate(size: Long, durationMs: Long): Int = if (durationMs > 0) {
        (size * C.BITS_PER_BYTE * C.MILLIS_PER_SECOND / durationMs).toInt()
    } else {
        0
    }
}
