package dev.jyotiraditya.dmt.core.model

import android.net.Uri

/** A playable track from [source] and what is known about it. */
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val albumArtist: String = "",
    val album: String,
    val path: String,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val trackNumber: Int,
    val discNumber: Int = 0,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val coverUri: Uri? = null,
    val source: SourceMode = SourceMode.LOCAL,
    val remoteId: String? = null,
    val clipStartMs: Long? = null,
    val clipEndMs: Long? = null,
    val cue: Boolean = false,
    val genre: String? = null,
    val bitDepth: Int? = null,
    val sampleRateHz: Int? = null,
)
