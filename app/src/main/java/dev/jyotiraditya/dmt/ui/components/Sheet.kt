package dev.jyotiraditya.dmt.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.Dark
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import kotlinx.coroutines.launch

/** What a sheet's content can do with the sheet. */
@Stable
class DmtSheetScope internal constructor(private val close: () -> Unit) {
    /** Slides the sheet away, then reports it dismissed. */
    fun dismiss() = close()
}

/** A bottom sheet on the page background, with a short square drag handle in ink. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmtSheet(onDismiss: () -> Unit, content: @Composable DmtSheetScope.() -> Unit) {
    val state = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val dismissed by rememberUpdatedState(onDismiss)
    val sheet =
        remember(state) {
            DmtSheetScope {
                scope
                    .launch { state.hide() }
                    .invokeOnCompletion { if (!state.isVisible) dismissed() }
            }
        }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        scrimColor = Dark.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(width = 40.dp, height = 4.dp, color = Ink) },
    ) {
        Column {
            sheet.content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SheetHeader(title: String, modifier: Modifier = Modifier, meta: String? = null) {
    Column(modifier = modifier.padding(horizontal = Margin, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** One action in a sheet: an icon and a label, the whole row tappable. */
@Composable
fun SheetAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .pressable(onClick = onClick, enabled = enabled, sink = 0.985f, label = label)
                .padding(horizontal = Margin, vertical = 14.dp),
    ) {
        DmtIcon(icon = icon, tint = if (enabled) Ink else Muted)
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Ink else Muted,
            modifier = Modifier.padding(start = 18.dp),
        )
    }
}
