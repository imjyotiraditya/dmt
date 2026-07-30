package dev.jyotiraditya.dmt.presentation.library

import android.net.Uri
import dev.jyotiraditya.dmt.domain.model.FolderNode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderSortTest {

    private fun node(name: String, songCount: Int, lastModified: Long) = FolderNode(
        id = name,
        name = name,
        absolutePath = "/storage/emulated/0/$name",
        parentPath = null,
        childFolderCount = 0,
        songCount = songCount,
        artwork = null as Uri?,
        lastModified = lastModified,
        children = emptyList(),
        songs = emptyList(),
    )

    private val nodes = listOf(
        node("Coldplay", songCount = 12, lastModified = 200L),
        node("ambient", songCount = 3, lastModified = 500L),
        node("Bowie", songCount = 40, lastModified = 100L),
    )

    @Test
    fun `alphabetical sort is case-insensitive`() {
        val sorted = nodes.sortedWith(FolderSort.ALPHABETICAL.comparator).map { it.name }

        assertEquals(listOf("ambient", "Bowie", "Coldplay"), sorted)
    }

    @Test
    fun `reverse alphabetical sort is case-insensitive`() {
        val sorted = nodes.sortedWith(FolderSort.REVERSE_ALPHABETICAL.comparator).map { it.name }

        assertEquals(listOf("Coldplay", "Bowie", "ambient"), sorted)
    }

    @Test
    fun `recently modified sorts newest first`() {
        val sorted = nodes.sortedWith(FolderSort.RECENT_MODIFIED.comparator).map { it.name }

        assertEquals(listOf("ambient", "Coldplay", "Bowie"), sorted)
    }

    @Test
    fun `oldest sorts oldest first`() {
        val sorted = nodes.sortedWith(FolderSort.OLDEST.comparator).map { it.name }

        assertEquals(listOf("Bowie", "Coldplay", "ambient"), sorted)
    }

    @Test
    fun `most songs sorts highest count first`() {
        val sorted = nodes.sortedWith(FolderSort.MOST_SONGS.comparator).map { it.name }

        assertEquals(listOf("Bowie", "Coldplay", "ambient"), sorted)
    }

    @Test
    fun `next cycles through every sort and wraps around`() {
        var sort = FolderSort.ALPHABETICAL
        val seen = mutableListOf(sort)
        repeat(FolderSort.entries.size) {
            sort = sort.next()
            seen += sort
        }

        assertEquals(FolderSort.entries + FolderSort.ALPHABETICAL, seen)
    }
}
