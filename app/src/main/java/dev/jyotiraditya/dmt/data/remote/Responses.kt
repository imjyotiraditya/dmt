package dev.jyotiraditya.dmt.data.remote

/** This tag, or null when it is blank or the [placeholder] shown for a missing one. */
internal fun String.knownOrNull(placeholder: String): String? = takeIf {
    it.isNotBlank() && it != placeholder
}
