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
    buildList {
        add(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

val localNetworkPermission: String? =
    if (Build.VERSION.SDK_INT >= 37) {
        Manifest.permission.ACCESS_LOCAL_NETWORK
    } else {
        null
    }
