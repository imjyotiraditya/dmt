package dev.jyotiraditya.dmt.domain.model

import java.text.Collator

enum class LibrarySort {
    TITLE,
    ARTIST,
    RECENT_ADDED,
    RECENT_MODIFIED,
    RECENT,
    ;

    /** The order this sort lists tracks in; names compare the way the device's language sorts. */
    val comparator: Comparator<Track>
        get() {
            val names = Collator.getInstance().apply { strength = Collator.PRIMARY }
            return when (this) {
                TITLE -> compareBy(names) { it.title }

                ARTIST -> compareBy<Track, String>(names) { it.artist }.thenBy(names) { it.title }

                RECENT_ADDED,
                RECENT,
                -> compareByDescending { it.dateAdded }

                RECENT_MODIFIED -> compareByDescending { it.dateModified }
            }
        }

    companion object {
        fun options(mode: SourceMode): List<LibrarySort> = when (mode) {
            SourceMode.LOCAL -> listOf(TITLE, ARTIST, RECENT_ADDED, RECENT_MODIFIED)

            SourceMode.JELLYFIN,
            SourceMode.SUBSONIC,
            -> listOf(TITLE, ARTIST, RECENT)
        }
    }
}

/** The colour of buttons, toggles and the now-playing marks. */
enum class AccentColor {
    GREEN,
    BLUE,
    RED,
    ORANGE,
    PURPLE,
    MONO,

    /** The system's colours, taken from the wallpaper. */
    WALLPAPER,

    /** The colour of the cover playing now. */
    COVER,
}

data class DmtSettings(
    val accent: AccentColor = AccentColor.GREEN,
    val normalizeVolume: Boolean = false,
    val romanizedLyrics: Boolean = false,
    val singerColors: Boolean = true,
    val animatedArt: Boolean = false,
    val lyricsSource: LyricsSource = LyricsSource.DEFAULT,
    val stopOnDismiss: Boolean = false,
    /** The length the sleep timer was last started with, in minutes. */
    val sleepCustomMinutes: Int = 20,
    val sleepFinishSong: Boolean = true,
    val keepPitch: Boolean = true,
    val setupDone: Boolean = false,
    val hiddenFolders: Set<String> = emptySet(),
    val librarySort: LibrarySort = LibrarySort.TITLE,
    /** The servers added as sources, in the order they were added. */
    val sources: List<ServerSource> = emptyList(),
    /** The [ServerSource.id] of the source in use, or null for this device. */
    val activeSource: String? = null,
) {
    /** The signed-in server in use, or null while music comes from this device. */
    val server: ServerSource?
        get() = sources.firstOrNull { it.id == activeSource && it.signedIn }

    val sourceMode: SourceMode
        get() = server?.kind ?: SourceMode.LOCAL

    /** Adds [source], or replaces the saved source with the same id. */
    fun withSource(source: ServerSource): DmtSettings {
        val index = sources.indexOfFirst { it.id == source.id }
        return copy(
            sources =
                if (index < 0) {
                    sources + source
                } else {
                    sources.toMutableList().apply { set(index, source) }
                },
        )
    }

    /** Signs out of source [id], falling back to this device if it was in use. */
    fun signedOut(id: String): DmtSettings = copy(
        sources = sources.map { if (it.id == id) it.copy(secrets = emptyMap()) else it },
        activeSource = activeSource.takeIf { it != id },
    )

    /** Removes source [id], falling back to this device if it was in use. */
    fun without(id: String): DmtSettings = copy(
        sources = sources.filterNot { it.id == id },
        activeSource = activeSource.takeIf { it != id },
    )
}

data class LastSession(
    val queueIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean = false,
    val shuffleOrder: String = "",
)
