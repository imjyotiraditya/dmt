package dev.jyotiraditya.dmt.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import android.util.TypedValue
import dev.jyotiraditya.dmt.core.common.toAsciiBitmap
import dev.jyotiraditya.dmt.data.source.local.KEY_RAW
import dev.jyotiraditya.dmt.data.source.local.dmtStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URL

private const val TAG = "DMT_WIDGET_ART"

/**
 * Lightweight in-process artwork cache for the widget.
 *
 * Depending on the 'raw_art' user setting stored in Preferences DataStore:
 *  - 'raw_art' = true  -> original scaled bitmap is cached and returned
 *  - 'raw_art' = false -> bitmap converted to ASCII using toAsciiBitmap and cached
 */
object WidgetArtworkCache {

    private const val MAX_ENTRIES = 6
    private const val ARTWORK_DP = 46f

    private val lru = LruCache<String, Bitmap>(MAX_ENTRIES)

    /**
     * Return a [Bitmap] for the given track, using the cache when available.
     * Respects 'raw_art' preference to decide between raw image or ASCII rendering.
     *
     * Always call from an IO dispatcher — may perform disk or network I/O.
     */
    suspend fun get(context: Context, artworkUri: Uri?, trackUri: Uri?): Bitmap? {
        // Use artworkUri as the primary cache key; fall back to trackUri if absent.
        val key = artworkUri?.toString() ?: trackUri?.toString() ?: return null

        lru.get(key)?.let {
            Log.d(TAG, "cache hit for $key")
            return it
        }

        val targetPx = dpToPx(context, ARTWORK_DP)

        // Read raw_art user preference from app's local Preferences DataStore
        val isRawArt = runCatching {
            context.dmtStore.data.map { it[KEY_RAW] ?: false }.first()
        }.getOrDefault(false)

        Log.d(TAG, "get: loading artwork (isRawArt=$isRawArt) for $key")

        // Strategy 1 — explicit artwork URI (content://, file://, http://, https://)
        if (artworkUri != null) {
            val bmp = runCatching { loadFromUri(context, artworkUri, targetPx) }
                .getOrElse { e -> Log.w(TAG, "artworkUri load failed: ${e.message}"); null }
            if (bmp != null) {
                Log.d(TAG, "loaded via artworkUri: $artworkUri")
                val processed = if (isRawArt) {
                    scaleFinal(bmp, targetPx)
                } else {
                    runCatching { scaleFinal(bmp.toAsciiBitmap(context, cols = 32), targetPx) }
                        .getOrElse { e -> Log.e(TAG, "toAsciiBitmap failed", e); bmp }
                }
                lru.put(key, processed)
                return processed
            }
        }

        // Strategy 2 — MediaMetadataRetriever from the track file (embedded APIC tag)
        if (trackUri != null) {
            val bmp = runCatching { loadEmbedded(context, trackUri, targetPx) }
                .getOrElse { e -> Log.w(TAG, "embedded art failed for $trackUri: ${e.message}"); null }
            if (bmp != null) {
                Log.d(TAG, "loaded via MediaMetadataRetriever: $trackUri")
                val processed = if (isRawArt) {
                    scaleFinal(bmp, targetPx)
                } else {
                    runCatching { scaleFinal(bmp.toAsciiBitmap(context, cols = 32), targetPx) }
                        .getOrElse { e -> Log.e(TAG, "toAsciiBitmap failed", e); bmp }
                }
                lru.put(key, processed)
                return processed
            }
        }

        Log.d(TAG, "no artwork found (artworkUri=$artworkUri, trackUri=$trackUri)")
        return null
    }

    /** Remove cached entry when track changes. */
    fun evict(key: String?) {
        key?.let { lru.remove(it) }
    }

    /** Clear entire cache (called when all widget instances removed). */
    fun clear() {
        lru.evictAll()
    }

    // -----------------------------------------------------------------------
    // Strategy implementations
    // -----------------------------------------------------------------------

    private fun loadFromUri(context: Context, uri: Uri, targetPx: Int): Bitmap? {
        return when (uri.scheme) {
            "http", "https" -> loadRemote(uri, targetPx)
            else -> loadLocal(context, uri, targetPx)
        }
    }

    /** Decode via ContentResolver (content://, file://, android.resource://). */
    private fun loadLocal(context: Context, uri: Uri, targetPx: Int): Bitmap? {
        // First pass — measure
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: return null

        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        // Second pass — decode with inSampleSize
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, targetPx)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        return raw
    }

    /** Download and decode a remote image. */
    private fun loadRemote(uri: Uri, targetPx: Int): Bitmap? {
        return URL(uri.toString()).openStream().use { BitmapFactory.decodeStream(it) }
    }

    /**
     * Extract embedded cover art from the audio file using [MediaMetadataRetriever].
     * Handles MP3 APIC frames, FLAC PICTURE blocks, OGG COVERART, MP4 covr atoms, etc.
     */
    private fun loadEmbedded(context: Context, trackUri: Uri, targetPx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, trackUri)
            val artBytes = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
        } finally {
            runCatching { retriever.release() }
        }
    }

    // -----------------------------------------------------------------------
    // Bitmap utilities
    // -----------------------------------------------------------------------

    private fun scaleFinal(src: Bitmap, targetPx: Int): Bitmap {
        if (src.width == targetPx && src.height == targetPx) return src
        val scaled = Bitmap.createScaledBitmap(src, targetPx, targetPx, true)
        if (scaled !== src) src.recycle()
        return scaled
    }

    private fun calculateSampleSize(srcW: Int, srcH: Int, targetPx: Int): Int {
        var size = 1
        val larger = maxOf(srcW, srcH)
        while (larger / (size * 2) >= targetPx) size *= 2
        return size
    }

    private fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics,
        ).toInt().coerceAtLeast(1)
}
