package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberIsPlaying
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberPlaybackPosition
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.components.EmptyState
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.WordToggle
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Night
import dev.jyotiraditya.dmt.ui.theme.NightInk

/** How far ahead of the reported position words are lit, to cover display latency. */
private const val LEAD_MS = 80L

/** The lyrics with floating playback controls, and below them the source and romanization. */
@OptIn(UnstableApi::class)
@Composable
internal fun LyricsPage(state: DmtState, player: Player, dispatch: (DmtAction) -> Unit) {
    val lyrics = state.lyrics
    val bySinger = state.settings.singerColors
    val colors = remember(bySinger) { LyricColors.on(ink = NightInk, background = Night, bySinger) }
    val controls = rememberLyricsControlsState()
    val playPause = rememberPlayPauseButtonState(player)
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = Margin)) {
        Box(modifier = Modifier.weight(1f).revealsControls(controls)) {
            if (lyrics != null) {
                LyricsView(
                    lyrics = lyrics,
                    trackId = state.nowPlayingId,
                    position =
                        rememberPlaybackPosition(
                            player,
                            moving = rememberIsPlaying(player) && lyrics.synced,
                            leadMs = LEAD_MS,
                        ),
                    durationMs = state.durationMs,
                    romanized = state.settings.romanizedLyrics,
                    colors = colors,
                    onSeek = {
                        dispatch(DmtAction.Seek(it))
                        if (playPause.showPlay) playPause.onClick()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                LyricsControls(
                    state = state,
                    player = player,
                    controls = controls,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                )
            } else {
                val title =
                    if (state.lyricsFetching) {
                        R.string.lyrics_fetching
                    } else {
                        R.string.lyrics_none_title
                    }
                EmptyState(
                    title = stringResource(title),
                    body = stringResource(R.string.lyrics_none_body),
                    ink = NightInk,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            val source = state.lyricsShowingFrom ?: state.lyricsSource
            TextLink(
                label = stringResource(R.string.lyrics_from, stringResource(source.label)),
                color = NightInk,
                onClick = { dispatch(DmtAction.OpenLyricsSources) },
            )
            if (lyrics?.lines?.any { it.transliteration != null } == true) {
                WordToggle(
                    label = stringResource(R.string.lyrics_romanized),
                    on = state.settings.romanizedLyrics,
                    onClick = {
                        val settings = state.settings
                        val toggled = settings.copy(romanizedLyrics = !settings.romanizedLyrics)
                        dispatch(DmtAction.Config(toggled))
                    },
                    ink = NightInk,
                    field = Night,
                )
            }
        }
    }
}

@get:StringRes
internal val LyricsSource.label: Int
    get() =
        when (this) {
            LyricsSource.EMBEDDED -> R.string.lyrics_source_embedded
            LyricsSource.LOCAL -> R.string.lyrics_source_local
            LyricsSource.LRCLIB -> R.string.lyrics_source_lrclib
            LyricsSource.LRC_RED -> R.string.lyrics_source_lrc_red
        }
