package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.nowplaying.details.DetailsPage
import dev.jyotiraditya.dmt.presentation.nowplaying.lyrics.LyricsPage
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.adaptive.SystemBarsOver
import dev.jyotiraditya.dmt.ui.adaptive.windowShape
import dev.jyotiraditya.dmt.ui.components.IconTap
import dev.jyotiraditya.dmt.ui.components.WordTabs
import dev.jyotiraditya.dmt.ui.components.animateFieldAsState
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.motion.PAGE_MS
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Night
import dev.jyotiraditya.dmt.ui.theme.Surface
import dev.jyotiraditya.dmt.ui.theme.inkOn
import dev.jyotiraditya.dmt.ui.theme.resting
import kotlinx.coroutines.launch

internal enum class PlayerPage(@param:StringRes val title: Int) {
    PLAYING(R.string.player_tab_playing),
    LYRICS(R.string.player_lyrics),
    QUEUE(R.string.queue_title),
    DETAILS(R.string.player_tab_details),
    ;

    /** The page's background; the playing page takes the colour of the cover. */
    fun ground(cover: Color): Color = when (this) {
        PLAYING -> cover
        LYRICS -> Night
        QUEUE -> Ground
        DETAILS -> Surface
    }
}

/** The expanded player; in a wide window the cover stays on the left beside the pages. */
@Composable
internal fun NowPlayingScreen(
    state: DmtState,
    player: Player,
    field: Color,
    dispatch: (DmtAction) -> Unit,
    pager: PagerState,
    ownsSystemBars: Boolean,
    coverHidden: Boolean,
    onCoverPlaced: (LayoutCoordinates) -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: () -> Unit,
) {
    // The background follows the swipe; the ink follows the target page.
    val targetGround = PlayerPage.entries[pager.targetPage].ground(field)
    val ink by animateColorAsState(targetGround.inkOn(), tween(PAGE_MS), label = "playerInk")
    val restingInk by
        animateColorAsState(
            targetValue = targetGround.inkOn().resting(targetGround),
            animationSpec = tween(PAGE_MS),
            label = "playerRestingInk",
        )
    if (ownsSystemBars) SystemBarsOver(targetGround)

    val wide = windowShape().wide
    val steps = rememberSeekSteps()
    val paint by animateFieldAsState(field)
    Box(
        modifier =
            Modifier.fillMaxSize()
                .drawBehind { drawRect(pager.groundAt(paint)) }
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(0.46f).fillMaxHeight()) {
                    CollapseButton(ink, onClick = onCollapse)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxWidth()
                                .padding(start = Margin, end = 8.dp, bottom = Margin),
                    ) {
                        PlayerCover(state, player, steps, coverHidden, onPlaced = onCoverPlaced)
                    }
                }
                Column(modifier = Modifier.weight(0.54f).fillMaxHeight()) {
                    PageTabs(pager, ink, restingInk, modifier = Modifier.padding(top = 4.dp))
                    Pages(
                        state,
                        player,
                        steps,
                        field,
                        dispatch,
                        pager,
                        wide = true,
                        coverHidden,
                        onCoverPlaced,
                        onOpenArtist,
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    CollapseButton(ink, onClick = onCollapse)
                    PageTabs(pager, ink, restingInk, modifier = Modifier.weight(1f), inset = 6.dp)
                }
                Pages(
                    state,
                    player,
                    steps,
                    field,
                    dispatch,
                    pager,
                    wide = false,
                    coverHidden,
                    onCoverPlaced,
                    onOpenArtist,
                )
            }
        }
    }
}

@Composable
private fun CollapseButton(ink: Color, onClick: () -> Unit) {
    IconTap(
        icon = DmtIcons.ChevronDown,
        description = stringResource(R.string.player_close),
        onClick = onClick,
        ink = ink,
        modifier = Modifier.padding(start = 6.dp),
    )
}

@Composable
private fun PageTabs(
    pager: PagerState,
    ink: Color,
    rest: Color,
    modifier: Modifier = Modifier,
    inset: Dp = Margin,
) {
    val scope = rememberCoroutineScope()
    WordTabs(
        labels = PlayerPage.entries.map { stringResource(it.title) },
        selected = pager.currentPage,
        onSelect = { scope.launch { pager.animateScrollToPage(it) } },
        ink = ink,
        rest = rest,
        inset = inset,
        modifier = modifier,
    )
}

@Composable
private fun ColumnScope.Pages(
    state: DmtState,
    player: Player,
    steps: SeekSteps,
    field: Color,
    dispatch: (DmtAction) -> Unit,
    pager: PagerState,
    wide: Boolean,
    coverHidden: Boolean,
    onCoverPlaced: (LayoutCoordinates) -> Unit,
    onOpenArtist: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 1,
        modifier = Modifier.weight(1f).fillMaxWidth(),
    ) { index ->
        when (PlayerPage.entries[index]) {
            PlayerPage.PLAYING ->
                PlayingPage(
                    state = state,
                    player = player,
                    steps = steps,
                    field = field,
                    dispatch = dispatch,
                    showCover = !wide,
                    coverHidden = coverHidden,
                    onCoverPlaced = onCoverPlaced,
                    onOpenArtist = onOpenArtist,
                    onOpenQueue = {
                        scope.launch { pager.animateScrollToPage(PlayerPage.QUEUE.ordinal) }
                    },
                )

            PlayerPage.LYRICS -> LyricsPage(state, player, dispatch)

            PlayerPage.QUEUE -> QueuePage(state, player, dispatch)

            PlayerPage.DETAILS -> DetailsPage(state, player)
        }
    }
}

/** The background at the pager's scroll position, blended between neighbouring pages. */
private fun PagerState.groundAt(cover: Color): Color {
    val position = currentPage + currentPageOffsetFraction
    val from = PlayerPage.entries[position.toInt().coerceIn(0, PlayerPage.entries.lastIndex)]
    val to = PlayerPage.entries[(from.ordinal + 1).coerceAtMost(PlayerPage.entries.lastIndex)]
    return lerp(from.ground(cover), to.ground(cover), (position - from.ordinal).coerceIn(0f, 1f))
}
