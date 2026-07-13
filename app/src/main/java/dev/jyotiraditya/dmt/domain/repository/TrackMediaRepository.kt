package dev.jyotiraditya.dmt.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackMediaRepository {
    fun loadArt(uri: Uri): Bitmap?
    fun techSpecs(uri: Uri, track: Track?): List<Spec>
    fun routeSpecs(): Flow<List<Spec>>
}
