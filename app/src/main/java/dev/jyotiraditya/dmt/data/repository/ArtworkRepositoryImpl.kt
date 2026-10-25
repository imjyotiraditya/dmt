package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.toArgb
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.data.image.coverKey
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository
import dev.jyotiraditya.dmt.util.fieldColor
import javax.inject.Inject
import javax.inject.Singleton

/** Decode sizes, smallest first; a request is served by the smallest that covers it. */
private val SIZES_PX = intArrayOf(256, 512, 1024)

private const val COLOR_ENTRIES = 512

/** Cache marker for a cover with no usable colour. */
private const val NO_COLOR = 0

/** Track covers through Coil, which keeps them in memory and on disk. */
@Singleton
class ArtworkRepositoryImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) : ArtworkRepository {

    private val colors = LruCache<Long, Int>(COLOR_ENTRIES)
    private val missing = LruCache<Long, Unit>(COLOR_ENTRIES)

    override fun cached(track: Track, sizePx: Int): Bitmap? {
        val memory = imageLoader.memoryCache ?: return null
        val wanted = SIZES_PX.indexOf(bucket(sizePx))
        // The asked size or larger first, then smaller ones.
        for (i in wanted until SIZES_PX.size) {
            memory.bitmap(track, SIZES_PX[i])?.let {
                return it
            }
        }
        for (i in wanted - 1 downTo 0) {
            memory.bitmap(track, SIZES_PX[i])?.let {
                return it
            }
        }
        return null
    }

    override suspend fun load(track: Track, sizePx: Int): Bitmap? {
        if (missing.get(track.id) != null) return null
        val size = bucket(sizePx)
        val request =
            ImageRequest.Builder(context)
                .data(track)
                .size(size)
                .memoryCacheKey(key(track, size))
                .allowHardware(false)
                .build()
        val art = (imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
        // A server cover may only have failed to download, so it is asked for again later.
        if (art == null && !track.hasRemoteCover) missing.put(track.id, Unit)
        return art
    }

    override fun cachedColor(track: Track): Int? = colors.get(track.id)?.takeIf { it != NO_COLOR }

    override suspend fun color(track: Track): Int? {
        colors.get(track.id)?.let {
            return it.takeIf { color -> color != NO_COLOR }
        }
        val art = cached(track, SIZES_PX.first()) ?: load(track, SIZES_PX.first())
        if (art == null) {
            if (!track.hasRemoteCover) colors.put(track.id, NO_COLOR)
            return null
        }
        return art.fieldColor().toArgb().also { colors.put(track.id, it) }
    }

    private val Track.hasRemoteCover: Boolean
        get() = coverUri?.scheme == "http" || coverUri?.scheme == "https"

    private fun bucket(sizePx: Int): Int = SIZES_PX.firstOrNull { it >= sizePx } ?: SIZES_PX.last()

    private fun key(track: Track, size: Int): String = "${track.coverKey}@$size"

    private fun MemoryCache.bitmap(track: Track, size: Int): Bitmap? =
        get(MemoryCache.Key(key(track, size)))?.image?.toBitmap()
}
