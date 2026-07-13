package dev.jyotiraditya.dmt.data.repository

import androidx.core.net.toUri
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinApi
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinItem
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import dev.jyotiraditya.dmt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private fun String.toJellyfinLong(): Long {
    val hexDigits = replace("-", "").take(16)

    return hexDigits.toULong(16).toLong()
}

@Singleton
class JellyfinMediaRepositoryImpl @Inject constructor(
    private val api: JellyfinApi,
    private val settingsRepository: SettingsRepository,
) : MediaRepository {

    override suspend fun scan(): List<Track> {
        val settings = settingsRepository.settings.first()
        val url = settings.jellyfinUrl ?: return emptyList()
        val userId = settings.jellyfinUserId ?: return emptyList()
        val token = settings.jellyfinToken ?: return emptyList()

        return api.fetchAudioItems(url, userId, token).map { item ->
            item.toTrack(url, token)
        }
    }

    private fun JellyfinItem.toTrack(baseUrl: String, token: String): Track {
        val artId = if (hasOwnArt) id else albumId

        return Track(
            id = id.toJellyfinLong(),
            uri = api.streamUrl(baseUrl, id, token).toUri(),
            title = title,
            artist = artist,
            album = album,
            path = "",
            durationMs = durationMs,
            mime = mime,
            bitrate = bitrate,
            size = size,
            trackNumber = trackNumber,
            dateAdded = dateAdded,
            coverUri = artId?.let { api.imageUrl(baseUrl, it, token).toUri() },
            source = TrackSource.JELLYFIN,
            remoteId = id,
        )
    }
}
