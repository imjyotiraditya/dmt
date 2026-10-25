package dev.jyotiraditya.dmt.presentation.player

import androidx.media3.common.Player
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinError
import dev.jyotiraditya.dmt.data.remote.jellyfin.toJellyfinError
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicError
import dev.jyotiraditya.dmt.data.remote.subsonic.toSubsonicError
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.model.homeShelves
import dev.jyotiraditya.dmt.library.takeIfSet
import dev.jyotiraditya.dmt.presentation.settings.scanFailedMessage
import dev.jyotiraditya.lyrics.Lyrics

/** This state with the track and length that [player] is on. */
internal fun DmtState.withNowPlaying(player: Player?): DmtState {
    if (player == null) return this
    val meta = player.mediaMetadata
    return copy(
        nowPlayingId = player.currentMediaItem?.mediaId,
        title = meta.title?.toString() ?: title,
        artist = meta.artist?.toString() ?: artist,
        album = meta.albumTitle?.toString() ?: album,
        durationMs = player.duration.takeIfSet()?.coerceAtLeast(0L) ?: durationMs,
        queueIndex = player.currentMediaItemIndex,
        queuePosition = queue.indexOfFirst { it.index == player.currentMediaItemIndex },
    )
}

internal fun DmtState.withLibrary(library: LibrarySnapshot): DmtState =
    copy(library = library, error = null)

internal fun DmtState.withoutLibrary(): DmtState = copy(
    library = LibrarySnapshot(),
    filtered = emptyList(),
    filteredAlbums = emptyList(),
    filteredArtists = emptyList(),
    filteredFolders = emptyList(),
    filteredGenres = emptyList(),
)

internal fun DmtState.withFiltered(filtered: FilteredLibrary): DmtState = copy(
    filtered = filtered.tracks,
    filteredAlbums = filtered.albums,
    filteredArtists = filtered.artists,
    filteredFolders = filtered.folders,
    filteredGenres = filtered.genres,
)

internal fun DmtState.withHome(): DmtState =
    copy(home = homeShelves(tracks, albums, artists, stats.counts))

/** This state with the lyrics read for track [forId], if that track still plays. */
internal fun DmtState.withLyrics(
    forId: String?,
    source: LyricsSource,
    result: Result<Lyrics?>,
    sources: Map<LyricsSource, LyricsSourceStatus>,
): DmtState {
    if (nowPlayingId != forId) return this
    val lyrics = result.getOrNull()
    return copy(
        lyrics = lyrics,
        lyricsFetching = false,
        lyricsSource = source,
        lyricsSources = sources + (source to result.toSourceStatus()),
        lyricsShowingFrom = source.takeIf { lyrics != null },
    )
}

/** This state with the [sources] re-read for track [forId], if that track still plays. */
internal fun DmtState.withLyricsSources(
    forId: String,
    sources: Map<LyricsSource, LyricsSourceStatus>,
): DmtState = if (nowPlayingId == forId) copy(lyricsSources = lyricsSources + sources) else this

internal fun DmtState.withLyricsFetching(source: LyricsSource): DmtState = copy(
    lyricsFetching = true,
    lyricsSources = lyricsSources + (source to LyricsSourceStatus.Fetching),
)

/** This state after fetching from [source]; the lyrics on screen stay when it had none. */
internal fun DmtState.withFetchedLyrics(
    forId: String,
    source: LyricsSource,
    result: Result<Lyrics?>,
): DmtState {
    if (nowPlayingId != forId) return copy(lyricsFetching = false)
    val sources = lyricsSources + (source to result.toSourceStatus())
    val lyrics = result.getOrNull() ?: return copy(lyricsFetching = false, lyricsSources = sources)
    return copy(
        lyrics = lyrics,
        lyricsFetching = false,
        lyricsSources = sources,
        lyricsShowingFrom = source,
    )
}

/** The message for a library scan from the source in [settings] that failed with [failure]. */
internal fun scanFailureMessage(settings: DmtSettings, failure: Throwable): UiText {
    val mode = settings.sourceMode
    val sessionEnded =
        when (mode) {
            SourceMode.JELLYFIN -> failure.toJellyfinError() == JellyfinError.BAD_CREDENTIALS
            SourceMode.SUBSONIC -> failure.toSubsonicError() == SubsonicError.BAD_CREDENTIALS
            SourceMode.LOCAL -> false
        }
    val server = settings.server
    return if (sessionEnded && server != null) {
        UiText(R.string.sources_session_ended, server.name)
    } else {
        UiText(mode.scanFailedMessage)
    }
}
