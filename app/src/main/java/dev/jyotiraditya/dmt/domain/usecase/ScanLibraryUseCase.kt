package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.toLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the library from the current source and groups it, once per scan for every caller. */
@Singleton
class ScanLibraryUseCase @Inject constructor(private val mediaSourceProvider: MediaSourceProvider) {

    @Volatile private var grouped: LibrarySnapshot? = null

    fun changes(): Flow<Unit> = flow { emitAll(mediaSourceProvider.current().changes()) }

    /**
     * The library as stored, then as scanned now, each only when it changed; [refresh] skips the
     * stored one.
     */
    operator fun invoke(refresh: Boolean = false): Flow<LibrarySnapshot> = flow {
        val source = mediaSourceProvider.current()
        if (refresh) {
            source.invalidate()
        } else {
            source.stored().takeIf { it.isNotEmpty() }?.let { emit(it) }
        }
        emit(source.scan())
    }
        .distinctUntilChanged()
        .map(::snapshotOf)
        .flowOn(Dispatchers.IO)

    /** The library as the current source holds it now. */
    suspend fun current(): LibrarySnapshot = snapshotOf(mediaSourceProvider.current().scan())

    private suspend fun snapshotOf(tracks: List<Track>): LibrarySnapshot {
        grouped
            ?.takeIf { it.tracks === tracks }
            ?.let {
                return it
            }
        return withContext(Dispatchers.Default) { tracks.toLibrary() }.also { grouped = it }
    }
}
