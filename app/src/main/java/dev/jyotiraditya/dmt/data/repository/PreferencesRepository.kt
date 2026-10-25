package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.data.source.local.KEY_ACCENT
import dev.jyotiraditya.dmt.data.source.local.KEY_ACTIVE_SOURCE
import dev.jyotiraditya.dmt.data.source.local.KEY_ANIMATED_ART
import dev.jyotiraditya.dmt.data.source.local.KEY_HIDDEN_FOLDERS
import dev.jyotiraditya.dmt.data.source.local.KEY_JELLYFIN_DEVICE_ID
import dev.jyotiraditya.dmt.data.source.local.KEY_KEEP_PITCH
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_INDEX
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_POS
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_QUEUE
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_SCAN_AT
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_SCAN_TRACKS
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_SHUFFLE
import dev.jyotiraditya.dmt.data.source.local.KEY_LAST_SHUFFLE_ORDER
import dev.jyotiraditya.dmt.data.source.local.KEY_LIBRARY_GENERATION
import dev.jyotiraditya.dmt.data.source.local.KEY_LIBRARY_SORT
import dev.jyotiraditya.dmt.data.source.local.KEY_LYRICS_SOURCE
import dev.jyotiraditya.dmt.data.source.local.KEY_NORMALIZE
import dev.jyotiraditya.dmt.data.source.local.KEY_ROMANIZED_LYRICS
import dev.jyotiraditya.dmt.data.source.local.KEY_SETUP_DONE
import dev.jyotiraditya.dmt.data.source.local.KEY_SINGER_COLORS
import dev.jyotiraditya.dmt.data.source.local.KEY_SLEEP_CUSTOM_MINUTES
import dev.jyotiraditya.dmt.data.source.local.KEY_SLEEP_FINISH_SONG
import dev.jyotiraditya.dmt.data.source.local.KEY_SOURCES
import dev.jyotiraditya.dmt.data.source.local.KEY_SPEED
import dev.jyotiraditya.dmt.data.source.local.KEY_STAT_COUNTS
import dev.jyotiraditya.dmt.data.source.local.KEY_STAT_TOTAL
import dev.jyotiraditya.dmt.data.source.local.KEY_STOP_ON_DISMISS
import dev.jyotiraditya.dmt.data.source.local.dmtStore
import dev.jyotiraditya.dmt.data.source.local.encodeCounts
import dev.jyotiraditya.dmt.data.source.local.encodeSources
import dev.jyotiraditya.dmt.data.source.local.toCounts
import dev.jyotiraditya.dmt.data.source.local.toSources
import dev.jyotiraditya.dmt.domain.model.AccentColor
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.DmtStats
import dev.jyotiraditya.dmt.domain.model.LastSession
import dev.jyotiraditya.dmt.domain.model.LibraryScan
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

private const val COUNTED_LISTEN_MS = 30_000L

@Singleton
class PreferencesRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) {

    val settings: Flow<DmtSettings> =
        context.dmtStore.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun save(settings: DmtSettings) {
        context.dmtStore.edit { it.write(settings) }
    }

    /** Changes the saved settings with [transform], reading and writing them in one step. */
    suspend fun update(transform: (DmtSettings) -> DmtSettings) {
        context.dmtStore.edit { it.write(transform(it.toSettings())) }
    }

    suspend fun savedSpeed(): Float = context.dmtStore.data.first()[KEY_SPEED] ?: 1f

    suspend fun saveSpeed(speed: Float) {
        context.dmtStore.edit { it[KEY_SPEED] = speed }
    }

    /** This install's Jellyfin device id, created on first use; Jellyfin keeps a session per id. */
    suspend fun jellyfinDeviceId(): String {
        val prefs =
            context.dmtStore.edit {
                if (KEY_JELLYFIN_DEVICE_ID !in it) {
                    it[KEY_JELLYFIN_DEVICE_ID] = Uuid.random().toHexString()
                }
            }
        return checkNotNull(prefs[KEY_JELLYFIN_DEVICE_ID])
    }

    suspend fun lastSession(): LastSession? {
        val prefs = context.dmtStore.data.first()
        val ids = (prefs[KEY_LAST_QUEUE] ?: "").split(',').mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        return LastSession(
            queueIds = ids,
            index = prefs[KEY_LAST_INDEX] ?: 0,
            positionMs = prefs[KEY_LAST_POS] ?: 0L,
            shuffle = prefs[KEY_LAST_SHUFFLE] ?: false,
            shuffleOrder = prefs[KEY_LAST_SHUFFLE_ORDER] ?: "",
        )
    }

