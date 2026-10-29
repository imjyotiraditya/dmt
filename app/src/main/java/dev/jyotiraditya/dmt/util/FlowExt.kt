package dev.jyotiraditya.dmt.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration

/** Each value, then null once [time] passes without a newer one. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> Flow<T>.lingering(time: Duration): Flow<T?> = transformLatest {
    emit(it)
    delay(time)
    emit(null)
}
