package dev.jyotiraditya.dmt.data.remote

import dev.jyotiraditya.dmt.data.repository.SourceRepository
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import javax.inject.Provider

/** A client plugin that adds the headers the source in use needs to each request. */
fun sourceHeaders(sources: Provider<SourceRepository>): ClientPlugin<Unit> =
    createClientPlugin("SourceHeaders") {
        onRequest { request, _ ->
            sources.get().headersFor(request.url.buildString()).forEach { (name, value) ->
                request.headers[name] = value
            }
        }
    }
