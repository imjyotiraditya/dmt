package dev.jyotiraditya.dmt.domain.model

/** Where music comes from: this device or a kind of server. */
enum class SourceMode {
    LOCAL,
    JELLYFIN,
    SUBSONIC,
}

/** A server added as a music source. */
data class ServerSource(
    val id: String,
    val kind: SourceMode,
    val name: String,
    val url: String,
    val username: String,
    /** Sign-in data for [kind], such as a token; empty when signed out. */
    val secrets: Map<String, String> = emptyMap(),
) {
    val signedIn: Boolean
        get() = secrets.isNotEmpty()
}

/** A source as entered in its form; [id] is null for a new source. */
data class SourceDraft(
    val id: String?,
    val kind: SourceMode,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
)
