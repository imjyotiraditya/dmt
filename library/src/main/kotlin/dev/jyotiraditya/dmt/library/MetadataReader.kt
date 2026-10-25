package dev.jyotiraditya.dmt.library

import android.content.Context
import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.inspector.MetadataRetriever
import kotlinx.coroutines.guava.await

/** The ID3 lyrics frame; MP4 lyrics are reported under the same id. */
private const val LYRICS_FRAME_ID = "USLT"

/** The ID3 frame for user-defined text tags. */
private const val DESCRIBED_FRAME_ID = "TXXX"

private const val LYRICS_TAG = "LYRICS"

/** Reads track metadata by parsing the container, for formats the media store does not index. */
@UnstableApi
object MetadataReader {

    /** The metadata of the track at [uri], or empty metadata if it can't be read. */
    suspend fun read(context: Context, uri: Uri): TrackMetadata = suspendRunCatching {
        retriever(context, uri).use { retriever ->
            val durationUs = retriever.retrieveDurationUs().await()
            val tags = retriever.retrieveTrackGroups().await().tags()

            TrackMetadata(
                durationMs = durationUs.takeIfSet()?.let(Util::usToMs) ?: 0L,
                title = tags?.title?.toString(),
                artist = tags?.artist?.toString(),
                album = tags?.albumTitle?.toString(),
                trackNumber = tags?.trackNumber ?: 0,
                discNumber = tags?.discNumber ?: 0,
                genre = tags?.genre?.toString(),
            )
        }
    }
        .getOrDefault(TrackMetadata())

    suspend fun readFormat(context: Context, uri: Uri): Format? = suspendRunCatching {
        retriever(context, uri).use { retriever ->
            retriever.retrieveTrackGroups().await().formats().firstOrNull { format ->
                MimeTypes.isAudio(format.sampleMimeType)
            }
        }
    }
        .getOrNull()

    /** The tags of the track at [uri], keyed by upper-case Vorbis comment name. */
    suspend fun readTags(context: Context, uri: Uri): Map<String, List<String>> =
        suspendRunCatching {
            retriever(context, uri).use { retriever ->
                retriever
                    .retrieveTrackGroups()
                    .await()
                    .formats()
                    .mapNotNull { it.metadata }
                    .flatMap { metadata -> (0 until metadata.length()).map(metadata::get) }
                    .mapNotNull { entry -> entry.tag() }
                    .groupBy({ it.first }, { it.second })
            }
        }
            .getOrDefault(emptyMap())

    private fun Metadata.Entry.tag(): Pair<String, String>? = when (this) {
        is VorbisComment -> key to value

        is InternalFrame -> description.uppercase() to text

        is TextInformationFrame ->
            when (id) {
                LYRICS_FRAME_ID -> LYRICS_TAG to values.first()
                DESCRIBED_FRAME_ID -> description?.uppercase()?.let { it to values.first() }
                else -> null
            }

        else -> null
    }

    private fun retriever(context: Context, uri: Uri): MetadataRetriever =
        MetadataRetriever.Builder(context, MediaItem.fromUri(uri))
            .setMediaSourceFactory(DefaultMediaSourceFactory(context, artworkFreeExtractors()))
            .build()

    // Artwork is loaded separately, so skipping it here saves memory.
    private fun artworkFreeExtractors(): DefaultExtractorsFactory =
        DefaultExtractorsFactory().setDisableArtworkMetadata(true)

    private fun TrackGroupArray.tags(): MediaMetadata? = formats()
        .mapNotNull { it.metadata }
        .takeIf { it.isNotEmpty() }
        ?.let { MediaMetadata.Builder().populateFromMetadata(it).build() }

    private fun TrackGroupArray.formats(): List<Format> = (0 until length).flatMap { group ->
        get(group).let { tracks -> (0 until tracks.length).map(tracks::getFormat) }
    }
}
