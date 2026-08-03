package dev.jyotiraditya.dmt.library

import android.net.Uri

/**
 * A track that the device holds.
 *
 * A track is usually a whole file, but a file that a cue sheet describes holds several, in which
 * case each one covers the part of the file between [clipStartMs] and [clipEndMs].
 */
data class LibraryTrack(
    /** Identifies the track, and is negative for a track that the platform does not index. */
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val path: String,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val trackNumber: Int,
    val discNumber: Int,
    /** Seconds since the epoch, as the platform reports them. */
    val dateAdded: Long,
    val dateModified: Long,
    val coverUri: Uri? = null,
    /** Whether a cue sheet describes this track rather than the file holding it alone. */
    val cue: Boolean = false,
    /** Where the track starts in its file, or null if it starts at the beginning. */
    val clipStartMs: Long? = null,
    /** Where the track ends in its file, or null if it ends with the file. */
    val clipEndMs: Long? = null,
)
