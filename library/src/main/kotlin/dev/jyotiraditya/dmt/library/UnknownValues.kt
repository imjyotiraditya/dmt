package dev.jyotiraditya.dmt.library

import androidx.media3.common.C

fun Long.takeIfSet(): Long? = takeIf { it != C.TIME_UNSET }
