package dev.jyotiraditya.dmt.source.jellyfin

import android.util.LruCache
import androidx.core.net.toUri
import dev.jyotiraditya.dmt.core.model.ServerSource
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.core.source.MusicSource
import dev.jyotiraditya.dmt.core.source.SourceDisplay
import dev.jyotiraditya.dmt.core.source.SourceFailure
import dev.jyotiraditya.lyrics.Lyrics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private fun String.toJellyfinLong(): Long {
    val hexDigits = replace("-", "").take(16)

    return hexDigits.toULong(16).toLong()
}

/** Jellyfin media servers. */
@Singleton
class JellyfinMusicSource @Inject constructor(private val api: JellyfinApi) : MusicSource {

    private val fetchLock = Mutex()

    /** Tracks of recently used accounts, so switching back needs no fetch. */
    private val cached = LruCache<JellyfinAccount, List<Track>>(4)

    override val display =
        SourceDisplay(
            title = R.string.source_jellyfin,
            line = R.string.source_jellyfin_line,
            setupDetail = R.string.source_jellyfin_setup,
            icon = R.drawable.ic_jellyfin,
            addressHint = R.string.source_jellyfin_address_hint,
            noTracks = R.string.source_jellyfin_no_tracks,
            scanFailed = R.string.source_jellyfin_scan_failed,
            wrongKind = R.string.source_jellyfin_wrong_kind,
        )

    override fun normalize(address: String): String = normalizeJellyfinUrl(address)

    override suspend fun signIn(
        address: String,
        username: String,
        password: String,
    ): Map<String, String> = api.authenticate(address, username, password).secrets

    /** The tracks of [server], fetched once per account until [invalidate]. */
    override suspend fun scan(server: ServerSource?): List<Track> {
        val account = server?.jellyfinAccount() ?: return emptyList()
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

    override suspend fun lyrics(server: ServerSource, track: Track): Lyrics? {
        val remoteId = track.remoteId ?: return null
        val account = server.jellyfinAccount() ?: return null
        return api.fetchLyrics(account.url, remoteId, account.token)
    }

    override fun failure(failure: Throwable): SourceFailure = failure.toSourceFailure()

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
            source = SourceMode.JELLYFIN,
            remoteId = id,
            bitDepth = bitDepth,
            sampleRateHz = sampleRateHz,
        )
    }
}
