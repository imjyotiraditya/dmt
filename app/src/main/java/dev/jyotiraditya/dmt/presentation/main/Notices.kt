package dev.jyotiraditya.dmt.presentation.main

import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.di.ApplicationScope
import dev.jyotiraditya.dmt.util.lingering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val NOTICE_TIME = 2.seconds

/** The short notices and errors shown at the foot of the screen. */
@Singleton
class Notices @Inject constructor(@ApplicationScope private val scope: CoroutineScope) {

    private val notices =
        MutableSharedFlow<UiText>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** A passing confirmation, cleared on its own after a moment. */
    val notice: StateFlow<UiText?> =
        notices.lingering(NOTICE_TIME).stateIn(scope, SharingStarted.Eagerly, null)

    /** A failure that stays until it is cleared. */
    val error: StateFlow<UiText?>
        field = MutableStateFlow<UiText?>(null)

    fun notify(message: UiText) {
        notices.tryEmit(message)
    }

    fun fail(message: UiText) {
        error.value = message
    }

    fun clearError() {
        error.value = null
    }
}
