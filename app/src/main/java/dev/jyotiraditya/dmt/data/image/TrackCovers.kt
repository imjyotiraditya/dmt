package dev.jyotiraditya.dmt.data.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.map.Mapper
import coil3.request.Options
import coil3.size.pxOrElse
import dev.jyotiraditya.dmt.data.repository.CoverArtRepository
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.util.fitWithin
import javax.inject.Provider

/** The size a cover is decoded at when the request names none. */
private const val DEFAULT_COVER_PX = 512

/** A key for this track's cover that changes whenever the file or its cover does. */
val Track.coverKey: String
    get() = "track:$id:$dateModified:$coverUri"

/** Sends a track whose cover is on a server to its address, for the network fetcher. */
class TrackCoverMapper : Mapper<Track, String> {
    override fun map(data: Track, options: Options): String? =
        data.coverUri?.takeIf { it.scheme == "http" || it.scheme == "https" }?.toString()
}

class TrackCoverKeyer : Keyer<Track> {
    override fun key(data: Track, options: Options): String = data.coverKey
}

/** Loads a track's cover at the requested size and keeps that copy in the disk cache. */
class TrackCoverFetcher(
    private val track: Track,
    private val options: Options,
    private val context: Context,
    private val coverArt: CoverArtRepository,
    private val diskCache: DiskCache?,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val px = options.size.width.pxOrElse { DEFAULT_COVER_PX }
        val key = "${track.coverKey}@$px"
        diskCache?.let { cache ->
            cache.openSnapshot(key)?.let {
                return it.result(cache, key)
            }
        }
        val bitmap = decode(px) ?: return null
        store(key, bitmap)
        return ImageFetchResult(bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    /** Writes [bitmap] to the disk cache under [key], for the next launch. */
    private fun store(key: String, bitmap: Bitmap) {
        val cache = diskCache ?: return
        val editor = cache.openEditor(key) ?: return
        runCatching {
            cache.fileSystem.write(editor.data) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream())
            }
            editor.commit()
        }
            .onFailure { editor.abort() }
    }

    private fun DiskCache.Snapshot.result(cache: DiskCache, key: String): FetchResult =
        SourceFetchResult(
            source = ImageSource(data, cache.fileSystem, key, this),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK,
        )

    /** The track's cover at [px], from the media store when it indexes the track. */
    private suspend fun decode(px: Int): Bitmap? {
        val indexed = track.uri.scheme == ContentResolver.SCHEME_CONTENT
        if (indexed) {
            runCatching { context.contentResolver.loadThumbnail(track.uri, Size(px, px), null) }
                .getOrNull()
                ?.let {
                    return it
                }
        }
        return coverArt.loadArt(track, embedded = !indexed)?.let {
            runCatching { it.fitWithin(px) }.getOrNull()
        }
    }

    class Factory(
        private val context: Context,
        private val coverArt: Provider<CoverArtRepository>,
    ) : Fetcher.Factory<Track> {
        override fun create(data: Track, options: Options, imageLoader: ImageLoader): Fetcher =
            TrackCoverFetcher(data, options, context, coverArt.get(), imageLoader.diskCache)
    }
}
