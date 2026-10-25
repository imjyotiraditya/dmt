package dev.jyotiraditya.dmt.playback

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * A [BitmapLoader] that hands out a copy of every bitmap [delegate] loads, since some ROMs recycle
 * the artwork the platform session is given while media3 still reuses it.
 */
@OptIn(UnstableApi::class)
internal class FreshCopyBitmapLoader(private val delegate: BitmapLoader) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        fresh(delegate.decodeBitmap(data))

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = fresh(delegate.loadBitmap(uri))

    private fun fresh(future: ListenableFuture<Bitmap>): ListenableFuture<Bitmap> =
        Futures.transform(
            future,
            { it.copy(it.config ?: Bitmap.Config.ARGB_8888, false) ?: it },
            MoreExecutors.directExecutor(),
        )
}
