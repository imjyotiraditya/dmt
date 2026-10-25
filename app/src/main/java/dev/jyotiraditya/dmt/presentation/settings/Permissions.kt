package dev.jyotiraditya.dmt.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.PermissionGrants
import dev.jyotiraditya.dmt.core.common.rememberPermissionGrants
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface
import dev.jyotiraditya.dmt.ui.theme.rowFill
import dev.jyotiraditya.dmt.util.audioPermission
import dev.jyotiraditya.dmt.util.localNetworkPermission
import dev.jyotiraditya.dmt.util.notificationPermission

/**
 * A permission the app asks for, with what it is for and what happens without it; a null
 * [permission] is the all-files access setting.
 */
data class PermissionEntry(
    val permission: String?,
    @param:StringRes val label: Int,
    @param:StringRes val why: Int,
    @param:StringRes val whenOff: Int,
)

/** The runtime permissions the app asks for on this Android version. */
val PERMISSION_REGISTRY: List<PermissionEntry> = buildList {
    add(
        PermissionEntry(
            audioPermission,
            R.string.permission_audio_title,
            R.string.permission_audio_reason,
            R.string.permission_audio_denied,
        ),
    )
    notificationPermission?.let {
        add(
            PermissionEntry(
                it,
                R.string.permission_notifications_title,
                R.string.permission_notifications_reason,
                R.string.permission_notifications_denied,
            ),
        )
    }
    localNetworkPermission?.let {
        add(
            PermissionEntry(
                it,
                R.string.permission_network_title,
                R.string.permission_network_reason,
                R.string.permission_network_denied,
            ),
        )
    }
}

/** The all-files access setting, granted from system settings. */
val FILES_ENTRY =
    PermissionEntry(
        permission = null,
        label = R.string.permission_files_title,
        why = R.string.permission_files_reason,
        whenOff = R.string.permission_files_denied,
    )

/** Every permission with its purpose and a way to grant or revoke it. */
@Composable
fun PermissionsPage(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val grants = rememberPermissionGrants(PERMISSION_REGISTRY.mapNotNull { it.permission })
    SyncPermissionState(grants, state, dispatch)
    Column(
        modifier =
            Modifier.verticalScroll(rememberScrollState())
                .padding(bottom = LocalContentBottom.current),
    ) {
        PageHead(
            title = stringResource(R.string.permissions_title),
            modifier = Modifier.padding(horizontal = Margin),
        )
        (PERMISSION_REGISTRY + FILES_ENTRY).forEachIndexed { index, entry ->
            PermissionRow(entry, grants, fill = index.rowFill)
        }
        Text(
            text = stringResource(R.string.permissions_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.padding(horizontal = Margin, vertical = 20.dp),
        )
    }
}

/** Tells the view model when audio access or all-files access changes. */
@Composable
fun SyncPermissionState(grants: PermissionGrants, state: DmtState, dispatch: (DmtAction) -> Unit) {
    val audioGranted = grants.isGranted(audioPermission)
    LaunchedEffect(audioGranted) {
        if (audioGranted != state.hasPermission) dispatch(DmtAction.Permission(audioGranted))
    }
    var hadFilesAccess by remember { mutableStateOf(grants.filesAccess) }
    LaunchedEffect(grants.filesAccess) {
        if (grants.filesAccess && !hadFilesAccess) dispatch(DmtAction.Rescan)
        hadFilesAccess = grants.filesAccess
    }
}

@Composable
fun PermissionRow(
    entry: PermissionEntry,
    grants: PermissionGrants,
    modifier: Modifier = Modifier,
    fill: Color = Surface,
    showWhenOff: Boolean = true,
) {
    val permission = entry.permission
    val granted = if (permission == null) grants.filesAccess else grants.isGranted(permission)
    val action =
        stringResource(
            when {
                permission != null -> grants.actionLabel(permission)
                granted -> R.string.permissions_manage
                else -> R.string.permissions_allow
            },
        )
    val act = { if (permission == null) grants.openFilesAccess() else grants.act(permission) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(fill)
                .animateContentSize()
                .padding(horizontal = Margin, vertical = 18.dp),
    ) {
        Text(
            text = stringResource(entry.label),
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
        )
        Text(
            text = stringResource(entry.why),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (showWhenOff && !granted) {
            Text(
                text = stringResource(entry.whenOff),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (granted) {
            TextLink(label = action, onClick = act)
        } else {
            Block(label = action, onClick = act, modifier = Modifier.padding(top = 14.dp))
        }
    }
}
