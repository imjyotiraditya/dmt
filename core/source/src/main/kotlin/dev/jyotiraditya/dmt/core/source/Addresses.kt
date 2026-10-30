package dev.jyotiraditya.dmt.core.source

/** A server's address from what the user typed, with `https://` added and the end slash cut. */
fun normalizeServerAddress(input: String): String {
    val trimmed = input.trim()
    val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
    return withScheme.trimEnd('/')
}
