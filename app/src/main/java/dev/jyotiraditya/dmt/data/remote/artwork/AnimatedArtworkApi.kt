package dev.jyotiraditya.dmt.data.remote.artwork

import dev.jyotiraditya.dmt.core.source.missingAsNull
import dev.jyotiraditya.dmt.util.suspendRunCatching
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://artwork.m8tec.top/api/v1/artwork/search"

@Serializable private data class ArtworkResult(val url: String? = null)

/** Looks up animated album covers by artist and album. */
@Singleton
class AnimatedArtworkApi @Inject constructor(private val client: HttpClient) {

    /**
     * The animated cover of [album] by [artist], null if it has none, or a failure if the service
     * could not be reached.
     */
    suspend fun fetchStreamUrl(artist: String, album: String): Result<String?> =
        suspendRunCatching {
            missingAsNull {
                client
                    .get(BASE_URL) {
                        parameter("artist", artist)
                        parameter("album", album)
                    }
                    .body<ArtworkResult>()
            }
                ?.url
                ?.ifBlank { null }
        }
}
