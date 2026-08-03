package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.Track
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** The kinds of image a folder may hold a cover in, best first. */
private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

/** The names given to the image that is the cover of the album a folder holds. */
private val COVER_NAMES = setOf("cover", "folder", "front", "album", "albumart", "artwork")

/** Reads the image shown for a track, from wherever the track happens to keep one. */
@Singleton
class CoverArtRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {

    /**
     * Returns the cover a track carries, that the platform holds for its album, or that sits in
     * the folder beside it.
     *
     * @param track The track to read the cover of.
     * @param embedded Whether to read the file itself, which costs far more than the rest.
     * @return The cover, or null if none of the three hold one.
     */
    suspend fun loadArt(track: Track, embedded: Boolean = true): Bitmap? {
        val cover = track.coverUri
        if (cover?.scheme == "http" || cover?.scheme == "https") return loadCachedArt(cover)

        return (if (embedded) loadEmbeddedArt(track.uri) else null)
            ?: cover?.let { loadCachedArt(it) }
            ?: loadFolderArt(track.path)
    }

    /**
     * Returns the image that sits in the folder of the track at [path].
     *
     * @param path The path of the track to look beside.
     * @return The image, or null if the folder is one anything may land in or holds none.
     */
    private suspend fun loadFolderArt(path: String): Bitmap? {
        val folder = File(path).parentFile?.takeIf { it.holdsAnAlbum() } ?: return null
        val images = runCatching {
            folder.listFiles { file -> file.extension.lowercase() in IMAGE_EXTENSIONS }
        }.getOrNull().orEmpty()

        val named = images.filter { it.nameWithoutExtension.lowercase() in COVER_NAMES }
        val cover = named
            .ifEmpty { images.asList() }
            .minByOrNull { IMAGE_EXTENSIONS.indexOf(it.extension.lowercase()) }

        return cover?.let { loadCachedArt(it.toUri()) }
    }

    /** Whether [this] was made to hold an album rather than whatever is put on the device. */
    private fun File.holdsAnAlbum(): Boolean {
        val holder = parentFile ?: return false
        return name != Environment.DIRECTORY_DOWNLOADS &&
            Environment.getExternalStorageState(holder) != Environment.MEDIA_UNKNOWN
    }

    /** Returns the image at [uri], which is kept once it has been read. */
    private suspend fun loadCachedArt(uri: Uri): Bitmap? {
        val request = ImageRequest.Builder(context).data(uri).build()
        val result = imageLoader.execute(request) as? SuccessResult ?: return null
        return (result.image as? BitmapImage)?.bitmap
    }

    /** Returns the image the file at [fileUri] carries in its tags, or null if it carries none. */
    private fun loadEmbeddedArt(fileUri: Uri): Bitmap? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, fileUri)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    }.getOrNull()
}
