package dev.jyotiraditya.dmt.domain.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackMappersTest {

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

    private fun track(id: Long, path: String, title: String = "track$id") =
        base.copy(id = id, path = path, title = title)

    @Test
    fun `builds nested folders from track paths without duplicates`() {
        val tracks = listOf(
            track(1, "/storage/emulated/0/Music/Coldplay/Parachutes/01.mp3"),
            track(2, "/storage/emulated/0/Music/Coldplay/Parachutes/02.mp3"),
            track(3, "/storage/emulated/0/Music/Coldplay/ARushOfBloodToTheHead/01.mp3"),
            track(4, "/storage/emulated/0/Podcasts/EpisodeOne.mp3"),
        )

        val tree = tracks.toFolderTree()

        assertEquals(listOf("Music", "Podcasts"), tree.map { it.name })

        val music = tree.first { it.name == "Music" }
        assertEquals(1, music.childFolderCount)
        assertEquals(3, music.songCount)
        assertTrue(music.songs.isEmpty())
        assertNull(music.parentPath)

        val coldplay = music.children.single()
        assertEquals("Coldplay", coldplay.name)
        assertEquals("/storage/emulated/0/Music", coldplay.parentPath)
        assertEquals(2, coldplay.childFolderCount)
        assertEquals(3, coldplay.songCount)

        val parachutes = coldplay.children.first { it.name == "Parachutes" }
        assertEquals("/storage/emulated/0/Music/Coldplay", parachutes.parentPath)
        assertEquals(2, parachutes.songs.size)

        val podcasts = tree.first { it.name == "Podcasts" }
        assertEquals(1, podcasts.songs.size)
        assertEquals(0, podcasts.childFolderCount)
    }

    @Test
    fun `ignores tracks with no path and folders sit at the storage root`() {
        val tracks = listOf(
            base.copy(id = 1, path = ""),
            track(2, "/storage/emulated/0/track.mp3"),
        )

        assertTrue(tracks.toFolderTree().isEmpty())
    }

    @Test
    fun `flatten returns every node at every depth`() {
        val tracks = listOf(
            track(1, "/storage/emulated/0/Music/Coldplay/Parachutes/01.mp3"),
        )
        val tree = tracks.toFolderTree()

        val names = tree.flatten().map { it.name }

        assertEquals(listOf("Music", "Coldplay", "Parachutes"), names)
    }

    @Test
    fun `findNode locates a node anywhere in the tree`() {
        val tracks = listOf(
            track(1, "/storage/emulated/0/Music/Coldplay/Parachutes/01.mp3"),
        )
        val tree = tracks.toFolderTree()

        val found = tree.findNode("/storage/emulated/0/Music/Coldplay/Parachutes")

        assertEquals("Parachutes", found?.name)
        assertEquals(1, found?.songs?.size)
    }

    @Test
    fun `allSongs collects songs from every descendant subfolder`() {
        val tracks = listOf(
            track(1, "/storage/emulated/0/Music/Coldplay/Parachutes/01.mp3"),
            track(2, "/storage/emulated/0/Music/Coldplay/ARushOfBloodToTheHead/01.mp3"),
        )
        val coldplay = tracks.toFolderTree().flatten().first { it.name == "Coldplay" }

        assertEquals(setOf(1L, 2L), coldplay.allSongs().map { it.id }.toSet())
    }
}
