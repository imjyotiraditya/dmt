package dev.jyotiraditya.dmt.data.remote.lrclib

import dev.jyotiraditya.dmt.BuildConfig
import dev.jyotiraditya.dmt.domain.model.Track
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val client: OkHttpClient,
) {

    fun fetchLyrics(track: Track): String? {
        val title = known(track.title, "unknown title") ?: return null
        val artist = known(track.artist, "unknown artist") ?: return null
        val album = known(track.album, "unknown album")

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .apply { album?.let { addQueryParameter("album_name", it) } }
            .addQueryParameter("duration", "${track.durationMs / 1000}")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        val json = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            JSONObject(response.body.string())
        }

        if (json.optBoolean("instrumental")) return null

        return json.optString("syncedLyrics")
            .ifBlank { json.optString("plainLyrics") }
            .ifBlank { null }
    }
}
