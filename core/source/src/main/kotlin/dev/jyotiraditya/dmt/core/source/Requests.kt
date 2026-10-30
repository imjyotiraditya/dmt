package dev.jyotiraditya.dmt.core.source

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

/** What [request] returns, or null when the service has nothing at that address. */
inline fun <T> missingAsNull(request: () -> T): T? = try {
    request()
} catch (failure: ClientRequestException) {
    if (failure.response.status == HttpStatusCode.NotFound) null else throw failure
}