    suspend fun saveSession(session: LastSession) {
        context.dmtStore.edit { prefs ->
            prefs[KEY_LAST_QUEUE] = session.queueIds.joinToString(",")
            prefs[KEY_LAST_INDEX] = session.index
            prefs[KEY_LAST_POS] = session.positionMs
            prefs[KEY_LAST_SHUFFLE] = session.shuffle
            prefs[KEY_LAST_SHUFFLE_ORDER] = session.shuffleOrder
        }
    }

    suspend fun libraryGeneration(): Long =
        context.dmtStore.data.first()[KEY_LIBRARY_GENERATION] ?: -1L

    suspend fun setLibraryGeneration(generation: Long) {
        context.dmtStore.edit { prefs -> prefs[KEY_LIBRARY_GENERATION] = generation }
    }

    /** When the library was last scanned and how many tracks it held, or null before any scan. */
    suspend fun lastScan(): LibraryScan? {
        val prefs = context.dmtStore.data.first()
        val at = prefs[KEY_LAST_SCAN_AT] ?: return null
        return LibraryScan(atMs = at, tracks = prefs[KEY_LAST_SCAN_TRACKS] ?: 0)
    }

    suspend fun saveLastScan(scan: LibraryScan) {
        context.dmtStore.edit { prefs ->
            prefs[KEY_LAST_SCAN_AT] = scan.atMs
            prefs[KEY_LAST_SCAN_TRACKS] = scan.tracks
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

    val stats: Flow<DmtStats> =
        context.dmtStore.data
            .map { prefs ->
                DmtStats(
                    totalMs = prefs[KEY_STAT_TOTAL] ?: 0L,
                    counts = (prefs[KEY_STAT_COUNTS] ?: "").toCounts(),
                )
            }
            .distinctUntilChanged()
}

private fun Preferences.toSettings(): DmtSettings = DmtSettings(
    accent = this[KEY_ACCENT].toEnum(AccentColor.GREEN),
    normalizeVolume = this[KEY_NORMALIZE] ?: false,
    romanizedLyrics = this[KEY_ROMANIZED_LYRICS] ?: false,
    singerColors = this[KEY_SINGER_COLORS] ?: true,
    animatedArt = this[KEY_ANIMATED_ART] ?: false,
    // Online sources are picked per track and never saved.
    lyricsSource =
        this[KEY_LYRICS_SOURCE].toEnum(LyricsSource.DEFAULT).takeIf { !it.online }
            ?: LyricsSource.DEFAULT,
    stopOnDismiss = this[KEY_STOP_ON_DISMISS] ?: false,
    sleepCustomMinutes = this[KEY_SLEEP_CUSTOM_MINUTES] ?: 20,
    sleepFinishSong = this[KEY_SLEEP_FINISH_SONG] ?: true,
    keepPitch = this[KEY_KEEP_PITCH] ?: true,
    setupDone = this[KEY_SETUP_DONE] ?: false,
    hiddenFolders = this[KEY_HIDDEN_FOLDERS] ?: emptySet(),
    librarySort = this[KEY_LIBRARY_SORT].toEnum(LibrarySort.TITLE),
    sources = this[KEY_SOURCES]?.toSources().orEmpty(),
    activeSource = this[KEY_ACTIVE_SOURCE],
)

private fun MutablePreferences.write(settings: DmtSettings) {
    this[KEY_ACCENT] = settings.accent.name
    this[KEY_NORMALIZE] = settings.normalizeVolume
    this[KEY_ROMANIZED_LYRICS] = settings.romanizedLyrics
    this[KEY_SINGER_COLORS] = settings.singerColors
    this[KEY_ANIMATED_ART] = settings.animatedArt
    this[KEY_LYRICS_SOURCE] = settings.lyricsSource.name
    this[KEY_STOP_ON_DISMISS] = settings.stopOnDismiss
    this[KEY_SLEEP_CUSTOM_MINUTES] = settings.sleepCustomMinutes
    this[KEY_SLEEP_FINISH_SONG] = settings.sleepFinishSong
    this[KEY_KEEP_PITCH] = settings.keepPitch
    this[KEY_SETUP_DONE] = settings.setupDone
    this[KEY_HIDDEN_FOLDERS] = settings.hiddenFolders
    this[KEY_LIBRARY_SORT] = settings.librarySort.name
    this[KEY_SOURCES] = settings.sources.encodeSources()
    putOrRemove(KEY_ACTIVE_SOURCE, settings.activeSource)
}

/** The entry of [E] with this name, or [default] when there is none. */
private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default

private fun <T> MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
    if (value != null) this[key] = value else remove(key)
}
