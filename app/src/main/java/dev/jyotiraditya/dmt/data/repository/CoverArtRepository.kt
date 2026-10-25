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

/** Image extensions accepted for folder covers, in order of preference. */
private val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")

private val COVER_NAMES = setOf("cover", "folder", "front", "album", "albumart", "artwork")

@Singleton
class CoverArtRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {

    /**
     * The embedded cover, the media store's album art, or a cover image in the track's folder, at
     * full size. [embedded] reads the file itself, which costs the most.
     */
    suspend fun loadArt(track: Track, embedded: Boolean = true): Bitmap? {
        val cover = track.coverUri
        if (cover?.scheme == "http" || cover?.scheme == "https") return loadCachedArt(cover)

        return (if (embedded) loadEmbeddedArt(track.uri) else null)
            ?: cover?.let { loadCachedArt(it) }
            ?: loadFolderArt(track.path)
    }

    private suspend fun loadFolderArt(path: String): Bitmap? {
        val folder = File(path).parentFile?.takeIf { it.holdsAnAlbum() } ?: return null
        val images = runCatching {
            folder.listFiles { file -> file.extension.lowercase() in IMAGE_EXTENSIONS }
        }
            .getOrNull()
            .orEmpty()

        val named = images.filter { it.nameWithoutExtension.lowercase() in COVER_NAMES }
        val cover =
            named
                .ifEmpty { images.asList() }
                .minByOrNull { IMAGE_EXTENSIONS.indexOf(it.extension.lowercase()) }

        return cover?.let { loadCachedArt(it.toUri()) }
    }

    /** Whether this directory is an album folder, not Downloads or an unknown volume. */
    private fun File.holdsAnAlbum(): Boolean {
        val holder = parentFile ?: return false
        return name != Environment.DIRECTORY_DOWNLOADS &&
            Environment.getExternalStorageState(holder) != Environment.MEDIA_UNKNOWN
    }

    private suspend fun loadCachedArt(uri: Uri): Bitmap? {
        val request = ImageRequest.Builder(context).data(uri).build()
        val result = imageLoader.execute(request) as? SuccessResult ?: return null
        return (result.image as? BitmapImage)?.bitmap
    }

    private fun loadEmbeddedArt(fileUri: Uri): Bitmap? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, fileUri)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    }
        .getOrNull()
}
