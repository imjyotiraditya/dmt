package dev.jyotiraditya.dmt.data.remote.subsonic

import dev.jyotiraditya.dmt.data.remote.jellyfin.normalizeServerUrl
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException

private const val WRONG_CREDENTIALS = 40
private const val TOKEN_AUTH_UNSUPPORTED = 41

/** A failed Subsonic request, by what the user can do about it. */
enum class SubsonicError {
    UNREACHABLE,
    BAD_CREDENTIALS,

    /** The server does not accept token sign-in. */
    TOKEN_UNSUPPORTED,

    /** Something answered, but not as a Subsonic server. */
    NOT_SUBSONIC,
}

class SubsonicException(val code: Int, message: String) : Exception(message)

fun Throwable.toSubsonicError(): SubsonicError = when (this) {
    is SubsonicException ->
        when (code) {
            WRONG_CREDENTIALS -> SubsonicError.BAD_CREDENTIALS
            TOKEN_AUTH_UNSUPPORTED -> SubsonicError.TOKEN_UNSUPPORTED
            else -> SubsonicError.NOT_SUBSONIC
        }

    is ResponseException,
    is ContentConvertException,
    is NoTransformationFoundException,
    is IllegalArgumentException,
    -> SubsonicError.NOT_SUBSONIC

    else -> SubsonicError.UNREACHABLE
}

fun normalizeSubsonicUrl(input: String): String = normalizeServerUrl(input)
    .substringBefore("/app/")
    .substringBefore("/app#")
    .removeSuffix("/app")
    .substringBefore("/rest/")
    .removeSuffix("/rest")
    .trimEnd('/')
