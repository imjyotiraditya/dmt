package dev.jyotiraditya.dmt.data.repository

import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderRepositoryTest {

    private val base = Track(
        id = 1L,
        uri = Uri.EMPTY,
        title = "",
        artist = "",
        album = "",
        path = "",
        durationMs = 200_000L,
        mime = "audio/mpeg",
        bitrate = 320_000,
        size = 8_000_000L,
        trackNumber = 1,
    )

    @Test
    fun `starts empty until refreshed`() {
        val repository = FolderRepository()

        assertTrue(repository.tree.value.isEmpty())
    }

    @Test
    fun `refresh caches a tree built from the given tracks`() {
        val repository = FolderRepository()
        val tracks = listOf(
            base.copy(id = 1, path = "/storage/emulated/0/Music/Coldplay/01.mp3"),
        )

        repository.refresh(tracks)

        assertEquals(listOf("Music"), repository.tree.value.map { it.name })
    }

    @Test
    fun `refresh replaces the previously cached tree`() {
        val repository = FolderRepository()
        repository.refresh(listOf(base.copy(id = 1, path = "/storage/emulated/0/Music/a.mp3")))

        repository.refresh(listOf(base.copy(id = 2, path = "/storage/emulated/0/Podcasts/b.mp3")))

        assertEquals(listOf("Podcasts"), repository.tree.value.map { it.name })
    }

    @Test
    fun `refresh with no tracks clears the tree`() {
        val repository = FolderRepository()
        repository.refresh(listOf(base.copy(id = 1, path = "/storage/emulated/0/Music/a.mp3")))

        repository.refresh(emptyList())

        assertTrue(repository.tree.value.isEmpty())
    }
}
