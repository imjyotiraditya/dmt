package dev.jyotiraditya.dmt.util

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri

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

val allFilesAccess: Boolean
    get() = Environment.isExternalStorageManager()

fun allFilesAccessIntent(packageName: String): Intent =
    Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        "package:$packageName".toUri(),
    )
