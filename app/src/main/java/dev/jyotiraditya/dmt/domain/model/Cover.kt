package dev.jyotiraditya.dmt.domain.model

import android.graphics.Bitmap

/** A track's cover on its way to the screen. */
sealed interface Cover {
    data object Loading : Cover

    /** The track has no cover. */
    data object Missing : Cover

    data class Ready(val bitmap: Bitmap) : Cover
}

val Cover.bitmap: Bitmap?
    get() = (this as? Cover.Ready)?.bitmap
