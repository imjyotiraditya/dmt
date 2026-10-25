package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickCount
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.asString
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.components.Artwork
import dev.jyotiraditya.dmt.ui.components.FillBar
import dev.jyotiraditya.dmt.ui.components.IconTap
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.motion.FADE_MS
import dev.jyotiraditya.dmt.ui.theme.Danger
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface2
import dev.jyotiraditya.dmt.ui.theme.inkOn

/** The mini player's height, without the navigation bar. */
val MiniPlayerHeight = 88.dp

private val ProgressHeight = 3.dp
private val CoverSize = 68.dp
private val CoverInset = 12.dp

internal object MiniPlayerCover {
    /** Where the mini player's cover sits, from the top left of the surface. */
    fun bounds(density: Density): Rect = with(density) {
        val left = CoverInset.toPx()
        val top = (ProgressHeight + (MiniPlayerHeight - ProgressHeight - CoverSize) / 2).toPx()
        Rect(left, top, left + CoverSize.toPx(), top + CoverSize.toPx())
    }
}

/** The collapsed player: progress along the top, then cover, title, play/pause and next. */
@OptIn(UnstableApi::class)
@Composable
internal fun MiniPlayer(
    state: DmtState,
    player: Player,
    field: Color,
    coverHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    var width by remember { mutableIntStateOf(0) }
    val progress = rememberProgressStateWithTickCount(player, totalTickCount = width)
    val next = rememberNextButtonState(player)
    val progressColor = if (field.inkOn() == Ink) LocalAccent.current.highlight else field
    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        FillBar(
            fraction = { progress.currentPositionProgress },
            color = progressColor,
            track = Surface2,
            height = ProgressHeight,
            modifier = Modifier.onSizeChanged { width = it.width },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.height(MiniPlayerHeight - ProgressHeight)
                    .padding(start = CoverInset, end = 4.dp),
        ) {
            Artwork(
                cover = state.cover,
                modifier =
                    Modifier.size(CoverSize).graphicsLayer { alpha = if (coverHidden) 0f else 1f },
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                AnimatedContent(
                    targetState = state.title,
                    transitionSpec = {
                        fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS / 2))
                    },
                    label = "miniTitle",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = state.fault?.asString() ?: state.artist.asCredit(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.fault != null) Danger else Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlayPauseTap(player = player, ink = Ink, size = 56.dp, iconSize = 30.dp)
            IconTap(
                icon = DmtIcons.Next,
                description = stringResource(R.string.player_next),
                onClick = next::onClick,
                enabled = next.isEnabled,
                size = 56.dp,
                iconSize = 28.dp,
            )
        }
    }
}
