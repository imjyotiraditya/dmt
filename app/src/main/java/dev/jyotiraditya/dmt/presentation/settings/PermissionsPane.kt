package dev.jyotiraditya.dmt.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.PermissionGrants
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.rememberPermissionGrants
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

private val STACK_WIDTH = 340.dp

data class PermissionEntry(
    val permission: String,
    @param:StringRes val label: Int,
    @param:StringRes val why: Int,
    @param:StringRes val whenOff: Int,
) {
    val isFilesAccess: Boolean get() = permission.isEmpty()
}

val PERMISSION_REGISTRY: List<PermissionEntry> =
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

val FILES_ENTRY = PermissionEntry(
    permission = "",
    label = R.string.perm_files_label,
    why = R.string.perm_files_why,
    whenOff = R.string.perm_files_off,
)

@Composable
fun PermissionsPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val grants = rememberPermissionGrants(PERMISSION_REGISTRY.map { it.permission })
    SyncPermissionState(grants, state, dispatch)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PERMISSION_REGISTRY.forEach { entry ->
            PermissionRow(
                entry = entry,
                granted = grants.isGranted(entry.permission),
                actionLabel = stringResource(grants.actionLabel(entry.permission)),
                onAction = { grants.act(entry.permission) },
            )
        }
        PermissionRow(
            entry = FILES_ENTRY,
            granted = grants.filesAccess,
            actionLabel = stringResource(
                if (grants.filesAccess) R.string.perm_revoke else R.string.grant,
            ),
            onAction = grants::openFilesAccess,
        )
        Text(
            text = stringResource(R.string.perms_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun SyncPermissionState(
    grants: PermissionGrants,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
) {
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
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    showWhenOff: Boolean = true,
) {
    BoxWithConstraints {
        val stacked = maxWidth < STACK_WIDTH

        Column(modifier = Modifier.fillMaxWidth()) {
            if (stacked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                ) {
                    PermissionText(entry, granted, showWhenOff)
                    TuiKey(
                        label = actionLabel,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp),
                        onClick = onAction,
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                    ) {
                        PermissionText(entry, granted, showWhenOff)
                    }
                    TuiKey(
                        label = actionLabel,
                        onClick = onAction,
                    )
                }
            }
            HorizontalDivider(color = TuiLine)
        }
    }
}

@Composable
private fun PermissionText(entry: PermissionEntry, granted: Boolean, showWhenOff: Boolean) {
    val mark = if (granted) "[x] " else "[ ] "
    val indent = markWidth(mark)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = if (granted) TuiAccent else TuiFaint)) { append(mark) }
            withStyle(SpanStyle(color = TuiFg)) { append(stringResource(entry.label)) }
        },
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = stringResource(entry.why),
        style = MaterialTheme.typography.labelSmall,
        color = TuiDim,
        modifier = Modifier.padding(start = indent, top = 2.dp),
    )
    if (showWhenOff) {
        Text(
            text = stringResource(entry.whenOff),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            modifier = Modifier.padding(start = indent, top = 2.dp),
        )
    }
}

@Composable
fun markWidth(mark: String): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyLarge
    val density = LocalDensity.current
    return remember(mark, style, density) {
        with(density) { measurer.measure(mark, style).size.width.toDp() }
    }
}
