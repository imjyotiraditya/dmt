package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.toAlbums
import dev.jyotiraditya.dmt.domain.model.toArtists
import dev.jyotiraditya.dmt.domain.model.toFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanLibraryUseCase @Inject constructor(
    private val mediaSourceProvider: MediaSourceProvider,
) {
    suspend operator fun invoke(): LibrarySnapshot =
        withContext(Dispatchers.IO) {
            val tracks = mediaSourceProvider.current().scan()
            LibrarySnapshot(
                tracks = tracks,
                albums = tracks.toAlbums(),
                artists = tracks.toArtists(),
                folders = tracks.toFolders(),
            )
        }
}
