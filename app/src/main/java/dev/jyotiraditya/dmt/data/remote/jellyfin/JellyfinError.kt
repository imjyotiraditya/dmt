package dev.jyotiraditya.dmt.data.remote.jellyfin

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException

/** A failed Jellyfin request, by what the user can do about it. */
enum class JellyfinError {
    /** Wrong address, no network, or the server is down. */
    UNREACHABLE,
    BAD_CREDENTIALS,

    /** The server answered, but not with the Jellyfin API. */
    NOT_JELLYFIN,
}

class JellyfinHttpException(val status: HttpStatusCode) :
    Exception("Jellyfin request failed: $status")

fun Throwable.toJellyfinError(): JellyfinError = when (this) {
    is JellyfinHttpException ->
        when (status) {
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            -> JellyfinError.BAD_CREDENTIALS

            else -> JellyfinError.NOT_JELLYFIN
        }

    is SerializationException -> JellyfinError.NOT_JELLYFIN

    else -> JellyfinError.UNREACHABLE
}

/** The server's base address from what the user typed, with `https://` added and web paths cut. */
fun normalizeServerUrl(input: String): String {
    val trimmed = input.trim()
    val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
    return withScheme
        .substringBefore("/web/")
        .substringBefore("/web#")
        .removeSuffix("/web")
        .trimEnd('/')
}
