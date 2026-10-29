package dev.jyotiraditya.dmt.playback

import android.content.Context
import androidx.media3.session.MediaController
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.util.mediaController
import javax.inject.Inject

class PlaybackConnection
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** A controller connected to the playback service, telling [listener]; release it when done. */
    suspend fun connect(listener: MediaController.Listener): MediaController =
        context.mediaController(listener)
}
