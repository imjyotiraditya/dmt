package dev.jyotiraditya.dmt.domain.repository

import dev.jyotiraditya.dmt.domain.model.Lyrics

interface LyricsRepository {
    fun lyricsFor(path: String, mime: String): Lyrics?
}
