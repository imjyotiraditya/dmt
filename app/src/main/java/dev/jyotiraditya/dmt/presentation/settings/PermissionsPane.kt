package dev.jyotiraditya.dmt.presentation.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.util.audioPermission
import dev.jyotiraditya.dmt.util.localNetworkPermission
import dev.jyotiraditya.dmt.util.notificationPermission

private data class PermissionEntry(
    val permission: String,
    @StringRes val label: Int,
    @StringRes val why: Int,
    @StringRes val whenOff: Int,
)

private val PERMISSION_REGISTRY: List<PermissionEntry> =
    buildList {
        add(
            PermissionEntry(
                permission = audioPermission,
                label = R.string.perm_audio_label,
                why = R.string.perm_audio_why,
                whenOff = R.string.perm_audio_off,
            ),
        )
        notificationPermission?.let {
            add(
                PermissionEntry(
                    permission = it,
                    label = R.string.perm_notif_label,
                    why = R.string.perm_notif_why,
                    whenOff = R.string.perm_notif_off,
                ),
            )
        }
        localNetworkPermission?.let {
            add(
                PermissionEntry(
                    permission = it,
                    label = R.string.perm_net_label,
                    why = R.string.perm_net_why,
                    whenOff = R.string.perm_net_off,
                ),
            )
        }
    }

@Composable
fun PermissionsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
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
        PERMISSION_REGISTRY.associate { entry ->
            entry.permission to
                    (ContextCompat.checkSelfPermission(context, entry.permission) ==
                            PackageManager.PERMISSION_GRANTED)
        }
    }

    LaunchedEffect(granted) {
        val audioGranted = granted[audioPermission] == true
        if (audioGranted != state.hasPermission) {
            dispatch(DmtAction.Permission(audioGranted))
        }
    }

    var denied by remember { mutableStateOf(emptySet<String>()) }
    var requested by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        requested?.let { if (!isGranted) denied = denied + it }
        requested = null
        refresh++
    }

    val openAppSettings = {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        }
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Caption(stringResource(R.string.perms_title))

        PERMISSION_REGISTRY.forEach { entry ->
            val isGranted = granted[entry.permission] == true
            PermissionRow(
                label = stringResource(entry.label),
                why = stringResource(entry.why),
                whenOff = stringResource(entry.whenOff),
                granted = isGranted,
                actionLabel = stringResource(
                    when {
                        isGranted -> R.string.perm_revoke
                        entry.permission in denied -> R.string.perm_settings
                        else -> R.string.grant
                    },
                ),
                onAction = {
                    if (isGranted || entry.permission in denied) {
                        openAppSettings()
                    } else {
                        requested = entry.permission
                        launcher.launch(entry.permission)
                    }
                },
            )
        }

        Text(
            text = stringResource(R.string.perms_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    why: String,
    whenOff: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = if (granted) "[x] " else "[ ] ",
                style = MaterialTheme.typography.bodyLarge,
                color = if (granted) TuiAccent else TuiFaint,
                modifier = Modifier.align(Alignment.Top),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TuiFg,
                )
                Text(
                    text = why,
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiDim,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = whenOff,
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TuiKey(
                label = actionLabel,
                onClick = onAction,
            )
        }
        HorizontalDivider(color = TuiLine)
    }
}
