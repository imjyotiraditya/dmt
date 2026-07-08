package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.toAlbums
import dev.jyotiraditya.dmt.domain.model.toFolders
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanLibraryUseCase @Inject constructor(
    private val mediaSourceProvider: MediaSourceProvider,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(): LibrarySnapshot =
        withContext(dispatchers.io) {
            val tracks = mediaSourceProvider.current().scan()
            LibrarySnapshot(
                tracks = tracks,
                albums = tracks.toAlbums(),
                folders = tracks.toFolders(),
            )
        }
}
