package dev.jyotiraditya.dmt.data.repository

import dev.jyotiraditya.dmt.data.source.local.lyrics.LyricsExtractor
import dev.jyotiraditya.dmt.data.source.local.lyrics.LyricsParser
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.repository.LyricsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor() : LyricsRepository {

    override fun lyricsFor(path: String, mime: String): Lyrics? =
        LyricsExtractor.extract(path, mime)?.let(LyricsParser::parse)
}
