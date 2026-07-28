package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverArtRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {

    suspend fun loadArt(uri: Uri, fileUri: Uri? = null): Bitmap? =
        if (uri.scheme == "http" || uri.scheme == "https") {
            loadCachedArt(uri)
        } else {
            fileUri?.let(::loadEmbeddedArt) ?: loadCachedArt(uri)
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
    }.getOrNull()
}
