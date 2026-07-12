package dev.jyotiraditya.dmt.data.repository

import dev.jyotiraditya.dmt.data.source.local.lyrics.LyricsParser
import dev.jyotiraditya.dmt.data.source.local.lyrics.LyricsTags
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.repository.LyricsRepository
import dev.jyotiraditya.metadata.AudioTags
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor() : LyricsRepository {

    override fun lyricsFor(path: String, mime: String): Lyrics? =
        LyricsTags.bestOf(AudioTags.read(path))?.let(LyricsParser::parse)
}
