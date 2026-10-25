package dev.jyotiraditya.dmt.data.remote

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import java.io.IOException

/** This tag, or null when it is blank or the [placeholder] shown for a missing one. */
internal fun String.knownOrNull(placeholder: String): String? = takeIf {
    it.isNotBlank() && it != placeholder
}

/** Whether the service has what was asked for; throws when it failed to answer. */
internal fun HttpResponse.found(): Boolean = when {
    status.isSuccess() -> true
    status == HttpStatusCode.NotFound -> false
    else -> throw IOException("${request.url.host} answered $status")
}
