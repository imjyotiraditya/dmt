package dev.jyotiraditya.dmt.library

import android.net.Uri

/**
 * A track on the device: usually a whole file, or one part of a file a cue sheet describes.
 *
 * @property id the media store id, or a negative one for tracks it does not index.
 * @property dateAdded seconds since the epoch.
 * @property clipStartMs where the track starts in its file, or null at the beginning.
 * @property clipEndMs where the track ends in its file, or null at the end.
 */
data class LibraryTrack(
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
    val dateAdded: Long,
    val dateModified: Long,
    val coverUri: Uri? = null,
    val cue: Boolean = false,
    val clipStartMs: Long? = null,
    val clipEndMs: Long? = null,
    val genre: String? = null,
)
