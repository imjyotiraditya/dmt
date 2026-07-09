package dev.jyotiraditya.dmt.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.AudioJourney
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track

interface TrackMediaRepository {
    fun loadArt(uri: Uri): Bitmap?
    fun techSpecs(uri: Uri, track: Track?): List<Spec>

    /** Builds the Track -> Decoder -> Processing -> Output -> Output Device signal flow. */
    fun audioJourney(uri: Uri, track: Track?, speed: Float, audioSessionId: Int): AudioJourney
}
