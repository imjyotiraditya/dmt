package dev.jyotiraditya.dmt.library

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.inspector.MetadataRetriever
import kotlinx.coroutines.guava.await

/**
 * Reads the metadata of a track by parsing its container, for the formats that the platform does
 * not index and therefore knows nothing about.
 */
@UnstableApi
object MetadataReader {

    /** Returns the metadata of the track at [uri], or empty metadata if it cannot be read. */
    suspend fun read(context: Context, uri: Uri): TrackMetadata = runCatching {
        MetadataRetriever.Builder(context, MediaItem.fromUri(uri))
            .setMediaSourceFactory(DefaultMediaSourceFactory(context, artworkFreeExtractors()))
            .build()
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
