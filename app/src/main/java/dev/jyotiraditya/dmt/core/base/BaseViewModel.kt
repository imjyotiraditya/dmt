package dev.jyotiraditya.dmt.core.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** A view model that takes [I] intents and publishes [S] state. */
abstract class BaseViewModel<I, S>(initialState: S) : ViewModel() {

    val state: StateFlow<S>
        field = MutableStateFlow(initialState)

    protected val currentState: S
        get() = state.value

    abstract fun onIntent(intent: I)

    protected fun reduce(transform: (S) -> S) {
        state.update(transform)
    }

    protected val store: StateStore<S> =
        object : StateStore<S> {
            override val current: S
                get() = currentState

            override fun reduce(transform: (S) -> S) = this@BaseViewModel.reduce(transform)
        }
}

interface StateStore<S> {
    val current: S

    fun reduce(transform: (S) -> S)
}
