package dev.jyotiraditya.dmt.data.remote

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

/** This tag, or null when it is blank or the [placeholder] shown for a missing one. */
internal fun String.knownOrNull(placeholder: String): String? = takeIf {
    it.isNotBlank() && it != placeholder
}

/** What [request] returns, or null when the service has nothing at that address. */
internal inline fun <T> missingAsNull(request: () -> T): T? = try {
    request()
} catch (failure: ClientRequestException) {
    if (failure.response.status == HttpStatusCode.NotFound) null else throw failure
}
