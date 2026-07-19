package dev.jyotiraditya.dmt.domain.usecase

import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.data.repository.TrackMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetTrackTechUseCase @Inject constructor(
    private val trackMediaRepository: TrackMediaRepository,
) {
    suspend operator fun invoke(uri: Uri, track: Track?): List<Spec> =
        withContext(Dispatchers.IO) {
            trackMediaRepository.techSpecs(uri, track)
        }
}
