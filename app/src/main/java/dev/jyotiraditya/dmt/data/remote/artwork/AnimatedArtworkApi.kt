package dev.jyotiraditya.dmt.data.remote.artwork

import dev.jyotiraditya.dmt.data.remote.DMT_USER_AGENT
import dev.jyotiraditya.dmt.data.remote.found
import dev.jyotiraditya.dmt.util.suspendRunCatching
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

private const val BASE_URL = "https://artwork.m8tec.top/api/v1/artwork/search"

@Serializable private data class ArtworkResult(val url: String? = null)

/** Looks up animated album covers by artist and album. */
@Singleton
class AnimatedArtworkApi
@Inject
constructor(
    private val client: HttpClient,
    private val json: Json,
) {

    /**
     * The animated cover of [album] by [artist], null if it has none, or a failure if the service
     * could not be reached.
     */
    suspend fun fetchStreamUrl(artist: String, album: String): Result<String?> =
        suspendRunCatching {
            val response =
                client.get(BASE_URL) {
                    parameter("artist", artist)
                    parameter("album", album)
                    header(HttpHeaders.UserAgent, DMT_USER_AGENT)
                }
            if (!response.found()) return@suspendRunCatching null
            json.decodeFromString<ArtworkResult>(response.bodyAsText()).url?.ifBlank { null }
        }
}
