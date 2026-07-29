package dev.jyotiraditya.dmt.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class TrackSource { LOCAL, JELLYFIN }

@Immutable
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val trackNumber: Int,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val coverUri: Uri? = null,
    val source: TrackSource = TrackSource.LOCAL,
    val remoteId: String? = null,
    val clipStartMs: Long? = null,
    val clipEndMs: Long? = null,
)

@Immutable
data class Album(
    val name: String,
    val artist: String,
    val tracks: List<Track>,
)

@Immutable
data class Artist(
    val name: String,
    val albums: Int,
    val tracks: List<Track>,
)

@Immutable
data class Folder(
    val name: String,
    val path: String,
    val tracks: List<Track>,
)

/**
 * One directory in the indexed music library's storage hierarchy.
 *
 * Only directories that contain indexed tracks (directly or in a subfolder) are
 * ever materialized as a [FolderNode] — this is never a raw filesystem listing.
 */
@Immutable
data class FolderNode(
    val id: String,
    val name: String,
    val absolutePath: String,
    val parentPath: String?,
    val childFolderCount: Int,
    val songCount: Int,
    val artwork: Uri?,
    val lastModified: Long,
    val children: List<FolderNode>,
    val songs: List<Track>,
)

@Immutable
data class Playlist(
    val name: String,
    val tracks: List<Track>,
)

@Immutable
data class Spec(
    val label: String,
    val value: String,
    val hot: Boolean = false,
)

@Immutable
data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
)
