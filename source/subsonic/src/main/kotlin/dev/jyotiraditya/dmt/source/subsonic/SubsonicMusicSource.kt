package dev.jyotiraditya.dmt.source.subsonic

import android.util.LruCache
import androidx.core.net.toUri
import dev.jyotiraditya.dmt.core.model.ServerSource
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.core.source.MusicSource
import dev.jyotiraditya.dmt.core.source.SourceDisplay
import dev.jyotiraditya.dmt.core.source.SourceFailure
import dev.jyotiraditya.dmt.core.source.toTrackId
import dev.jyotiraditya.lyrics.Lyrics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Navidrome and other Subsonic servers. */
@Singleton
class SubsonicMusicSource @Inject constructor(private val api: SubsonicApi) : MusicSource {

    private val fetchLock = Mutex()

    /** Tracks of recently used accounts, so switching back needs no fetch. */
    private val cached = LruCache<SubsonicAccount, List<Track>>(4)

    override val display =
        SourceDisplay(
            title = R.string.source_subsonic,
            line = R.string.source_subsonic_line,
            setupDetail = R.string.source_subsonic_setup,
            icon = R.drawable.ic_navidrome,
            addressHint = R.string.source_subsonic_address_hint,
            noTracks = R.string.source_subsonic_no_tracks,
            scanFailed = R.string.source_subsonic_scan_failed,
            wrongKind = R.string.source_subsonic_wrong_kind,
        )

    override fun normalize(address: String): String = normalizeSubsonicUrl(address)

    override suspend fun signIn(
        address: String,
        username: String,
        password: String,
    ): Map<String, String> =
        SubsonicAccount.create(address, username, password).also { api.ping(it) }.secrets

    /** The tracks of [server], fetched once per account until [invalidate]. */
    override suspend fun scan(server: ServerSource?): List<Track> {
        val account = server?.subsonicAccount() ?: return emptyList()
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

    override suspend fun lyrics(server: ServerSource, track: Track): Lyrics? {
        val songId = track.remoteId ?: return null
        val account = server.subsonicAccount() ?: return null
        return api.lyrics(account, SubsonicSongRef(songId, track.artist, track.title))
    }

    override fun failure(failure: Throwable): SourceFailure = failure.toSourceFailure()

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
        source = SourceMode.SUBSONIC,
        remoteId = id,
        genre = genre,
        bitDepth = bitDepth,
        sampleRateHz = sampleRateHz,
    )
}
