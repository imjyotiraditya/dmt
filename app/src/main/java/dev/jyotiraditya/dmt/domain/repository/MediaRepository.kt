package dev.jyotiraditya.dmt.domain.repository

import dev.jyotiraditya.dmt.domain.model.Track

interface MediaRepository {
    fun scan(): List<Track>
}
