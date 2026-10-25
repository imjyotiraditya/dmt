package dev.jyotiraditya.dmt.domain.repository

import android.graphics.Bitmap
import dev.jyotiraditya.dmt.domain.model.Track

/** Track covers decoded at display size. */
interface ArtworkRepository {

    /** The best cover of [track] already in memory, at any size, or null. */
    fun cached(track: Track, sizePx: Int): Bitmap?

    /** The cover of [track] at least [sizePx] on its longer side where the source allows. */
    suspend fun load(track: Track, sizePx: Int): Bitmap?

    /** The field colour already worked out for [track]'s cover, as ARGB, or null. */
    fun cachedColor(track: Track): Int?

    /** The field colour of [track]'s cover as ARGB, or null when it has no clear colour. */
    suspend fun color(track: Track): Int?
}
