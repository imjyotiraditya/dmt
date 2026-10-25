package dev.jyotiraditya.dmt.library

import kotlin.coroutines.cancellation.CancellationException

/** [runCatching] that rethrows [CancellationException], for suspending code. */
internal inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}
