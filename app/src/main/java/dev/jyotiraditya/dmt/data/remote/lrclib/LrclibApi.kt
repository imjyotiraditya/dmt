package dev.jyotiraditya.dmt.data.remote.lrclib

import dev.jyotiraditya.dmt.data.remote.DMT_USER_AGENT
import dev.jyotiraditya.dmt.data.remote.found
import dev.jyotiraditya.dmt.data.remote.knownOrNull
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ALBUM
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_TITLE
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://lrclib.net/api/get"

@Serializable
private data class LrclibLyrics(
    val instrumental: Boolean = false,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
)

/** Client for the LRCLIB lyrics database. */
@Singleton
class LrclibApi @Inject constructor(private val client: HttpClient, private val json: Json) {

    /** The synced or else plain lyrics of [track], or null when LRCLIB has none. */
    suspend fun fetchLyrics(track: Track): String? {
        val title = track.title.knownOrNull(UNKNOWN_TITLE) ?: return null
        val artist = track.artist.knownOrNull(UNKNOWN_ARTIST) ?: return null
        val album = track.album.knownOrNull(UNKNOWN_ALBUM)

        val response =
            client.get(BASE_URL) {
                parameter("track_name", title)
                parameter("artist_name", artist)
                album?.let { parameter("album_name", it) }
                parameter("duration", track.durationMs / 1000)
                header(HttpHeaders.UserAgent, DMT_USER_AGENT)
            }
        if (!response.found()) return null
        val lyrics = json.decodeFromString<LrclibLyrics>(response.bodyAsText())
        if (lyrics.instrumental) return null
        return lyrics.syncedLyrics?.ifBlank { null } ?: lyrics.plainLyrics?.ifBlank { null }
    }
}
