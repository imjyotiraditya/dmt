package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinApi
import dev.jyotiraditya.dmt.data.remote.lrclib.LrclibApi
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.LyricsRepository
import dev.jyotiraditya.dmt.domain.repository.SettingsRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    private val jellyfinApi: JellyfinApi,
    private val lrclibApi: LrclibApi,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider,
) {
    private val onlineCache = ConcurrentHashMap<Long, Lyrics>()

    suspend operator fun invoke(track: Track): Lyrics? =
        withContext(dispatchers.io) {
            val local = if (track.source == TrackSource.JELLYFIN) {
                jellyfinLyrics(track)
            } else {
                lyricsRepository.lyricsFor(track.path, track.mime)
            }
            local ?: onlineCache[track.id]
        }

    suspend fun online(track: Track): Lyrics? =
        withContext(dispatchers.io) {
            onlineCache[track.id]
                ?: runCatching { lrclibApi.fetchLyrics(track) }.getOrNull()
                    ?.also { onlineCache[track.id] = it }
        }

    private suspend fun jellyfinLyrics(track: Track): Lyrics? {
        val remoteId = track.remoteId ?: return null

        val settings = settingsRepository.settings.first()
        val url = settings.jellyfinUrl ?: return null
        val token = settings.jellyfinToken ?: return null

        return runCatching {
            jellyfinApi.fetchLyrics(url, remoteId, token)
        }.getOrNull()
    }
}
