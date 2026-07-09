package dev.jyotiraditya.dmt.domain.usecase

import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.AudioJourney
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.repository.TrackMediaRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAudioJourneyUseCase @Inject constructor(
    private val trackMediaRepository: TrackMediaRepository,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(
        uri: Uri,
        track: Track?,
        speed: Float,
        audioSessionId: Int,
    ): AudioJourney =
        withContext(dispatchers.io) {
            trackMediaRepository.audioJourney(uri, track, speed, audioSessionId)
        }
}
