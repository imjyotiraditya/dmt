package dev.jyotiraditya.dmt.domain.model

enum class SourceMode(val label: String) {
    LOCAL("local"),
    JELLYFIN("jellyfin"),
}

enum class LibrarySort(val label: String) {
    TITLE("title"),
    ARTIST("artist"),
    RECENT_ADDED("recent-a"),
    RECENT_MODIFIED("recent-m"),
    RECENT("recent"),
    ;

    val comparator: Comparator<Track>
        get() = when (this) {
            TITLE -> compareBy { it.title.lowercase() }
            ARTIST -> compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
            RECENT_ADDED, RECENT -> compareByDescending { it.dateAdded }
            RECENT_MODIFIED -> compareByDescending { it.dateModified }
        }

    fun next(mode: SourceMode): LibrarySort {
        val cycle = when (mode) {
            SourceMode.LOCAL -> listOf(TITLE, ARTIST, RECENT_ADDED, RECENT_MODIFIED)
            SourceMode.JELLYFIN -> listOf(TITLE, ARTIST, RECENT)
        }
        return cycle[(cycle.indexOf(this) + 1) % cycle.size]
    }
}

data class DmtSettings(
    val wave: Boolean = true,
    val cols: Int = 96,
    val listSpecs: Boolean = true,
    val romanizedLyrics: Boolean = false,
    val rawArt: Boolean = false,
    val blockedFolders: Set<String> = emptySet(),
    val sourceMode: SourceMode = SourceMode.LOCAL,
    val librarySort: LibrarySort = LibrarySort.TITLE,
    val jellyfinUrl: String? = null,
    val jellyfinUserId: String? = null,
    val jellyfinToken: String? = null,
)

data class LastSession(
    val queueIds: List<Long>,
    val index: Int,
    val positionMs: Long,
)
