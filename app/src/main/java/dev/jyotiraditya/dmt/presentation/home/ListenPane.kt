package dev.jyotiraditya.dmt.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.presentation.library.TrackMenuNavigation
import dev.jyotiraditya.dmt.presentation.library.artistSummary
import dev.jyotiraditya.dmt.presentation.library.rememberTrackMenu
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberWantsPlayback
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.settings.noTracksMessage
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.CoverTile
import dev.jyotiraditya.dmt.ui.components.EmptyState
import dev.jyotiraditya.dmt.ui.components.SectionHeading
import dev.jyotiraditya.dmt.ui.components.ShelfPlaceholders
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.TrackRow
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.util.asHoursMinutes
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val SHELF = 12
private const val MOST_PLAYED = 5
private val COVER = 156.dp

/** The home page: a greeting, then shelves of repeats, most played, artists and unplayed tracks. */
@Composable
fun ListenPane(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSources: () -> Unit,
) {
    val home = state.home
    // The newest albums until enough has been played.
    val onRepeat: List<Album> =
        remember(home.albums, state.albums) {
            home.albums
                .ifEmpty {
                    state.albums.sortedByDescending { album -> album.tracks.maxOf { it.dateAdded } }
                }
                .take(SHELF)
        }

    val openTrackMenu =
        rememberTrackMenu(
            state = state,
            dispatch = dispatch,
            navigation = TrackMenuNavigation(openAlbum = onOpenAlbum, openArtist = onOpenArtist),
        )
    val playing = rememberWantsPlayback()

    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item(key = "greeting") { Greeting(state, dispatch, onOpenLibrary) }

        when {
            state.scanning && state.tracks.isEmpty() ->
                item {
                    ShelfPlaceholders(
                        text = stringResource(R.string.library_scanning),
                        cover = COVER,
                    )
                }

            state.tracks.isEmpty() ->
                item {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(state.settings.sourceMode.noTracksMessage),
                        modifier = Modifier.padding(horizontal = Margin),
                        action = {
                            Block(
                                label = stringResource(R.string.home_empty_action),
                                onClick = onOpenSources,
                            )
                        },
                    )
                }

            else -> {
                if (onRepeat.isNotEmpty()) {
                    item(key = "repeat") {
                        val heading =
                            if (home.albums.isNotEmpty()) {
                                R.string.home_on_repeat
                            } else {
                                R.string.home_recent
                            }
                        SectionHeading(stringResource(heading))
                        CoverShelf(
                            items = onRepeat,
                            key = { it.id },
                            cover = { it.tracks.first() },
                            title = { it.name },
                            meta = { it.artist },
                            onClick = { onOpenAlbum(it.id) },
                        )
                    }
                }
                if (home.tracks.isNotEmpty()) {
                    item(key = "most") { SectionHeading(stringResource(R.string.home_most_played)) }
                    itemsIndexed(
                        home.tracks.take(MOST_PLAYED),
                        key = { _, t -> "most:${t.id}" },
                    ) { index, track ->
                        TrackRow(
                            track = track,
                            meta = track.artist.asCredit(),
                            current = track.id.toString() == state.nowPlayingId,
                            playing = playing,
                            onClick = { dispatch(DmtAction.PlayAt(home.tracks, index)) },
                            onLongClick = { openTrackMenu(track) },
                        )
                    }
                }
                if (home.artists.isNotEmpty()) {
                    item(key = "artists") {
                        SectionHeading(stringResource(R.string.home_artists))
                        CoverShelf(
                            items = home.artists,
                            key = { it.name },
                            cover = { it.tracks.first() },
                            title = { it.name },
                            meta = { artistSummary(it) },
                            onClick = { onOpenArtist(it.name) },
                        )
                    }
                }
                if (home.fresh.isNotEmpty()) {
                    item(key = "fresh") {
                        SectionHeading(stringResource(R.string.home_not_heard))
                        CoverShelf(
                            items = home.fresh,
                            key = { it.id },
                            cover = { it },
                            title = { it.title },
                            meta = { it.artist.asCredit() },
                            onClick = { track ->
                                dispatch(
                                    DmtAction.PlayAt(
                                        home.fresh,
                                        home.fresh.indexOf(track),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The time of day's greeting, listening totals, and the shuffle action. */
@Composable
private fun Greeting(state: DmtState, dispatch: (DmtAction) -> Unit, onOpenLibrary: () -> Unit) {
    val part = remember {
        when (Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..16 -> R.string.home_greeting_afternoon
            in 17..21 -> R.string.home_greeting_evening
            else -> R.string.home_greeting_night
        }
    }
    Column(modifier = Modifier.padding(horizontal = Margin).padding(top = 20.dp)) {
        Text(
            text = stringResource(part),
            style = MaterialTheme.typography.headlineLarge,
            color = Ink,
        )
        val plays = state.stats.counts.values.sum()
        Text(
            text =
                if (state.stats.totalMs > 60_000L) {
                    stringResource(
                        R.string.home_greeting_together,
                        state.stats.totalMs.asHoursMinutes(),
                        pluralStringResource(R.plurals.count_plays, plays, plays),
                    )
                } else {
                    pluralStringResource(
                        R.plurals.home_greeting_waiting,
                        state.tracks.size,
                        state.tracks.size,
                    )
                },
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (state.tracks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Block(
                    label = stringResource(R.string.home_shuffle_all),
                    icon = DmtIcons.Shuffle,
                    onClick = { dispatch(DmtAction.ShufflePlay(state.tracks)) },
                )
                TextLink(label = stringResource(R.string.home_see_library), onClick = onOpenLibrary)
            }
        }
    }
}

/** A horizontally scrolling row of covers with captions. */
@Composable
private fun <T : Any> CoverShelf(
    items: List<T>,
    key: (T) -> Any,
    cover: (T) -> Track,
    title: (T) -> String,
    meta: @Composable (T) -> String,
    onClick: (T) -> Unit,
    size: Dp = COVER,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = Margin),
    ) {
        items(items, key = key) { item ->
            CoverTile(
                cover = cover(item),
                title = title(item),
                subtitle = meta(item),
                onClick = { onClick(item) },
                modifier = Modifier.width(size),
            )
        }
    }
}
