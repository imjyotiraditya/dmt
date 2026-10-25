package dev.jyotiraditya.dmt.playback

import androidx.media3.common.util.UnstableApi
import kotlin.random.Random

/** A Fisher-Yates shuffle from [seed] with the track at [head] pinned first, so it can be saved. */
data class ShufflePlan(val seed: Long, val head: Int, val count: Int) {

    @UnstableApi
    fun order(): AnchoredShuffleOrder {
        val random = Random(seed)
        val order = IntArray(count) { it }
        val first = if (head in 0 until count) 1 else 0

        if (first == 1) {
            order[head] = order[0]
            order[0] = head
        }

        for (i in count - 1 downTo first + 1) {
            val j = random.nextInt(first, i + 1)
            val swapped = order[i]
            order[i] = order[j]
            order[j] = swapped
        }

        return AnchoredShuffleOrder(order)
    }

    override fun toString(): String = "$seed;$head;$count"

    companion object {
        fun parse(value: String): ShufflePlan? {
            val parts = value.split(';')
            if (parts.size != 3) return null
            val seed = parts[0].toLongOrNull() ?: return null
            val head = parts[1].toIntOrNull() ?: return null
            val count = parts[2].toIntOrNull() ?: return null

            return ShufflePlan(seed, head, count)
        }
    }
}
