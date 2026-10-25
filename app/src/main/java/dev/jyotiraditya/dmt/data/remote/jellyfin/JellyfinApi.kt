package dev.jyotiraditya.dmt.data.remote.jellyfin

import dev.jyotiraditya.dmt.domain.model.ServerSource
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ALBUM
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_TITLE
import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.LyricsParser
import dev.jyotiraditya.lyrics.fillLineEnds
import dev.jyotiraditya.lyrics.withInterludes
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

data class JellyfinAuth(val userId: String, val token: String) {
    /** The sign-in data saved for a Jellyfin source. */
    val secrets: Map<String, String>
        get() = mapOf("userId" to userId, "token" to token)
}

/** A signed-in Jellyfin server: its address, the user and the session token. */
data class JellyfinAccount(val url: String, val userId: String, val token: String)

/** This source's Jellyfin account, or null if it is another kind or signed out. */
fun ServerSource.jellyfinAccount(): JellyfinAccount? {
    if (kind != SourceMode.JELLYFIN) return null
    return JellyfinAccount(
        url = url,
        userId = secrets["userId"] ?: return null,
        token = secrets["token"] ?: return null,
    )
}

data class JellyfinItem(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val albumId: String?,
    val trackNumber: Int,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val hasOwnArt: Boolean,
    val dateAdded: Long,
    val codec: String?,
    val bitDepth: Int?,
    val sampleRateHz: Int?,
)

private const val CLIENT_NAME = "DMT"
private const val CLIENT_VERSION = "1"
private const val DEVICE_NAME = "Android"

private const val TICKS_PER_MS = 10_000L
private const val PRIMARY_IMAGE = "Primary"

@Singleton
class JellyfinApi
@Inject
constructor(
    private val client: HttpClient,
    private val json: Json,
    private val deviceId: JellyfinDeviceId,
) {

    suspend fun authenticate(baseUrl: String, username: String, password: String): JellyfinAuth {
        val result: AuthenticationResult =
            client
                .post(endpoint(baseUrl, "/Users/AuthenticateByName")) {
                    authorize(device())
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(AuthenticateRequest(username, password)))
                }
                .decode()
        return JellyfinAuth(userId = result.user.id, token = result.accessToken)
    }

    suspend fun fetchAudioItems(
        baseUrl: String,
        userId: String,
        token: String,
    ): List<JellyfinItem> {
        val url =
            endpoint(baseUrl, "/Users/$userId/Items") {
                append("IncludeItemTypes", "Audio")
                append("Recursive", "true")
                append("Fields", "MediaSources,DateCreated")
            }
        val result: ItemsResult = client.get(url) { authorize(device(), token) }.decode()
        return result.items.map { it.toJellyfinItem() }
    }

    fun streamUrl(baseUrl: String, itemId: String, token: String): String =
        endpoint(baseUrl, "/Audio/$itemId/stream") {
            append("static", "true")
            append("api_key", token)
        }

    fun imageUrl(baseUrl: String, itemId: String, token: String): String =
        endpoint(baseUrl, "/Items/$itemId/Images/$PRIMARY_IMAGE") { append("api_key", token) }

    /** The server's lyrics for [itemId], or null when it has none; throws if unreachable. */
    suspend fun fetchLyrics(baseUrl: String, itemId: String, token: String): Lyrics? {
        val lines =
            try {
                client
                    .get(endpoint(baseUrl, "/Audio/$itemId/Lyrics")) { authorize(device(), token) }
                    .decode<LyricsResult>()
                    .lines
            } catch (missing: JellyfinHttpException) {
                if (missing.status == HttpStatusCode.NotFound) return null
                throw missing
            }

        if (lines.all { it.text.isBlank() }) return null
        if (lines.any { it.start == null }) {
            return LyricsParser.parse(lines.joinToString("\n") { it.text })
        }
        return Lyrics(
            lines =
                lines
                    .filter { it.text.isNotBlank() }
                    .mapNotNull { line ->
                        line.start?.let {
                            LyricLine(
                                startMs = it / TICKS_PER_MS,
                                endMs = -1L,
                                text = line.text,
                            )
                        }
                    }
                    .sortedBy { it.startMs }
                    .fillLineEnds()
                    .withInterludes(),
            synced = true,
        )
    }

    private fun endpoint(
        baseUrl: String,
        path: String,
        query: ParametersBuilder.() -> Unit = {},
    ): String = URLBuilder(baseUrl.trimEnd('/') + path).apply { parameters.query() }.buildString()

    private suspend fun device(): String = deviceId.get()

    private fun HttpRequestBuilder.authorize(device: String, token: String? = null) {
        header(HttpHeaders.Authorization, authHeader(device, token))
    }

    /** The body as [T]; a failed status throws [JellyfinHttpException]. */
    private suspend inline fun <reified T> HttpResponse.decode(): T {
        if (!status.isSuccess()) throw JellyfinHttpException(status)
        return json.decodeFromString(bodyAsText())
    }

    private fun authHeader(device: String, token: String?): String = buildString {
        append("MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", ")
        append("DeviceId=\"$device\", Version=\"$CLIENT_VERSION\"")
        if (token != null) append(", Token=\"$token\"")
    }
}

private fun ItemDto.toJellyfinItem(): JellyfinItem {
    val albumArtist = albumArtist.orEmpty()
    val source = mediaSources.firstOrNull()
    val audio = source?.streams?.firstOrNull { it.type == "Audio" }
    return JellyfinItem(
        id = id,
        title = name ?: UNKNOWN_TITLE,
        artist = artists.joinToString("; ").ifBlank { albumArtist }.ifBlank { UNKNOWN_ARTIST },
        albumArtist = albumArtist,
        album = album ?: UNKNOWN_ALBUM,
        albumId = albumId,
        trackNumber = indexNumber ?: 0,
        durationMs = (runTimeTicks ?: 0L) / TICKS_PER_MS,
        mime = "audio/" + (audio?.codec ?: container ?: "?"),
        bitrate = source?.bitrate ?: 0,
        size = source?.size ?: 0L,
        hasOwnArt = PRIMARY_IMAGE in imageTags,
        dateAdded =
            dateCreated?.let { runCatching { Instant.parse(it).epochSeconds }.getOrNull() } ?: 0L,
        codec = audio?.codec,
        bitDepth = audio?.bitDepth,
        sampleRateHz = audio?.sampleRate,
    )
}
