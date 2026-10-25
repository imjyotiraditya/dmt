package dev.jyotiraditya.dmt.core.common

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.util.allFilesAccess
import dev.jyotiraditya.dmt.util.allFilesAccessIntent

/** Which permissions are granted, and how to grant or revoke each one. */
@Stable
class PermissionGrants
internal constructor(
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
        isGranted(permission) -> R.string.permissions_manage
        permission in denied -> R.string.permissions_open_settings
        else -> R.string.permissions_allow
    }

    /** Asks for [permission], or opens app settings once granted or refused. */
    fun act(permission: String) {
        if (isGranted(permission) || permission in denied) {
            onSettings()
        } else {
            onRequest(permission)
        }
    }

    fun openFilesAccess() = onFilesAccess()
}

/** The state of [permissions] and all-files access, read again each time the app resumes. */
@Composable
fun rememberPermissionGrants(permissions: List<String>): PermissionGrants {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    val granted =
        remember(lifecycle, permissions) {
            permissions.associateWith {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    val filesAccess = remember(lifecycle) { allFilesAccess }

    // Asked and refused, so the next tap opens system settings instead.
    var denied by remember { mutableStateOf(emptySet<String>()) }
    var requested by remember { mutableStateOf<String?>(null) }
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            requested?.let { if (!isGranted) denied = denied + it }
            requested = null
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
