package dev.jyotiraditya.dmt.core.common

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.util.allFilesAccess
import dev.jyotiraditya.dmt.util.allFilesAccessIntent

@Stable
class PermissionGrants internal constructor(
    private val granted: Map<String, Boolean>,
    private val denied: Set<String>,
    val filesAccess: Boolean,
    private val onRequest: (String) -> Unit,
    private val onSettings: () -> Unit,
    private val onFilesAccess: () -> Unit,
) {
    fun isGranted(permission: String): Boolean = granted[permission] == true

    @StringRes
    fun actionLabel(permission: String): Int = when {
        isGranted(permission) -> R.string.perm_revoke
        permission in denied -> R.string.perm_settings
        else -> R.string.grant
    }

    fun act(permission: String) {
        if (isGranted(permission) || permission in denied) {
            onSettings()
        } else {
            onRequest(permission)
        }
    }

    fun openFilesAccess() = onFilesAccess()
}

@Composable
fun rememberPermissionGrants(permissions: List<String>): PermissionGrants {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val granted = remember(refresh) {
        permissions.associateWith {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    val filesAccess = remember(refresh) { allFilesAccess }

    var denied by remember { mutableStateOf(emptySet<String>()) }
    var requested by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        requested?.let { if (!isGranted) denied = denied + it }
        requested = null
        refresh++
    }

    return remember(granted, denied, filesAccess) {
        PermissionGrants(
            granted = granted,
            denied = denied,
            filesAccess = filesAccess,
            onRequest = { permission ->
                requested = permission
                launcher.launch(permission)
            },
            onSettings = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            },
            onFilesAccess = {
                runCatching {
                    context.startActivity(allFilesAccessIntent(context.packageName))
                }
            },
        )
    }
}
