package dev.jyotiraditya.dmt.data.repository

import android.util.LruCache
import dev.jyotiraditya.dmt.data.remote.artwork.AnimatedArtworkApi
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.primaryArtist
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_ALBUMS = 64

/** Animated album covers, remembered per album once the service has answered. */
@Singleton
class AnimatedArtworkRepository @Inject constructor(private val api: AnimatedArtworkApi) {

    /** Answers by album; a null url is an album without one. */
    private val known = LruCache<String, Answer>(CACHE_ALBUMS)

    private data class Answer(val url: String?)

    /** The animated cover of [track]'s album, or null if it has none or none could be fetched. */
    suspend fun streamUrlFor(track: Track): String? {
        val key = "${track.primaryArtist}\u0000${track.album}".lowercase()
        known.get(key)?.let {
            return it.url
        }
        val url =
            api.fetchStreamUrl(track).getOrElse {
                return null
            }
        known.put(key, Answer(url))
        return url
    }
}
