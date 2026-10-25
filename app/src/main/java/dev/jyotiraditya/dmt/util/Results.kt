package dev.jyotiraditya.dmt.util

import kotlin.coroutines.cancellation.CancellationException

/** [runCatching] that lets coroutine cancellation through. */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}
