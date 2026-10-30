package dev.jyotiraditya.dmt.source.jellyfin

import dev.jyotiraditya.dmt.core.source.SourceFailure
import dev.jyotiraditya.dmt.core.source.normalizeServerAddress
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException

/** What the user can do about this failed Jellyfin request. */
internal fun Throwable.toSourceFailure(): SourceFailure = when (this) {
    is ResponseException ->
        when (response.status) {
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            -> SourceFailure.BAD_CREDENTIALS

            else -> SourceFailure.WRONG_KIND
        }

    is ContentConvertException,
    is NoTransformationFoundException,
    -> SourceFailure.WRONG_KIND

    else -> SourceFailure.UNREACHABLE
}

/** The server's base address from what the user typed, with its web app paths cut. */
fun normalizeJellyfinUrl(input: String): String = normalizeServerAddress(input)
    .substringBefore("/web/")
    .substringBefore("/web#")
    .removeSuffix("/web")
    .trimEnd('/')
