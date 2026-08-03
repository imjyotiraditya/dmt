package dev.jyotiraditya.dmt.data.remote.lrclib

import dev.jyotiraditya.dmt.BuildConfig
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ALBUM
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_TITLE
import dev.jyotiraditya.dmt.domain.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://lrclib.net/api/get"
private const val USER_AGENT =
    "dmt v${BuildConfig.VERSION_NAME} (https://github.com/imjyotiraditya/dmt)"

private fun known(value: String, placeholder: String): String? =
    value.takeIf { it.isNotBlank() && it != placeholder }

@Singleton
class LrclibApi @Inject constructor(
    private val client: HttpClient,
) {

    suspend fun fetchLyrics(track: Track): String? {
        val title = known(track.title, UNKNOWN_TITLE) ?: return null
        val artist = known(track.artist, UNKNOWN_ARTIST) ?: return null
        val album = known(track.album, UNKNOWN_ALBUM)

        val response = client.get(BASE_URL) {
            url {
                parameters.append("track_name", title)
                parameters.append("artist_name", artist)
                album?.let { parameters.append("album_name", it) }
                parameters.append("duration", "${track.durationMs / 1000}")
            }
            header("User-Agent", USER_AGENT)
        }
        if (!response.status.isSuccess()) return null
        val json = JSONObject(response.bodyAsText())

        if (json.optBoolean("instrumental")) return null

        return json.optString("syncedLyrics")
            .ifBlank { json.optString("plainLyrics") }
            .ifBlank { null }
    }
}
