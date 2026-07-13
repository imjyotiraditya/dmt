package dev.jyotiraditya.dmt.data.remote.jellyfin

import dev.jyotiraditya.dmt.data.source.local.lyrics.LyricsParser
import dev.jyotiraditya.dmt.data.source.local.lyrics.fillLineEnds
import dev.jyotiraditya.dmt.data.source.local.lyrics.withInterludes
import dev.jyotiraditya.dmt.domain.model.LyricLine
import dev.jyotiraditya.dmt.domain.model.Lyrics
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class JellyfinAuth(val userId: String, val token: String)

data class JellyfinItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String?,
    val trackNumber: Int,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val hasOwnArt: Boolean,
    val dateAdded: Long,
)

private const val CLIENT_NAME = "DMT"
private const val CLIENT_VERSION = "1"
private const val DEVICE_NAME = "Android"
private const val DEVICE_ID = "dmt-android"

private const val TICKS_PER_MS = 10_000L

@Singleton
class JellyfinApi @Inject constructor(
    private val client: OkHttpClient,
) {

    fun authenticate(baseUrl: String, username: String, password: String): JellyfinAuth {
        val credentials = JSONObject()
            .put("Username", username)
            .put("Pw", password)

        val json = postJson(
            url = endpoint(baseUrl, "/Users/AuthenticateByName"),
            body = credentials,
        )

        return JellyfinAuth(
            userId = json.getJSONObject("User").getString("Id"),
            token = json.getString("AccessToken"),
        )
    }

    fun fetchAudioItems(baseUrl: String, userId: String, token: String): List<JellyfinItem> {
        val json = getJson(
            url = endpoint(
                baseUrl,
                "/Users/$userId/Items" +
                        "?IncludeItemTypes=Audio&Recursive=true&Fields=MediaSources,DateCreated",
            ),
            token = token,
        )

        val items = json.getJSONArray("Items")

        return (0 until items.length()).map { index ->
            items.getJSONObject(index).toJellyfinItem()
        }
    }

    fun streamUrl(baseUrl: String, itemId: String, token: String): String =
        endpoint(baseUrl, "/Audio/$itemId/stream?static=true&api_key=$token")

    fun imageUrl(baseUrl: String, itemId: String, token: String): String =
        endpoint(baseUrl, "/Items/$itemId/Images/Primary?api_key=$token")

    fun fetchLyrics(baseUrl: String, itemId: String, token: String): Lyrics? {
        val json = runCatching {
            getJson(
                url = endpoint(baseUrl, "/Audio/$itemId/Lyrics"),
                token = token,
            )
        }.getOrNull() ?: return null

        val lines = json.optJSONArray("Lyrics") ?: return null

        val entries = (0 until lines.length()).map { index ->
            val entry = lines.getJSONObject(index)
            entry.optString("Text") to entry.optLong("Start", -1L)
        }

        if (entries.all { (text, _) -> text.isBlank() }) return null

        val synced = entries.all { (_, startTicks) -> startTicks >= 0L }
        if (!synced) {
            return LyricsParser.parse(entries.joinToString("\n") { (text, _) -> text })
        }

        return Lyrics(
            lines = entries
                .filter { (text, _) -> text.isNotBlank() }
                .map { (text, startTicks) ->
                    LyricLine(
                        startMs = startTicks / TICKS_PER_MS,
                        endMs = -1L,
                        text = text,
                    )
                }
                .sortedBy { it.startMs }
                .fillLineEnds()
                .withInterludes(),
            synced = true,
        )
    }

    private fun endpoint(baseUrl: String, path: String): String =
        baseUrl.trimEnd('/') + path

    private fun getJson(url: String, token: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Emby-Authorization", authHeader(token))
            .get()
            .build()

        return executeForJson(request)
    }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Emby-Authorization", authHeader())
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return executeForJson(request)
    }

    private fun executeForJson(request: Request): JSONObject =
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "request failed: ${response.code}" }

            JSONObject(response.body.string())
        }

    private fun authHeader(token: String? = null): String =
        buildString {
            append("MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", ")
            append("DeviceId=\"$DEVICE_ID\", Version=\"$CLIENT_VERSION\"")

            if (token != null) {
                append(", Token=\"$token\"")
            }
        }

    private fun JSONObject.toJellyfinItem(): JellyfinItem {
        val mediaSource = optJSONArray("MediaSources")?.optJSONObject(0)

        val artist = optString("AlbumArtist").ifBlank {
            optJSONArray("Artists")?.optString(0).orEmpty()
        }

        return JellyfinItem(
            id = getString("Id"),
            title = optString("Name", "unknown title"),
            artist = artist.ifBlank { "unknown artist" },
            album = optString("Album", "unknown album"),
            albumId = if (has("AlbumId")) optString("AlbumId") else null,
            trackNumber = optInt("IndexNumber", 0),
            durationMs = optLong("RunTimeTicks", 0L) / TICKS_PER_MS,
            mime = "audio/" + optString("Container", "?"),
            bitrate = mediaSource?.optInt("Bitrate", 0) ?: 0,
            size = mediaSource?.optLong("Size", 0L) ?: 0L,
            hasOwnArt = optJSONObject("ImageTags")?.has("Primary") == true,
            dateAdded = runCatching {
                Instant.parse(optString("DateCreated")).epochSecond
            }.getOrDefault(0L),
        )
    }
}
