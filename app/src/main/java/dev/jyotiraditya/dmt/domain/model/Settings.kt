package dev.jyotiraditya.dmt.domain.model

enum class SourceMode(val label: String) {
    LOCAL("local"),
    JELLYFIN("jellyfin"),
}

enum class LibrarySort(val label: String) {
    TITLE("title"),
    ARTIST("artist"),
    RECENT("recent"),
    ;

    val comparator: Comparator<Track>
        get() = when (this) {
            TITLE -> compareBy { it.title.lowercase() }
            ARTIST -> compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
            RECENT -> compareByDescending { it.dateAdded }
        }

    fun next(): LibrarySort = entries[(ordinal + 1) % entries.size]
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
