package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.data.remote.artwork.AnimatedArtworkApi
import dev.jyotiraditya.dmt.data.remote.knownOrNull
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ALBUM
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_TITLE
import dev.jyotiraditya.dmt.domain.model.primaryArtist
import dev.jyotiraditya.dmt.library.MetadataReader
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_ALBUMS = 64

/** Animated album covers, remembered per album once the service has answered. */
@Singleton
class AnimatedArtworkRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val api: AnimatedArtworkApi,
) {

    /** Answers by album; a null url is an album without one. */
    private val known = LruCache<String, Answer>(CACHE_ALBUMS)

    private data class Answer(val url: String?)

    /** The animated cover of [track]'s album, or null if it has none or none could be fetched. */
    suspend fun streamUrlFor(track: Track): String? {
        val artist = track.primaryArtist.knownOrNull(UNKNOWN_ARTIST) ?: return null
        val album = releaseOf(track) ?: return null
        val key = "$artist\u0000$album".lowercase()
        known.get(key)?.let {
            return it.url
        }
        val url =
            api.fetchStreamUrl(artist, album).getOrElse {
                return null
            }
        known.put(key, Answer(url))
        return url
    }

    /**
     * The release [track] came out on: its tagged album, or its title when untagged, as a single.
     * Local files are read for their tag, as the media store names an untagged album after its
     * folder.
     */
    @OptIn(UnstableApi::class)
    private suspend fun releaseOf(track: Track): String? {
        val album =
            if (track.source == TrackSource.LOCAL) {
                MetadataReader.read(context, track.uri).album?.ifBlank { null }
            } else {
                track.album.knownOrNull(UNKNOWN_ALBUM)
            }
        return album ?: track.title.knownOrNull(UNKNOWN_TITLE)
    }
}
