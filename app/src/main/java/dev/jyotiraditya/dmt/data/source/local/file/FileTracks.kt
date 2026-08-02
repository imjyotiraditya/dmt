package dev.jyotiraditya.dmt.data.source.local.file

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.inspector.MetadataRetriever
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.util.allFilesAccess
import kotlinx.coroutines.guava.await
import java.io.File
import kotlin.math.absoluteValue

private const val ID_BASE = -1_000_000_000_000L
private const val NO_MEDIA = ".nomedia"
private const val BITS_PER_BYTE = 8
private const val MILLIS_PER_SECOND = 1000L
private const val MICROS_PER_MILLI = 1000L

private val SKIPPED_DIRS = setOf("Android/data", "Android/obb")

private val MIME_TYPES = mapOf(
    "ape" to "audio/x-ape",
    "tak" to "audio/x-tak",
    "tta" to "audio/x-tta",
    "wv" to "audio/x-wavpack",
)

@UnstableApi
object FileTracks {

    suspend fun scan(context: Context, root: File, blocked: Set<String>): List<Track> {
        if (!allFilesAccess) return emptyList()

        val skipped = SKIPPED_DIRS.map { File(root, it) }

        return root.walkTopDown()
            .onEnter { dir -> dir.isScannable(skipped) }
            .filter { it.isFile && it.extension.lowercase() in MIME_TYPES }
            .filterNot { it.parentFile?.absolutePath in blocked }
            .toList()
            .map { it.toTrack(durationMs(context, it.toUri())) }
    }

    private fun File.isScannable(skipped: List<File>): Boolean =
        this !in skipped && !name.startsWith('.') && !File(this, NO_MEDIA).exists()

    private fun File.toTrack(durationMs: Long): Track {
        val size = length()

        return Track(
            id = ID_BASE - absolutePath.hashCode().toLong().absoluteValue,
            uri = toUri(),
            title = nameWithoutExtension,
            artist = "unknown artist",
            album = parentFile?.name ?: "unknown album",
            path = absolutePath,
            durationMs = durationMs,
            mime = MIME_TYPES.getValue(extension.lowercase()),
            bitrate = bitrate(size, durationMs),
            size = size,
            trackNumber = 0,
            dateAdded = lastModified() / MILLIS_PER_SECOND,
            dateModified = lastModified() / MILLIS_PER_SECOND,
            source = TrackSource.LOCAL,
        )
    }

    private suspend fun durationMs(context: Context, uri: Uri): Long =
        runCatching {
            MetadataRetriever.Builder(context, MediaItem.fromUri(uri))
                .build()
                .use { it.retrieveDurationUs().await() }
        }.getOrNull()
            ?.takeIf { it != C.TIME_UNSET }
            ?.div(MICROS_PER_MILLI)
            ?: 0L

    private fun bitrate(size: Long, durationMs: Long): Int =
        if (durationMs > 0) (size * BITS_PER_BYTE * MILLIS_PER_SECOND / durationMs).toInt() else 0
}
