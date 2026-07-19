package dev.jyotiraditya.dmt.domain.usecase

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import kotlinx.coroutines.Dispatchers
import dev.jyotiraditya.metadata.AudioTags
import dev.jyotiraditya.metadata.TagKey
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class EmbedLyricsUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    suspend fun writeRequest(track: Track): IntentSender? =
        withContext(Dispatchers.IO) {
            if (track.source != TrackSource.LOCAL || !AudioTags.canWrite(track.path)) {
                return@withContext null
            }

            val uri =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.id)
            MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender
        }

    suspend operator fun invoke(track: Track, text: String): Boolean =
        withContext(Dispatchers.IO) {
            AudioTags.write(
                file = File(track.path),
                tempDir = context.cacheDir,
                tags = mapOf(TagKey.LYRICS to text),
            )
        }
}
