package dev.jyotiraditya.dmt.domain.repository

import dev.jyotiraditya.dmt.domain.model.DmtStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    val stats: Flow<DmtStats>

    suspend fun recordPlayback(playedMs: Long, trackId: Long?)
}
