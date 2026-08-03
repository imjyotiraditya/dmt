package dev.jyotiraditya.dmt.library

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
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

/** The frame that holds the lyrics of a track, which an MP4 file names its lyrics by too. */
private const val LYRICS_FRAME_ID = "USLT"

/** The frame that names what it holds itself, which is how a file carries a tag of its own. */
private const val DESCRIBED_FRAME_ID = "TXXX"

/** The name the lyrics of a track are read by. */
private const val LYRICS_TAG = "LYRICS"

/**
 * Reads the metadata of a track by parsing its container, for the formats that the platform does
 * not index and therefore knows nothing about.
 */
@UnstableApi
object MetadataReader {

    /** Returns the metadata of the track at [uri], or empty metadata if it cannot be read. */
    suspend fun read(context: Context, uri: Uri): TrackMetadata = runCatching {
        retriever(context, uri)
            .use { retriever ->
                val durationUs = retriever.retrieveDurationUs().await()
                val tags = retriever.retrieveTrackGroups().await().tags()

                TrackMetadata(
                    durationMs = durationUs.takeIf { it != C.TIME_UNSET }?.let(Util::usToMs) ?: 0L,
                    title = tags?.title?.toString(),
                    artist = tags?.artist?.toString(),
                    album = tags?.albumTitle?.toString(),
                    trackNumber = tags?.trackNumber ?: 0,
                    discNumber = tags?.discNumber ?: 0,
                )
            }
    }.getOrDefault(TrackMetadata())

    /**
     * Returns the format of the audio of the track at [uri], or null if it cannot be read.
     *
     * The format describes the audio as the container declares it, which is what a track that is
     * not playing can be described by.
     */
    suspend fun readFormat(context: Context, uri: Uri): Format? = runCatching {
        retriever(context, uri).use { retriever ->
            retriever.retrieveTrackGroups().await().formats().firstOrNull { format ->
                MimeTypes.isAudio(format.sampleMimeType)
            }
        }
    }.getOrNull()

    /**
     * Returns the tags of the track at [uri], keyed by their name in upper case, or no tags if they
     * cannot be read.
     *
     * Every container names its tags differently, so the names are the ones a Vorbis comment would
     * use, which the lyrics and the gain of a track are looked up by.
     */
    suspend fun readTags(context: Context, uri: Uri): Map<String, List<String>> = runCatching {
        retriever(context, uri).use { retriever ->
            retriever.retrieveTrackGroups().await().formats()
                .mapNotNull { it.metadata }
                .flatMap { metadata -> (0 until metadata.length()).map(metadata::get) }
                .mapNotNull { entry -> entry.tag() }
                .groupBy({ it.first }, { it.second })
        }
    }.getOrDefault(emptyMap())

    /** Returns the name and the value of [this], or null if it does not hold a tag. */
    private fun Metadata.Entry.tag(): Pair<String, String>? = when (this) {
        is VorbisComment -> key to value
        is InternalFrame -> description.uppercase() to text
        is TextInformationFrame -> when (id) {
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

    /** Artwork is read separately, so parsing it here would only cost memory. */
    private fun artworkFreeExtractors(): DefaultExtractorsFactory =
        DefaultExtractorsFactory().setDisableArtworkMetadata(true)

    private fun TrackGroupArray.tags(): MediaMetadata? =
        formats()
            .mapNotNull { it.metadata }
            .takeIf { it.isNotEmpty() }
            ?.let { MediaMetadata.Builder().populateFromMetadata(it).build() }

    private fun TrackGroupArray.formats(): List<Format> =
        (0 until length).flatMap { group ->
            get(group).let { tracks -> (0 until tracks.length).map(tracks::getFormat) }
        }
}
