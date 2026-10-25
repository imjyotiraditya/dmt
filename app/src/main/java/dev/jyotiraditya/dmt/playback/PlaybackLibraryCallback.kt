package dev.jyotiraditya.dmt.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.util.cycleRepeat
import dev.jyotiraditya.dmt.util.resolveQueue
import dev.jyotiraditya.dmt.util.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

/**
 * Answers controllers of the session: the app's commands, browsing for Android Auto, and queue
 * requests.
 */
@OptIn(UnstableApi::class)
internal class PlaybackLibraryCallback(
    private val appPackage: String,
    private val scope: CoroutineScope,
    private val browseTree: MediaBrowseTree,
    private val preferences: PreferencesRepository,
    private val sleepTimer: SleepTimer,
    private val library: suspend () -> LibrarySnapshot,
    private val onQueueReplaced: () -> Unit,
) : MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val commands =
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(PlaybackCommands.SLEEP_SET)
                .add(PlaybackCommands.SLEEP_GET)
                .add(PlaybackCommands.TOGGLE_SHUFFLE)
                .add(PlaybackCommands.CYCLE_REPEAT)
                .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .setAvailableSessionCommands(commands)
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        val player = session.player
        val extras =
            when (customCommand.customAction) {
                PlaybackCommands.SLEEP_SET.customAction -> {
                    sleepTimer.set(
                        endAt = args.getLong(PlaybackCommands.KEY_END_AT),
                        finishSong = args.getBoolean(PlaybackCommands.KEY_FINISH_SONG),
                    )
                    Bundle.EMPTY
                }

                PlaybackCommands.SLEEP_GET.customAction ->
                    Bundle().apply {
                        putLong(PlaybackCommands.KEY_END_AT, sleepTimer.endAt ?: 0L)
                        putBoolean(PlaybackCommands.KEY_FINISH_SONG, sleepTimer.finishSong)
                    }

                PlaybackCommands.TOGGLE_SHUFFLE.customAction -> {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                    Bundle.EMPTY
                }

                PlaybackCommands.CYCLE_REPEAT.customAction -> {
                    player.cycleRepeat()
                    Bundle.EMPTY
                }

                else -> return super.onCustomCommand(session, controller, customCommand, args)
            }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(LibraryResult.ofItem(browseTree.root(), params))

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
        LibraryResult.ofItemList(browseTree.childrenOf(parentId, library()), params)
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
        val track = library().track(mediaId)
        if (track != null) {
            LibraryResult.ofItem(track.toMediaItem(), null)
        } else {
            LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> = scope.future {
        val count = browseTree.search(library().tracks, query).size
        session.notifySearchResultChanged(browser, query, count, params)
        LibraryResult.ofVoid()
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
        val results = browseTree.search(library().tracks, query).map { it.toMediaItem() }
        LibraryResult.ofItemList(results, params)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> = scope.future { resolveItems(mediaItems) }

    /**
     * The app sends the exact queue. Other controllers may send a search or a single album, artist
     * or folder, which is expanded to its tracks.
     */
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
        resolveQueue(controller, mediaItems, startIndex, startPositionMs).also {
            onQueueReplaced()
        }
    }

    private suspend fun resolveQueue(
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): MediaItemsWithStartPosition {
        suspend fun asSent() =
            MediaItemsWithStartPosition(resolveItems(mediaItems), startIndex, startPositionMs)
        if (controller.packageName == appPackage) return asSent()

        val single = mediaItems.singleOrNull() ?: return asSent()
        val library = library()
        val tracks = library.tracks
        val query = single.requestMetadata.searchQuery
        val id = single.mediaId
        return when {
            query != null -> queueOf(browseTree.search(tracks, query).ifEmpty { tracks }, 0)

            id.startsWith(BrowseIds.ALBUM_PREFIX) ->
                groupQueue(id, BrowseIds.ALBUM_PREFIX, tracks, startPositionMs) {
                    library.album(it)?.tracks
                }

            id.startsWith(BrowseIds.ARTIST_PREFIX) ->
                groupQueue(id, BrowseIds.ARTIST_PREFIX, tracks, startPositionMs) {
                    library.artist(it)?.tracks
                }

            id.startsWith(BrowseIds.FOLDER_PREFIX) ->
                groupQueue(id, BrowseIds.FOLDER_PREFIX, tracks, startPositionMs) {
                    library.folder(it)?.tracks
                }

            single.localConfiguration == null -> {
                val index = library.track(id)?.let(tracks::indexOf) ?: -1
                if (index >= 0) queueOf(tracks, index, startPositionMs) else asSent()
            }

            else -> asSent()
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean,
    ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
        val session = preferences.lastSession() ?: throw UnsupportedOperationException()
        val (existing, index, position) =
            session.resolveQueue(library().tracks) ?: throw UnsupportedOperationException()
        mediaSession.player.shuffleModeEnabled = session.shuffle
        queueOf(existing, index, position)
    }

    /** The queue for group item [mediaId], started at its track; the whole library if absent. */
    private fun groupQueue(
        mediaId: String,
        prefix: String,
        tracks: List<Track>,
        startPositionMs: Long,
        tracksFor: (String) -> List<Track>?,
    ): MediaItemsWithStartPosition {
        val key = mediaId.removePrefix(prefix).substringBeforeLast('/')
        val trackId = mediaId.substringAfterLast('/').toLongOrNull()
        val groupTracks = tracksFor(key).orEmpty()
        val index = groupTracks.indexOfFirst { it.id == trackId }
        return if (index >= 0) queueOf(groupTracks, index, startPositionMs) else queueOf(tracks, 0)
    }

    private fun queueOf(list: List<Track>, index: Int, positionMs: Long = 0L) =
        MediaItemsWithStartPosition(list.map { it.toMediaItem() }, index, positionMs)

    /** These items with each id-only request replaced by the library track it names. */
    private suspend fun resolveItems(mediaItems: List<MediaItem>): List<MediaItem> {
        if (mediaItems.all { it.localConfiguration != null }) return mediaItems
        val library = library()
        return mediaItems.map { item ->
            item.takeIf { it.localConfiguration != null }
                ?: library.track(item.mediaId.substringAfterLast('/'))?.toMediaItem()
                ?: item
        }
    }
}
