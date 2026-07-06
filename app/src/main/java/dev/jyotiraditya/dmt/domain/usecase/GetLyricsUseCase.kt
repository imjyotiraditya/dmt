package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.repository.LyricsRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetLyricsUseCase @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(track: Track): Lyrics? =
        withContext(dispatchers.io) {
            lyricsRepository.lyricsFor(track.path, track.mime)
        }
}
