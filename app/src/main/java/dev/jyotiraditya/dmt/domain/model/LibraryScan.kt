package dev.jyotiraditya.dmt.domain.model

/**
 * A finished library scan: when it ended and how many tracks it found, with the tracks it [added]
 * and [removed] when it was a rescan compared against the library before it.
 */
data class LibraryScan(
    val atMs: Long,
    val tracks: Int,
    val added: Int? = null,
    val removed: Int? = null,
) {
    companion object {
        /**
         * The scan that left [after] in place of [before], ending now; with [compare] it also
         * counts the tracks it added and removed.
         */
        fun between(before: List<Track>, after: List<Track>, compare: Boolean): LibraryScan {
            val atMs = System.currentTimeMillis()
            if (!compare) return LibraryScan(atMs = atMs, tracks = after.size)
            val old = before.mapTo(HashSet(), Track::id)
            val new = after.mapTo(HashSet(), Track::id)
            return LibraryScan(
                atMs = atMs,
                tracks = after.size,
                added = new.count { it !in old },
                removed = old.count { it !in new },
            )
        }
    }
}
