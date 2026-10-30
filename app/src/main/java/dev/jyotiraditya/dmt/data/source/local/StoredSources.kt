package dev.jyotiraditya.dmt.data.source.local

import dev.jyotiraditya.dmt.core.model.ServerSource
import dev.jyotiraditya.dmt.core.model.SourceMode
import kotlinx.serialization.Serializable

/** A [ServerSource] as stored, with its kind kept by name. */
@Serializable
private data class StoredSource(
    val id: String,
    val kind: String,
    val name: String,
    val url: String,
    val username: String,
    val secrets: Map<String, String> = emptyMap(),
)

/** Decodes stored sources, skipping any whose kind is unknown. */
fun String.toSources(): List<ServerSource> {
    val sources =
        try {
            StoreJson.decodeFromString<List<StoredSource>>(this)
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    return sources.mapNotNull { stored ->
        ServerSource(
            id = stored.id,
            kind =
                SourceMode.entries.firstOrNull { it.name == stored.kind } ?: return@mapNotNull null,
            name = stored.name,
            url = stored.url,
            username = stored.username,
            secrets = stored.secrets,
        )
    }
}

/** Encodes these sources for storage. */
fun List<ServerSource>.encodeSources(): String = StoreJson.encodeToString(
    map { StoredSource(it.id, it.kind.name, it.name, it.url, it.username, it.secrets) },
)
