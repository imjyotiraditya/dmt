package dev.jyotiraditya.dmt.data.repository

import android.util.LruCache
import androidx.core.net.toUri
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinAccount
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinApi
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinItem
import dev.jyotiraditya.dmt.data.remote.jellyfin.jellyfinAccount
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private fun String.toJellyfinLong(): Long {
    val hexDigits = replace("-", "").take(16)

    return hexDigits.toULong(16).toLong()
}

@Singleton
class JellyfinMediaRepositoryImpl
@Inject
constructor(
    private val api: JellyfinApi,
    private val settingsRepository: PreferencesRepository,
) : MediaRepository {

    private val fetchLock = Mutex()

    /** Tracks of recently used accounts, so switching back needs no fetch. */
    private val cached = LruCache<JellyfinAccount, List<Track>>(4)

    /** The tracks of the Jellyfin source in use, fetched once per account until [invalidate]. */
    override suspend fun scan(): List<Track> {
        val account =
            settingsRepository.settings.first().server?.jellyfinAccount() ?: return emptyList()
        cached[account]?.let {
            return it
        }
        return fetchLock.withLock {
            cached[account]
                ?: api.fetchAudioItems(account.url, account.userId, account.token)
                    .map { it.toTrack(account.url, account.token) }
                    .also { cached.put(account, it) }
        }
    }

    override fun invalidate() {
        cached.evictAll()
    }

    private fun JellyfinItem.toTrack(baseUrl: String, token: String): Track {
        val artId = if (hasOwnArt) id else albumId

        return Track(
            id = id.toJellyfinLong(),
            uri = api.streamUrl(baseUrl, id, token).toUri(),
            title = title,
            artist = artist,
            albumArtist = albumArtist,
            album = album,
            path = "",
            durationMs = durationMs,
            mime = mime,
            bitrate = bitrate,
            size = size,
            trackNumber = trackNumber,
            dateAdded = dateAdded,
            dateModified = dateAdded,
            coverUri = artId?.let { api.imageUrl(baseUrl, it, token).toUri() },
            source = TrackSource.JELLYFIN,
            remoteId = id,
            bitDepth = bitDepth,
            sampleRateHz = sampleRateHz,
        )
    }
}
