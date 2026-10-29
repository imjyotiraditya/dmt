package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.ServerSource
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.library.tracksSummary
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberWantsPlayback
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.rowFill

/** Picks where music comes from: this device or one of the added servers. */
@Composable
fun SourcesPage(onOpen: (Destination) -> Unit, viewModel: SourcesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val library = tracksSummary(state.tracks)
    val localChosen = settings.server == null
    val playing = rememberWantsPlayback()
    Column(
        modifier =
            Modifier.verticalScroll(rememberScrollState())
                .padding(bottom = LocalContentBottom.current),
    ) {
        PageHead(
            title = stringResource(R.string.sources_title),
            modifier = Modifier.padding(horizontal = Margin),
        )
        Text(
            text = stringResource(R.string.sources_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(horizontal = Margin).padding(bottom = 20.dp),
        )
        SourceSleeve(
            icon = SourceMode.LOCAL.icon,
            label = stringResource(R.string.source_kind_local),
            detail =
                listOfNotNull(
                    stringResource(R.string.setup_source_local_detail),
                    library.takeIf { localChosen },
                )
                    .joinToString(" · "),
            chosen = localChosen,
            onClick = { viewModel.use(null) },
            fill = 0.rowFill,
            spinning = localChosen && playing,
            actions =
                if (localChosen && !state.hasPermission) {
                    { ink ->
                        TextLink(
                            label = stringResource(R.string.permissions_allow_music),
                            onClick = { onOpen(Destination.Permissions) },
                            color = ink,
                        )
                    }
                } else {
                    null
                },
        )
        settings.sources.forEachIndexed { index, source ->
            ServerSleeve(
                source = source,
                position = index + 1,
                chosen = settings.server?.id == source.id,
                library = library,
                playing = playing,
                onUse = { viewModel.use(source.id) },
                onEdit = { onOpen(Destination.SourceForm(source.kind, source.id)) },
                onSignOut = { viewModel.signOut(source.id) },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
                Modifier.fillMaxWidth()
                    .pressable(
                        onClick = { onOpen(Destination.SourceKinds) },
                        label = stringResource(R.string.sources_add),
                    )
                    .padding(horizontal = Margin, vertical = 18.dp),
        ) {
            DmtIcon(icon = DmtIcons.Plus, size = 20.dp)
            Text(
                text = stringResource(R.string.sources_add),
                style = MaterialTheme.typography.labelLarge,
                color = Ink,
            )
        }
    }
}

/** A server added as a source: picks it when signed in, otherwise opens its sign-in. */
@Composable
private fun ServerSleeve(
    source: ServerSource,
    position: Int,
    chosen: Boolean,
    library: String,
    playing: Boolean,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onSignOut: () -> Unit,
) {
    SourceSleeve(
        icon = source.kind.icon,
        label = source.name,
        detail =
            listOfNotNull(
                stringResource(source.kind.title),
                if (source.signedIn) {
                    source.username
                } else {
                    stringResource(R.string.sources_signed_out)
                },
                source.url.toUri().host.takeIf { source.signedIn },
                library.takeIf { chosen },
            )
                .joinToString(" · "),
        chosen = chosen,
        onClick = if (source.signedIn) onUse else onEdit,
        fill = position.rowFill,
        muted = !source.signedIn,
        spinning = chosen && playing,
        actions =
            if (chosen) {
                { ink ->
                    TextLink(
                        label = stringResource(R.string.sources_edit),
                        onClick = onEdit,
                        color = ink,
                    )
                    TextLink(
                        label = stringResource(R.string.sources_disconnect),
                        onClick = onSignOut,
                        color = ink,
                    )
                }
            } else {
                null
            },
    )
}

/** The kinds of server a source can be added for. */
internal val serverKinds: List<SourceMode>
    get() = SourceMode.entries.filter { it != SourceMode.LOCAL }
