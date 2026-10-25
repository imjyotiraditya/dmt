package dev.jyotiraditya.dmt.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import org.junit.Assert.assertEquals
import org.junit.Test

@UnstableApi
class AnchoredShuffleOrderTest {

    private fun ShuffleOrder.playOrder(): List<Int> = buildList {
        var index = firstIndex
        while (index != C.INDEX_UNSET) {
            add(index)
            index = getNextIndex(index)
        }
    }

    @Test
    fun walksTheGivenOrder() {
        val order = AnchoredShuffleOrder(intArrayOf(2, 0, 3, 1))
        assertEquals(listOf(2, 0, 3, 1), order.playOrder())
        assertEquals(C.INDEX_UNSET, order.getPreviousIndex(2))
        assertEquals(3, order.getPreviousIndex(1))
        assertEquals(1, order.lastIndex)
    }

    @Test
    fun insertedAfterCurrentPlaysNext() {
        val order = AnchoredShuffleOrder(intArrayOf(2, 0, 3, 1)).cloneAndInsert(1, 2)
        assertEquals(listOf(4, 0, 1, 2, 5, 3), order.playOrder())
    }

    @Test
    fun appendedPlaysAfterTheLastItemOfThePlaylist() {
        val order = AnchoredShuffleOrder(intArrayOf(2, 0, 3, 1)).cloneAndInsert(4, 1)
        assertEquals(listOf(2, 0, 3, 4, 1), order.playOrder())
    }

    @Test
    fun insertedAtTheStartPlaysFirst() {
        val order = AnchoredShuffleOrder(intArrayOf(1, 0)).cloneAndInsert(0, 1)
        assertEquals(listOf(0, 2, 1), order.playOrder())
    }

    @Test
    fun removesAndRenumbers() {
        val order = AnchoredShuffleOrder(intArrayOf(2, 0, 3, 1)).cloneAndRemove(1, 3)
        assertEquals(listOf(0, 1), order.playOrder())
        assertEquals(0, order.cloneAndClear().length)
    }

    @Test
    fun planKeepsTheCurrentTrackFirst() {
        val order = ShufflePlan(seed = 42, head = 3, count = 6).order()
        assertEquals(3, order.firstIndex)
        assertEquals((0 until 6).toSet(), order.playOrder().toSet())
    }
}
