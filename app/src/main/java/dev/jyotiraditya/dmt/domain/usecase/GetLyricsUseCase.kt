package dev.jyotiraditya.dmt.domain.usecase

import android.util.LruCache
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinApi
import dev.jyotiraditya.dmt.data.remote.jellyfin.jellyfinAccount
import dev.jyotiraditya.dmt.data.remote.lrclib.LrclibApi
import dev.jyotiraditya.dmt.data.remote.lrcred.LrcRedApi
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicApi
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicSongRef
import dev.jyotiraditya.dmt.data.remote.subsonic.subsonicAccount
import dev.jyotiraditya.dmt.data.repository.LyricsRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.LyricsTiming
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.model.timing
import dev.jyotiraditya.dmt.util.suspendRunCatching
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.LyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_SIZE = 64

/** Finds lyrics on the phone, on the music server, and from online lyrics services. */
@Singleton
class GetLyricsUseCase
@Inject
constructor(
    private val lyricsRepository: LyricsRepository,
    private val jellyfinApi: JellyfinApi,
    private val subsonicApi: SubsonicApi,
    private val lrclibApi: LrclibApi,
    private val lrcRedApi: LrcRedApi,
    private val settingsRepository: PreferencesRepository,
) {

    /** One answer from a source; null lyrics when it had none. */
    private data class Answer(val lyrics: Lyrics?)

    /** Lyrics fetched this session from online services, by source and track. */
    private val online = LruCache<Pair<LyricsSource, Long>, Lyrics>(CACHE_SIZE)

    /** Lyrics fetched this session from the music server, by track. */
    private val server = LruCache<Long, Answer>(CACHE_SIZE)

    /** The lyrics of [track] from [source], or from its server for a remote track. */
    suspend operator fun invoke(track: Track, source: LyricsSource): Lyrics? =
        withContext(Dispatchers.IO) {
            if (track.source == TrackSource.LOCAL) {
                return@withContext lyricsRepository.lyricsFor(track.path, source)
            }
            server.get(track.id)?.let {
                return@withContext it.lyrics
            }
            val settings = settingsRepository.settings.first()
            val fetched = suspendRunCatching {
                when (track.source) {
                    TrackSource.JELLYFIN -> jellyfinLyrics(track, settings)
                    TrackSource.SUBSONIC -> subsonicLyrics(track, settings)
                    TrackSource.LOCAL -> null
                }
            }
            fetched.getOrNull().also { if (fetched.isSuccess) server.put(track.id, Answer(it)) }
        }

    /** How each source with lyrics for [track] is timed, without asking the network. */
    suspend fun availableSources(track: Track): Map<LyricsSource, LyricsTiming> =
        withContext(Dispatchers.IO) {
            LyricsSource.entries
                .mapNotNull { source -> heldLyrics(source, track)?.let { source to it.timing } }
                .toMap()
        }

    /** The lyrics [source] holds for [track] on the phone or from this session's fetches. */
    private suspend fun heldLyrics(source: LyricsSource, track: Track): Lyrics? = when {
        source.online -> online.get(source to track.id)
        track.source == TrackSource.LOCAL -> lyricsRepository.lyricsFor(track.path, source)
        else -> null
    }

    /**
     * The lyrics of [track] from the online [source]: null if it has none, a failure if
     * unreachable.
     */
    suspend fun onlineLyrics(source: LyricsSource, track: Track): Result<Lyrics?> {
        val key = source to track.id
        online.get(key)?.let {
            return Result.success(it)
        }
        val fetched =
            withContext(Dispatchers.IO) {
                suspendRunCatching {
                    val text =
                        when (source) {
                            LyricsSource.LRCLIB -> lrclibApi.fetchLyrics(track)

                            LyricsSource.LRC_RED -> lrcRedApi.fetchLyrics(track)

                            LyricsSource.EMBEDDED,
                            LyricsSource.LOCAL,
                            -> null
                        }
                    text?.let(LyricsParser::parse)
                }
            }
        // Only hits are kept; a miss is asked again next time.
        fetched.getOrNull()?.let { online.put(key, it) }
        return fetched
    }

    private suspend fun jellyfinLyrics(track: Track, settings: DmtSettings): Lyrics? {
        val remoteId = track.remoteId ?: return null
        val account = settings.server?.jellyfinAccount() ?: return null
        return jellyfinApi.fetchLyrics(account.url, remoteId, account.token)
    }

    private suspend fun subsonicLyrics(track: Track, settings: DmtSettings): Lyrics? {
        val songId = track.remoteId ?: return null
        val account = settings.server?.subsonicAccount() ?: return null
        return subsonicApi.lyrics(account, SubsonicSongRef(songId, track.artist, track.title))
    }
}
