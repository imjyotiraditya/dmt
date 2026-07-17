package dev.jyotiraditya.dmt.data.source.local.cue

import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CueLibraryTest {

    private val base = Track(
        id = 42L,
        uri = Uri.EMPTY,
        title = "Brothers In Arms (Full Length Version)",
        artist = "Dire Straits",
        album = "Brothers In Arms [1985]",
        path = "/storage/emulated/0/Music/dire/album.flac",
        durationMs = 1_020_000L,
        mime = "audio/flac",
        bitrate = 2_304_000,
        size = 313_242_237L,
        trackNumber = 0,
    )

    private val sheet = CueSheet(
        title = "Brothers In Arms (Full Length Version)",
        performer = "Dire Straits",
        files = listOf(
            CueFile(
                name = "album.flac",
                tracks = listOf(
                    CueTrack(1, "Brothers In Arms", null, 0L),
                    CueTrack(2, "Going Home", "Mark Knopfler", 419_000L),
                    CueTrack(3, "Why Worry", null, 720_000L),
                ),
            ),
        ),
    )

    @Test
    fun `split maps cue tracks onto clipped virtual tracks`() {
        val tracks = CueLibrary.split(base, sheet, sheet.files.first().tracks)
        assertEquals(3, tracks.size)

        val (first, second, third) = tracks

        // ids are negative and unique so they never collide with MediaStore ids
        assertTrue(tracks.all { it.id < 0 })
        assertEquals(tracks.size, tracks.map { it.id }.distinct().size)

        // first track needs no start clip, last track no end clip
        assertNull(first.clipStartMs)
        assertEquals(419_000L, first.clipEndMs)
        assertEquals(419_000L, second.clipStartMs)
        assertEquals(720_000L, second.clipEndMs)
        assertEquals(720_000L, third.clipStartMs)
        assertNull(third.clipEndMs)

        assertEquals(listOf(419_000L, 301_000L, 300_000L), tracks.map { it.durationMs })
        assertEquals(listOf(1, 2, 3), tracks.map { it.trackNumber })

        // per-track performer wins, otherwise the sheet performer
        assertEquals("Mark Knopfler", second.artist)
        assertEquals("Dire Straits", third.artist)
        assertEquals("Brothers In Arms (Full Length Version)", first.album)

        // the underlying media stays the whole file, sizes are proportional
        assertTrue(tracks.all { it.path == base.path && it.uri == base.uri })
        assertTrue(base.size - tracks.sumOf { it.size } in 0 until tracks.size)
    }

    @Test
    fun `expand leaves tracks without a matching cue untouched`() {
        assertEquals(listOf(base), CueLibrary.expand(listOf(base)))
    }
}
