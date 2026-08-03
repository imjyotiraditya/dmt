package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.toAlbums
import dev.jyotiraditya.dmt.domain.model.toArtists
import dev.jyotiraditya.dmt.domain.model.toFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanLibraryUseCase @Inject constructor(
    private val mediaSourceProvider: MediaSourceProvider,
) {
    /** Emits whenever the tracks of the source in use have changed. */
    fun changes(): Flow<Unit> = flow { emitAll(mediaSourceProvider.current().changes()) }

    suspend operator fun invoke(refresh: Boolean = false): LibrarySnapshot =
        withContext(Dispatchers.IO) {
            val source = mediaSourceProvider.current()
            if (refresh) source.invalidate()
            val tracks = source.scan()
            LibrarySnapshot(
                tracks = tracks,
                albums = tracks.toAlbums(),
                artists = tracks.toArtists(),
                folders = tracks.toFolders(),
            )
        }
}
