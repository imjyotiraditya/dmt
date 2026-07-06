package dev.jyotiraditya.dmt.domain.usecase

import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.repository.TrackMediaRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetTrackTechUseCase @Inject constructor(
    private val trackMediaRepository: TrackMediaRepository,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(uri: Uri, track: Track?): List<Spec> =
        withContext(dispatchers.io) {
            trackMediaRepository.techSpecs(uri, track)
        }
}
