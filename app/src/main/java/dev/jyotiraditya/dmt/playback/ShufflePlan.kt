package dev.jyotiraditya.dmt.playback

import androidx.media3.common.util.UnstableApi
import dev.jyotiraditya.dmt.domain.model.ShufflePlan
import kotlin.random.Random

/** The order this plan plays in, with the item at its head first. */
@UnstableApi
fun ShufflePlan.order(): AnchoredShuffleOrder {
    val random = Random(seed)
    if (head !in 0 until count) {
        return AnchoredShuffleOrder(IntArray(count) { it }.apply { shuffle(random) })
    }

    // The first track takes the head's place among the rest.
    val rest = IntArray(count - 1) { if (it + 1 == head) 0 else it + 1 }
    rest.shuffle(random)
    return AnchoredShuffleOrder(intArrayOf(head) + rest)
}
