package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.toAlbums
import dev.jyotiraditya.dmt.domain.model.toFolders
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanLibraryUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(): LibrarySnapshot =
        withContext(dispatchers.io) {
            val tracks = mediaRepository.scan()
            LibrarySnapshot(
                tracks = tracks,
                albums = tracks.toAlbums(),
                folders = tracks.toFolders(),
            )
        }
}
