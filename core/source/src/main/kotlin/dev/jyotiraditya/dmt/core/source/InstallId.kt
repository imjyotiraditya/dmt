package dev.jyotiraditya.dmt.core.source

/** An id for this install of the app, made once and kept. */
fun interface InstallId {
    suspend fun get(): String
}
