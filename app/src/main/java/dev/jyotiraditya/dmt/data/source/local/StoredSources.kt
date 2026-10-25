package dev.jyotiraditya.dmt.data.source.local

import dev.jyotiraditya.dmt.domain.model.ServerSource
import dev.jyotiraditya.dmt.domain.model.SourceMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

private val json = Json { ignoreUnknownKeys = true }

/** Decodes stored sources, skipping any whose kind is unknown. */
fun String.toSources(): List<ServerSource> = runCatching {
    json.decodeFromString<List<StoredSource>>(this)
}
    .getOrDefault(emptyList())
    .mapNotNull { stored ->
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

/** Encodes these sources for storage. */
fun List<ServerSource>.encodeSources(): String = json.encodeToString(
    map { StoredSource(it.id, it.kind.name, it.name, it.url, it.username, it.secrets) },
)
