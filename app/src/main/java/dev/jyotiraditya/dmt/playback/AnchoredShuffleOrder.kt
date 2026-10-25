package dev.jyotiraditya.dmt.playback

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder

/**
 * A shuffle order where inserted items follow the item before them in the playlist instead of
 * landing at random, so a track added next plays next even while shuffling.
 */
@UnstableApi
class AnchoredShuffleOrder(private val shuffled: IntArray) : ShuffleOrder {

    private val positionOf =
        IntArray(shuffled.size).also { positions ->
            shuffled.forEachIndexed { position, index -> positions[index] = position }
        }

    override fun getLength(): Int = shuffled.size

    override fun getNextIndex(index: Int): Int =
        positionOf[index].let { if (it + 1 < shuffled.size) shuffled[it + 1] else C.INDEX_UNSET }

    override fun getPreviousIndex(index: Int): Int =
        positionOf[index].let { if (it > 0) shuffled[it - 1] else C.INDEX_UNSET }

    override fun getFirstIndex(): Int = shuffled.firstOrNull() ?: C.INDEX_UNSET

    override fun getLastIndex(): Int = shuffled.lastOrNull() ?: C.INDEX_UNSET

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        val shifted = shuffled.map { if (it >= insertionIndex) it + insertionCount else it }
        val inserted = (insertionIndex until insertionIndex + insertionCount).toList()
        // The new items go right after their playlist predecessor, or first if there is none.
        val anchor = if (insertionIndex == 0) -1 else shifted.indexOf(insertionIndex - 1)
        val order =
            shifted.subList(0, anchor + 1) + inserted + shifted.subList(anchor + 1, shifted.size)
        return AnchoredShuffleOrder(order.toIntArray())
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val removed = indexToExclusive - indexFrom
        val order =
            shuffled
                .filter { it !in indexFrom until indexToExclusive }
                .map { if (it >= indexToExclusive) it - removed else it }
        return AnchoredShuffleOrder(order.toIntArray())
    }

    override fun cloneAndClear(): ShuffleOrder = AnchoredShuffleOrder(IntArray(0))
}
