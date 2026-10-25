package dev.jyotiraditya.dmt.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Numerals
import dev.jyotiraditya.dmt.ui.theme.Surface
import dev.jyotiraditya.dmt.ui.theme.Surface2

private val ROW_ART = 56.dp

/** The stripe that marks the current track, drawn in the row's leading margin. */
private val CURRENT_STRIPE = 4.dp

private val INLINE_BARS = 12.dp

/** A track with its cover or number, marking the current one with a stripe, accent and bars. */
@Composable
fun TrackRow(
    track: Track,
    meta: String,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val mark = LocalAccent.current.highlight
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    if (current) drawRect(mark, size = Size(CURRENT_STRIPE.toPx(), size.height))
                }
                .pressable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    sink = 0.985f,
                    label = track.title,
                )
                .padding(horizontal = Margin, vertical = 8.dp),
    ) {
        if (number != null) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.size(width = 36.dp, height = 44.dp),
            ) {
                if (current) {
                    PlayingBars(playing = playing, color = mark)
                } else {
                    Text(text = "$number", style = Numerals, color = Muted)
                }
            }
        } else {
            Artwork(track = track, modifier = Modifier.size(ROW_ART))
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (current) mark else Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val barsInline = current && number == null
            if (meta.isNotEmpty() || barsInline || track.cue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (barsInline) {
                        PlayingBars(
                            playing = playing,
                            color = mark,
                            modifier = Modifier.padding(end = 6.dp).size(INLINE_BARS),
                        )
                    }
                    if (track.cue) {
                        Tag(
                            text = stringResource(R.string.library_cue_tag),
                            color = Muted,
                            field = Surface2,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

/** An artist, genre, folder or playlist: its name and one detail line. */
@Composable
fun CollectionRow(
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .pressable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    sink = 0.985f,
                    label = title,
                )
                .padding(horizontal = Margin, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
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
}

/** Three bars that bounce while [playing] and rest otherwise. */
@Composable
fun PlayingBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = Ink) {
    if (playing) {
        BouncingBars(modifier, color)
    } else {
        Canvas(modifier = modifier.size(18.dp)) { drawBars(color, 0.45f, 0.8f, 0.6f) }
    }
}

/** The bars in motion, read while drawing. */
@Composable
private fun BouncingBars(modifier: Modifier, color: Color) {
    val transition = rememberInfiniteTransition(label = "bars")
    val a =
        transition.animateFloat(
            0.3f,
            1f,
            infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "barA",
        )
    val b =
        transition.animateFloat(
            1f,
            0.25f,
            infiniteRepeatable(tween(410), RepeatMode.Reverse),
            label = "barB",
        )
    val c =
        transition.animateFloat(
            0.45f,
            0.9f,
            infiniteRepeatable(tween(610), RepeatMode.Reverse),
            label = "barC",
        )
    Canvas(modifier = modifier.size(18.dp)) { drawBars(color, a.value, b.value, c.value) }
}

/** Three bars standing on the bottom edge, each [first], [second] and [third] of the height. */
private fun DrawScope.drawBars(color: Color, first: Float, second: Float, third: Float) {
    val barW = size.width / 5f
    drawBar(color, 0f, barW, first)
    drawBar(color, barW * 2, barW, second)
    drawBar(color, barW * 4, barW, third)
}

private fun DrawScope.drawBar(color: Color, left: Float, width: Float, height: Float) {
    val barH = size.height * height
    drawRect(color, topLeft = Offset(left, size.height - barH), size = Size(width, barH))
}

@Composable
fun SearchBox(
    query: String,
    hint: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Surface)
                .padding(start = 16.dp, end = 4.dp),
    ) {
        DmtIcon(icon = DmtIcons.Search, tint = Ink, size = 22.dp)
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            cursorBrush = SolidColor(Ink),
            modifier =
                Modifier.weight(1f)
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(text = hint, style = MaterialTheme.typography.bodyLarge, color = Muted)
                }
                inner()
            },
        )
        if (query.isNotEmpty()) {
            IconTap(
                icon = DmtIcons.Close,
                description = stringResource(R.string.search_clear),
                onClick = { onQuery("") },
                iconSize = 20.dp,
            )
        }
    }
}
