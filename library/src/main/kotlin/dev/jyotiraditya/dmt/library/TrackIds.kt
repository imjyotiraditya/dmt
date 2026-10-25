package dev.jyotiraditya.dmt.library

private const val FNV_OFFSET_BASIS = -3_750_763_034_362_895_579L
private const val FNV_PRIME = 1_099_511_628_211L

/**
 * Ids for tracks the media store does not index: negative, so they never meet its ids, and taken
 * from the path, so queues and play counts survive a rescan.
 */
object TrackIds {

    fun of(path: String): Long {
        var hash = FNV_OFFSET_BASIS
        path.forEach { character ->
            hash = (hash xor character.code.toLong()) * FNV_PRIME
        }
        return -1L - (hash and Long.MAX_VALUE)
    }
}
