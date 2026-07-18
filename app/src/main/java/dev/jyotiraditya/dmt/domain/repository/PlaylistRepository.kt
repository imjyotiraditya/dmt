package dev.jyotiraditya.dmt.domain.repository

import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.Track

interface PlaylistRepository {
    fun load(tracks: List<Track>): List<Playlist>
    fun create(name: String): Boolean
    fun delete(name: String)
    fun addTrack(name: String, track: Track): Boolean
    fun removeTrack(name: String, path: String)
}