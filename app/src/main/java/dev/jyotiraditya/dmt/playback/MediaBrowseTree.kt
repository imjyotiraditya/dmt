package dev.jyotiraditya.dmt.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.util.toMediaItem

/** Media ids of the browsable nodes, shared by the tree and the session that resolves picks. */
internal object BrowseIds {
    const val ROOT = "root"
    const val TRACKS = "tracks"
    const val ALBUMS = "albums"
    const val ALBUM_PREFIX = "album/"
    const val ARTISTS = "artists"
    const val ARTIST_PREFIX = "artist/"
    const val FOLDERS = "folders"
    const val FOLDER_PREFIX = "folder/"
}

/** The library as Android Auto and other media browsers see it. */
internal class MediaBrowseTree(private val context: Context) {

    fun root(): MediaItem = folder(BrowseIds.ROOT, context.getString(R.string.app_name))

    fun childrenOf(parentId: String, library: LibrarySnapshot): List<MediaItem> = when {
        parentId == BrowseIds.ROOT ->
            buildList {
                add(folder(BrowseIds.TRACKS, context.getString(R.string.auto_tracks)))
                add(
                    folder(
                        BrowseIds.ALBUMS,
                        context.getString(R.string.auto_albums),
                        childrenAsGrid = true,
                    ),
                )
                add(folder(BrowseIds.ARTISTS, context.getString(R.string.auto_artists)))
                if (library.folders.isNotEmpty()) {
                    add(folder(BrowseIds.FOLDERS, context.getString(R.string.auto_folders)))
                }
            }

        parentId == BrowseIds.TRACKS ->
            library.tracksSortedBy(LibrarySort.TITLE).map { it.toMediaItem() }

        parentId == BrowseIds.ALBUMS ->
            library.albums.map { album ->
                folder(
                    id = BrowseIds.ALBUM_PREFIX + album.id,
                    title = album.name,
                    subtitle = album.artist,
                    artwork = album.tracks.firstOrNull()?.coverUri,
                )
            }

        parentId.startsWith(BrowseIds.ALBUM_PREFIX) -> {
            library
                .album(parentId.removePrefix(BrowseIds.ALBUM_PREFIX))
                ?.tracks
                .orEmpty()
                .under(parentId)
        }

        parentId == BrowseIds.ARTISTS ->
            library.artists.map { artist ->
                folder(
                    id = BrowseIds.ARTIST_PREFIX + artist.name,
                    title = artist.name,
                    artwork = artist.tracks.firstOrNull()?.coverUri,
                )
            }

        parentId.startsWith(BrowseIds.ARTIST_PREFIX) -> {
            val name = parentId.removePrefix(BrowseIds.ARTIST_PREFIX)
            library.artist(name)?.tracks.orEmpty().under(parentId)
        }

        parentId == BrowseIds.FOLDERS ->
            library.folders.map { folder ->
                folder(
                    id = BrowseIds.FOLDER_PREFIX + folder.path,
                    title =
                        folder.name.ifEmpty {
                            context.getString(R.string.folders_storage_root)
                        },
                )
            }

        parentId.startsWith(BrowseIds.FOLDER_PREFIX) -> {
            val path = parentId.removePrefix(BrowseIds.FOLDER_PREFIX)
            library.folder(path)?.tracks.orEmpty().under(parentId)
        }

        else -> emptyList()
    }

    fun search(tracks: List<Track>, query: String): List<Track> = tracks.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
    }

    /** These tracks as playable children of [parentId], their ids prefixed with it. */
    private fun List<Track>.under(parentId: String): List<MediaItem> = map { track ->
        track.toMediaItem().buildUpon().setMediaId("$parentId/${track.id}").build()
    }

    @OptIn(UnstableApi::class)
    private fun folder(
        id: String,
        title: String,
        subtitle: String? = null,
        artwork: Uri? = null,
        childrenAsGrid: Boolean = false,
    ): MediaItem {
        val style =
            if (childrenAsGrid) {
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            } else {
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            }
        val extras =
            Bundle().apply {
                putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, style)
            }
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .setArtworkUri(artwork)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }
}
