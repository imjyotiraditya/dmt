package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinApi
import dev.jyotiraditya.dmt.data.remote.lrclib.LrclibApi
import dev.jyotiraditya.lyrics.LyricsParser
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.data.repository.LyricsRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    private val jellyfinApi: JellyfinApi,
    private val lrclibApi: LrclibApi,
    private val settingsRepository: PreferencesRepository,
) {

    /**
     * Returns the lyrics that [track] carries or that sit beside it, from the source that the
     * settings ask for, or null if that source has none.
     *
     * The source that has to be fetched is left to [onlineText], as it is the caller that decides
     * whether asking the network is wanted.
     */
    suspend operator fun invoke(track: Track): Lyrics? =
        withContext(Dispatchers.IO) {
            val settings = settingsRepository.settings.first()

            if (track.source == TrackSource.JELLYFIN) {
                jellyfinLyrics(track, settings)
            } else {
                lyricsRepository.lyricsFor(track.path, settings.lyricsSource)
            }
        }

    /** Returns the sources that have lyrics for [track] without asking the network. */
    suspend fun availableSources(track: Track): Set<LyricsSource> =
        withContext(Dispatchers.IO) {
            if (track.source == TrackSource.JELLYFIN) {
                emptySet()
            } else {
                LyricsSource.entries
                    .filter { lyricsRepository.lyricsFor(track.path, it) != null }
                    .toSet()
            }
        }

    /** Returns the lyrics of [track] as lrclib holds them, or null if it holds none. */
    suspend fun onlineText(track: Track): String? =
        withContext(Dispatchers.IO) {
            runCatching { lrclibApi.fetchLyrics(track) }.getOrNull()
        }

    /** Returns the lyrics that [text] describes, or null if it describes none. */
    suspend fun parse(text: String): Lyrics? =
        withContext(Dispatchers.Default) {
            LyricsParser.parse(text)
        }

    private suspend fun jellyfinLyrics(track: Track, settings: DmtSettings): Lyrics? {
        val remoteId = track.remoteId ?: return null

        val url = settings.jellyfinUrl ?: return null
        val token = settings.jellyfinToken ?: return null

        return runCatching {
            jellyfinApi.fetchLyrics(url, remoteId, token)
        }.getOrNull()
    }
}
