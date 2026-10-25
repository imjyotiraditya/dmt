package dev.jyotiraditya.dmt.presentation.player

import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.toLibrary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class LibraryFilterTest {

    private fun track(id: Long, title: String, artist: String, album: String) = Track(
        id = id,
        uri = mock(Uri::class.java),
        title = title,
        artist = artist,
        album = album,
        path = "/storage/emulated/0/Music/$album/$id.mp3",
        durationMs = 1_000L,
        mime = "audio/mpeg",
        bitrate = 0,
        size = 0L,
        trackNumber = 0,
    )

    private val library = runBlocking {
        listOf(
            track(1, "Yellow", "Coldplay", "Parachutes"),
            track(2, "Clocks", "Coldplay", "A Rush of Blood"),
            track(3, "Creep", "Radiohead", "Pablo Honey"),
        )
            .toLibrary()
    }

    @Test
    fun aBlankQueryKeepsEverythingInSortOrder() {
        val filtered = library.filtered("  ", LibrarySort.TITLE)
        assertEquals(listOf("Clocks", "Creep", "Yellow"), filtered.tracks.map { it.title })
        assertEquals(3, filtered.albums.size)
    }

    @Test
    fun aQueryMatchesTitleArtistOrAlbumIgnoringCase() {
        assertEquals(
            listOf(1L, 2L),
            library.filtered("COLD", LibrarySort.TITLE).tracks.map { it.id }.sorted(),
        )
        assertEquals(listOf(3L), library.filtered("pablo", LibrarySort.TITLE).tracks.map { it.id })
        assertEquals(listOf(1L), library.filtered("yell", LibrarySort.TITLE).tracks.map { it.id })
    }

    @Test
    fun aQueryNarrowsAlbumsArtistsAndFolders() {
        val filtered = library.filtered("radiohead", LibrarySort.TITLE)
        assertEquals(listOf("Pablo Honey"), filtered.albums.map { it.name })
        assertEquals(listOf("Radiohead"), filtered.artists.map { it.name })
        assertEquals(0, library.filtered("storage", LibrarySort.TITLE).folders.size)
    }
}
