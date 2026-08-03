package dev.jyotiraditya.dmt.domain.repository

import dev.jyotiraditya.dmt.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface MediaRepository {
    suspend fun scan(): List<Track>

    fun invalidate() = Unit

    /** Emits whenever the tracks this holds have changed and are worth reading again. */
    fun changes(): Flow<Unit> = emptyFlow()
}
