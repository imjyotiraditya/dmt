package dev.jyotiraditya.dmt.data.repository

import android.util.LruCache
import androidx.core.net.toUri
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicAccount
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicApi
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicSong
import dev.jyotiraditya.dmt.data.remote.subsonic.subsonicAccount
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val FNV_OFFSET = -0x340d631b7bdddcdbL
private const val FNV_PRIME = 0x100000001b3L

/** A stable 64-bit FNV-1a hash of this song id, used as the track id. */
internal fun String.toTrackId(): Long = encodeToByteArray().fold(FNV_OFFSET) { hash, byte ->
    (hash xor (byte.toLong() and 0xff)) * FNV_PRIME
}

@Singleton
class SubsonicMediaRepositoryImpl
@Inject
constructor(
    private val api: SubsonicApi,
    private val settingsRepository: PreferencesRepository,
) : MediaRepository {

    private val fetchLock = Mutex()

    /** Tracks of recently used accounts, so switching back needs no fetch. */
    private val cached = LruCache<SubsonicAccount, List<Track>>(4)

    /** The tracks of the Subsonic source in use, fetched once per account until [invalidate]. */
    override suspend fun scan(): List<Track> {
        val account =
            settingsRepository.settings.first().server?.subsonicAccount() ?: return emptyList()
        cached[account]?.let {
            return it
        }
        return fetchLock.withLock {
            cached[account]
                ?: api.songs(account).map { it.toTrack(account) }.also { cached.put(account, it) }
        }
    }

    override fun invalidate() {
        cached.evictAll()
    }

    private fun SubsonicSong.toTrack(account: SubsonicAccount): Track = Track(
        id = id.toTrackId(),
        uri = api.streamUrl(account, id).toUri(),
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
        discNumber = discNumber,
        dateAdded = dateAdded,
        dateModified = dateAdded,
        coverUri = coverArt?.let { api.coverUrl(account, it).toUri() },
        source = TrackSource.SUBSONIC,
        remoteId = id,
        genre = genre,
        bitDepth = bitDepth,
        sampleRateHz = sampleRateHz,
    )
}
