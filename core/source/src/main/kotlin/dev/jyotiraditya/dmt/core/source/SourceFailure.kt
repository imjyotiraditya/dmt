package dev.jyotiraditya.dmt.core.source

/** A failed request to a source, by what the user can do about it. */
enum class SourceFailure {
    /** Wrong address, no network, or the server is down. */
    UNREACHABLE,
    BAD_CREDENTIALS,

    /** The server does not accept the way the app signs in. */
    SIGN_IN_UNSUPPORTED,

    /** Something answered, but not as this kind of source. */
    WRONG_KIND,
}
