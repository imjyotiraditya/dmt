package dev.jyotiraditya.dmt.domain.repository

import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LastSession
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<DmtSettings>
    suspend fun save(settings: DmtSettings)
    suspend fun savedSpeed(): Float
    suspend fun saveSpeed(speed: Float)
    suspend fun lastSession(): LastSession?

    suspend fun saveSession(session: LastSession)
}
