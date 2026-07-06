package dev.jyotiraditya.dmt.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import dev.jyotiraditya.dmt.domain.repository.TrackMediaRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetCoverArtUseCase @Inject constructor(
    private val trackMediaRepository: TrackMediaRepository,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(uri: Uri): Bitmap? =
        withContext(dispatchers.io) {
            trackMediaRepository.loadArt(uri)
        }
}
