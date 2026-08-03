package dev.jyotiraditya.dmt.library.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A track of the library as it is stored, keyed by the path of its file. */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val path: String,
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val dateAdded: Long,
    val dateModified: Long,
    val coverUri: String?,
)
