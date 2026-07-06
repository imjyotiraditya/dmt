package dev.jyotiraditya.dmt.util

import android.Manifest
import android.os.Build

val audioPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

val runtimePermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(audioPermission, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(audioPermission)
    }
