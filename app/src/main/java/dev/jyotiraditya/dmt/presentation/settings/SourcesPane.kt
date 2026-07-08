package dev.jyotiraditya.dmt.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.util.audioPermission
import dev.jyotiraditya.dmt.util.runtimePermissions

private data class SourceDescriptor(
    val mode: SourceMode,
    val label: String,
    val subtitle: (DmtSettings) -> String,
    val requiresAuth: Boolean,
    val connected: (DmtSettings) -> Boolean = { true },
    val logout: (DmtSettings) -> DmtSettings = { it },
)

private val SOURCE_REGISTRY = listOf(
    SourceDescriptor(
        mode = SourceMode.LOCAL,
        label = SourceMode.LOCAL.label,
        subtitle = { "files on this device" },
        requiresAuth = false,
    ),
    SourceDescriptor(
        mode = SourceMode.JELLYFIN,
        label = SourceMode.JELLYFIN.label,
        subtitle = { it.jellyfinUrl ?: "not connected" },
        requiresAuth = true,
        connected = { !it.jellyfinToken.isNullOrBlank() },
        logout = {
            it.copy(
                sourceMode = SourceMode.LOCAL,
                jellyfinUrl = null,
                jellyfinUserId = null,
                jellyfinToken = null,
            )
        },
    ),
)

@Composable
fun SourcesPane(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val settings = state.settings

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        dispatch(DmtAction.Permission(grants[audioPermission] == true))
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Caption(stringResource(R.string.sources_title))

        SOURCE_REGISTRY.forEach { source ->
            val connected = source.connected(settings)
            val needsGrant = source.mode == SourceMode.LOCAL && !state.hasPermission
            SourceRow(
                label = source.label,
                subtitle = source.subtitle(settings),
                active = settings.sourceMode == source.mode,
                needsLogin = source.requiresAuth && !connected,
                onGrant = if (needsGrant) {
                    { permissionLauncher.launch(runtimePermissions) }
                } else {
                    null
                },
                onSelect = {
                    if (source.requiresAuth && !connected) {
                        dispatch(DmtAction.ShowLogin(source.mode))
                    } else {
                        dispatch(DmtAction.Config(settings.copy(sourceMode = source.mode)))
                    }
                },
                onLogout = if (source.requiresAuth && connected) {
                    { dispatch(DmtAction.Config(source.logout(settings))) }
                } else {
                    null
                },
            )
        }

        Text(
            text = stringResource(R.string.sources_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun SourceRow(
    label: String,
    subtitle: String,
    active: Boolean,
    needsLogin: Boolean,
    onSelect: () -> Unit,
    onLogout: (() -> Unit)?,
    onGrant: (() -> Unit)?,
) {
    val accent = LocalAccent.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tuiClickable(onSelect),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = if (active) "(*) " else "( ) ",
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) accent else TuiFaint,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (active) TuiBright else TuiFg,
                )
                Text(
                    text = "└ $subtitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            onGrant?.let {
                TuiKey(
                    label = stringResource(R.string.grant),
                    onClick = it,
                )
            }
            if (needsLogin) {
                TuiKey(
                    label = "[ ${stringResource(R.string.source_login)} ]",
                    onClick = onSelect,
                )
            }
            onLogout?.let {
                TuiKey(
                    label = "[ ${stringResource(R.string.source_logout)} ]",
                    onClick = it,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TuiLine),
        )
    }
}
