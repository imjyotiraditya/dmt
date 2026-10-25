package dev.jyotiraditya.dmt.library

import androidx.media3.common.C

const val UNKNOWN_TITLE = "unknown title"
const val UNKNOWN_ARTIST = "unknown artist"
const val UNKNOWN_ALBUM = "unknown album"

fun Long.takeIfSet(): Long? = takeIf { it != C.TIME_UNSET }
