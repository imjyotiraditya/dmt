package dev.jyotiraditya.dmt.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Muted

private val CaptionGap = 10.dp

/** A square cover with a title and subtitle beneath; [coverModifier] applies to the cover only. */
@Composable
fun CoverTile(
    cover: Track?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleLines: Int = 1,
    onLongClick: (() -> Unit)? = null,
    coverModifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.pressable(
                onClick = onClick,
                onLongClick = onLongClick,
                label = title,
            ),
    ) {
        Artwork(
            track = cover,
            modifier = coverModifier.fillMaxWidth().aspectRatio(1f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Ink,
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = CaptionGap),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
