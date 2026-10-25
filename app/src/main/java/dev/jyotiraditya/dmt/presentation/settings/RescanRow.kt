package dev.jyotiraditya.dmt.presentation.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LibraryScan
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import kotlinx.coroutines.delay

/** How long the check stays after a scan finishes. */
private const val CHECK_HOLD_MS = 1_600L

/** Rescans the library on tap, with what the [last] scan found and when underneath. */
@Composable
internal fun RescanRow(
    scanning: Boolean,
    last: LibraryScan?,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.settings_rescan)
    val now by rememberMinuteClock()
    val hint =
        when {
            scanning -> AnnotatedString(stringResource(R.string.settings_rescan_scanning))
            last != null -> scanSummary(last, now)
            else -> null
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .pressable(onClick = onRescan, enabled = !scanning, sink = 0.985f, label = label)
                .padding(horizontal = Margin, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = Ink)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        RescanGlyph(scanning = scanning, done = rememberJustFinished(last))
    }
}

/** The current time, updated each minute. */
@Composable
private fun rememberMinuteClock() = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(DateUtils.MINUTE_IN_MILLIS)
        value = System.currentTimeMillis()
    }
}

/** True for a moment after a scan newer than the one shown when this was first composed. */
@Composable
private fun rememberJustFinished(last: LibraryScan?): Boolean {
    val first = remember { last }
    val finished by
        produceState(false, last) {
            if (last != null && last != first) {
                value = true
                delay(CHECK_HOLD_MS)
                value = false
            }
        }
    return finished
}

/** What [scan] found and when: within an hour of a rescan what changed, else the track count. */
@Composable
private fun scanSummary(scan: LibraryScan, nowMs: Long): AnnotatedString {
    val age = nowMs - scan.atMs
    val time =
        if (age < DateUtils.MINUTE_IN_MILLIS) {
            stringResource(R.string.settings_rescan_just_now)
        } else {
            DateUtils.getRelativeTimeSpanString(
                scan.atMs,
                nowMs,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            )
                .toString()
        }
    val added = scan.added ?: 0
    val removed = scan.removed ?: 0
    val changes = buildList {
        if (added > 0) add(pluralStringResource(R.plurals.settings_rescan_added, added, added))
        if (removed > 0) {
            add(pluralStringResource(R.plurals.settings_rescan_removed, removed, removed))
        }
    }
    val recent = scan.added != null && age < DateUtils.HOUR_IN_MILLIS
    val lead =
        when {
            recent && changes.isNotEmpty() -> changes
            recent -> listOf(stringResource(R.string.settings_rescan_up_to_date))
            else -> listOf(pluralStringResource(R.plurals.count_tracks, scan.tracks, scan.tracks))
        }
    val bright = recent && changes.isNotEmpty()
    return buildAnnotatedString {
        lead.forEach { part ->
            if (bright) withStyle(SpanStyle(color = Ink)) { append(part) } else append(part)
            append(" · ")
        }
        append(time)
    }
}
