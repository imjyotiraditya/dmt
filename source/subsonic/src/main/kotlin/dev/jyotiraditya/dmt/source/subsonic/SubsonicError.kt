package dev.jyotiraditya.dmt.source.subsonic

import dev.jyotiraditya.dmt.core.source.SourceFailure
import dev.jyotiraditya.dmt.core.source.normalizeServerAddress
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException

private const val WRONG_CREDENTIALS = 40
private const val TOKEN_AUTH_UNSUPPORTED = 41

/** A failure the Subsonic server reported, with its error [code]. */
class SubsonicException(val code: Int, message: String) : Exception(message)

/** What the user can do about this failed Subsonic request. */
internal fun Throwable.toSourceFailure(): SourceFailure = when (this) {
    is SubsonicException ->
        when (code) {
            WRONG_CREDENTIALS -> SourceFailure.BAD_CREDENTIALS
            TOKEN_AUTH_UNSUPPORTED -> SourceFailure.SIGN_IN_UNSUPPORTED
            else -> SourceFailure.WRONG_KIND
        }

    is ResponseException,
    is ContentConvertException,
    is NoTransformationFoundException,
    is IllegalArgumentException,
    -> SourceFailure.WRONG_KIND

    else -> SourceFailure.UNREACHABLE
}

/** The server's base address from what the user typed, with its web app and API paths cut. */
fun normalizeSubsonicUrl(input: String): String = normalizeServerAddress(input)
    .substringBefore("/web/")
    .substringBefore("/web#")
    .removeSuffix("/web")
    .substringBefore("/app/")
    .substringBefore("/app#")
    .removeSuffix("/app")
    .substringBefore("/rest/")
    .removeSuffix("/rest")
    .trimEnd('/')
