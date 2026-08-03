package dev.jyotiraditya.dmt.library

private const val FNV_OFFSET_BASIS = -3_750_763_034_362_895_579L
private const val FNV_PRIME = 1_099_511_628_211L

/**
 * Ids for tracks that the platform does not index, which it therefore does not give an id.
 *
 * <p>The ids are negative, so that they cannot collide with the ids of indexed tracks, and they are
 * derived from the path of a file rather than assigned, so that a saved queue and the play counts of
 * a track survive a rescan.
 */
object TrackIds {

    /** Returns the id of the track at [path]. */
    fun of(path: String): Long {
        var hash = FNV_OFFSET_BASIS
        path.forEach { character ->
            hash = (hash xor character.code.toLong()) * FNV_PRIME
        }
        return -1L - (hash and Long.MAX_VALUE)
    }
}
