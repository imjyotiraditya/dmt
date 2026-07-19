package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.data.source.local.KEY_BLOCKED_FOLDERS
import dev.jyotiraditya.dmt.data.source.local.KEY_COLS
import dev.jyotiraditya.dmt.data.source.local.KEY_JELLYFIN_TOKEN
import dev.jyotiraditya.dmt.data.source.local.KEY_JELLYFIN_URL
import dev.jyotiraditya.dmt.data.source.local.KEY_JELLYFIN_USER_ID
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_INDEX
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_POS
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_QUEUE
import dev.jyotiraditya.dmt.data.source.local.KEY_LIBRARY_SORT
import dev.jyotiraditya.dmt.data.source.local.KEY_RAW
import dev.jyotiraditya.dmt.data.source.local.KEY_ROMANIZED_LYRICS
import dev.jyotiraditya.dmt.data.source.local.KEY_SOURCE_MODE
import dev.jyotiraditya.dmt.data.source.local.KEY_SPECS
import dev.jyotiraditya.dmt.data.source.local.KEY_SPEED
import dev.jyotiraditya.dmt.data.source.local.KEY_STAT_COUNTS
import dev.jyotiraditya.dmt.data.source.local.KEY_STAT_TOTAL
import dev.jyotiraditya.dmt.data.source.local.KEY_WAVE
import dev.jyotiraditya.dmt.data.source.local.dmtStore
import dev.jyotiraditya.dmt.data.source.local.encodeCounts
import dev.jyotiraditya.dmt.data.source.local.toCounts
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.DmtStats
import dev.jyotiraditya.dmt.domain.model.LastSession
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.SourceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val COUNTED_LISTEN_MS = 30_000L

@Singleton
class PreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    val settings: Flow<DmtSettings> = context.dmtStore.data.map { prefs ->
        DmtSettings(
            wave = prefs[KEY_WAVE] ?: true,
            cols = prefs[KEY_COLS] ?: 96,
            listSpecs = prefs[KEY_SPECS] ?: true,
            romanizedLyrics = prefs[KEY_ROMANIZED_LYRICS] ?: false,
            rawArt = prefs[KEY_RAW] ?: false,
            blockedFolders = prefs[KEY_BLOCKED_FOLDERS] ?: emptySet(),
            sourceMode = SourceMode.entries[(prefs[KEY_SOURCE_MODE]
                ?: 0).mod(SourceMode.entries.size)],
            librarySort = LibrarySort.entries[(prefs[KEY_LIBRARY_SORT]
                ?: 0).mod(LibrarySort.entries.size)],
            jellyfinUrl = prefs[KEY_JELLYFIN_URL],
            jellyfinUserId = prefs[KEY_JELLYFIN_USER_ID],
            jellyfinToken = prefs[KEY_JELLYFIN_TOKEN],
        )
    }

    suspend fun save(settings: DmtSettings) {
        context.dmtStore.edit {
            it[KEY_WAVE] = settings.wave
            it[KEY_COLS] = settings.cols
            it[KEY_SPECS] = settings.listSpecs
            it[KEY_ROMANIZED_LYRICS] = settings.romanizedLyrics
            it[KEY_RAW] = settings.rawArt
            it[KEY_BLOCKED_FOLDERS] = settings.blockedFolders
            it[KEY_SOURCE_MODE] = settings.sourceMode.ordinal
            it[KEY_LIBRARY_SORT] = settings.librarySort.ordinal
            settings.jellyfinUrl
                ?.let { url -> it[KEY_JELLYFIN_URL] = url }
                ?: it.remove(KEY_JELLYFIN_URL)
            settings.jellyfinUserId
                ?.let { userId -> it[KEY_JELLYFIN_USER_ID] = userId }
                ?: it.remove(KEY_JELLYFIN_USER_ID)
            settings.jellyfinToken
                ?.let { token -> it[KEY_JELLYFIN_TOKEN] = token }
                ?: it.remove(KEY_JELLYFIN_TOKEN)
        }
    }

    suspend fun savedSpeed(): Float = context.dmtStore.data.first()[KEY_SPEED] ?: 1f

    suspend fun saveSpeed(speed: Float) {
        context.dmtStore.edit { it[KEY_SPEED] = speed }
    }

    suspend fun lastSession(): LastSession? {
        val prefs = context.dmtStore.data.first()
        val ids = (prefs[KEY_LAST_QUEUE] ?: "")
            .split(',')
            .mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        return LastSession(
            queueIds = ids,
            index = prefs[KEY_LAST_INDEX] ?: 0,
            positionMs = prefs[KEY_LAST_POS] ?: 0L,
        )
    }

    suspend fun saveSession(session: LastSession) {
        context.dmtStore.edit { prefs ->
            prefs[KEY_LAST_QUEUE] = session.queueIds.joinToString(",")
            prefs[KEY_LAST_INDEX] = session.index
            prefs[KEY_LAST_POS] = session.positionMs
        }
    }

    suspend fun recordPlayback(playedMs: Long, trackId: Long?) {
        context.dmtStore.edit { prefs ->
            prefs[KEY_STAT_TOTAL] = (prefs[KEY_STAT_TOTAL] ?: 0L) + playedMs
            if (playedMs >= COUNTED_LISTEN_MS && trackId != null) {
                val counts = (prefs[KEY_STAT_COUNTS] ?: "").toCounts().toMutableMap()
                counts[trackId] = (counts[trackId] ?: 0) + 1
                prefs[KEY_STAT_COUNTS] = counts.encodeCounts()
            }
        }
    }

    val stats: Flow<DmtStats> = context.dmtStore.data.map { prefs ->
        DmtStats(
            totalMs = prefs[KEY_STAT_TOTAL] ?: 0L,
            counts = (prefs[KEY_STAT_COUNTS] ?: "").toCounts(),
        )
    }
}
