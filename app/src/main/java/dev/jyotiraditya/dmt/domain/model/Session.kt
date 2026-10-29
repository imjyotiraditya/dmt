package dev.jyotiraditya.dmt.domain.model

import kotlinx.serialization.Serializable

/** A shuffle of [count] items drawn from [seed], with the item at [head] played first. */
@Serializable data class ShufflePlan(val seed: Long, val head: Int, val count: Int)

/** The queue and the place in it, saved for resuming later. */
@Serializable
data class LastSession(
    val queueIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean = false,
    val shufflePlan: ShufflePlan? = null,
)
