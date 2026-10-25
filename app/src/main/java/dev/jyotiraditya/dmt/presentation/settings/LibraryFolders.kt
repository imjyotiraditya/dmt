package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.Toggle
import dev.jyotiraditya.dmt.ui.components.pressableToggle
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted

/** A folder that holds music, and whether its tracks appear in the library. */
private data class MusicFolder(
    val path: String,
    val name: String,
    val location: String,
    val trackCount: Int?,
    val shown: Boolean,
)

/** Every folder with music and every hidden folder, sorted by name. */
private fun musicFolders(
    visible: List<Folder>,
    hidden: Set<String>,
    counts: Map<String, Int>,
    storageName: String,
): List<MusicFolder> = (visible.map { it.path } + hidden)
    .distinct()
    .map { path ->
        val folder = Folder(path = path, tracks = emptyList())
        MusicFolder(
            path = path,
            name = folder.name.ifEmpty { storageName },
            location = folder.location,
            trackCount = counts[path],
            shown = path !in hidden,
        )
    }
    .sortedBy { it.name.lowercase() }

/** Chooses which folders' tracks appear in the library. */
@Composable
internal fun LibraryFoldersPage(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val hidden = state.settings.hiddenFolders
    // Counts as the page opened, so a folder hidden since keeps its count.
    val counts = remember { state.folders.associate { it.path to it.tracks.size } }
    val folders =
        musicFolders(state.folders, hidden, counts, stringResource(R.string.folders_storage_root))
    val setShown = { folder: MusicFolder, shown: Boolean ->
        val updated = if (shown) hidden - folder.path else hidden + folder.path
        dispatch(DmtAction.Config(state.settings.copy(hiddenFolders = updated)))
    }

    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item {
            PageHead(
                title = stringResource(R.string.folders_title),
                modifier = Modifier.padding(horizontal = Margin),
            )
            Text(
                text = stringResource(R.string.folders_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
                modifier = Modifier.padding(start = Margin, end = Margin, bottom = 12.dp),
            )
        }
        items(folders, key = { it.path }) { folder ->
            FolderRow(
                folder,
                onShownChange = {
                    setShown(folder, it)
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun FolderRow(
    folder: MusicFolder,
    onShownChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail =
        listOfNotNull(
            folder.location.takeIf { it.isNotEmpty() },
            if (folder.shown) {
                folder.trackCount?.let { pluralStringResource(R.plurals.count_tracks, it, it) }
            } else {
                stringResource(R.string.folders_hidden)
            },
        )
            .joinToString(" · ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .pressableToggle(value = folder.shown, onValueChange = onShownChange, sink = 0.985f)
                .padding(horizontal = Margin, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (folder.shown) Ink else Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Toggle(checked = folder.shown)
    }
}
