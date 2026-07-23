package dev.jyotiraditya.dmt.util

import android.Manifest
import android.os.Build

val audioPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

val notificationPermission: String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

val localNetworkPermission: String? =
    if (Build.VERSION.SDK_INT >= 37) {
        Manifest.permission.ACCESS_LOCAL_NETWORK
    } else {
        null
    }
