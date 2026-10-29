package dev.jyotiraditya.dmt.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinError
import dev.jyotiraditya.dmt.data.remote.jellyfin.toJellyfinError
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicError
import dev.jyotiraditya.dmt.data.remote.subsonic.toSubsonicError
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.AccentColor
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.settings.scanFailedMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    /** Whether the settings have been read, so the right first screen can show. */
    val loaded: Boolean = false,
    val setupDone: Boolean = false,
    val accent: AccentColor = AccentColor.GREEN,
)

/** What the whole app shows: setup or the main screen, its accent, and the notices at its foot. */
@HiltViewModel
class AppViewModel
@Inject
constructor(
    libraryRepository: LibraryRepository,
    preferences: PreferencesRepository,
    private val notices: Notices,
) : ViewModel() {

    val state: StateFlow<AppUiState> =
        preferences.loadedSettings
            .map(::stateOf)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                stateOf(preferences.loadedSettings.value),
            )

    val notice: StateFlow<UiText?> = notices.notice

    val error: StateFlow<UiText?> = notices.error

    init {
        viewModelScope.launch {
            libraryRepository.failure.collect { failure ->
                if (failure == null) {
                    notices.clearError()
                } else {
                    val settings = preferences.loadedSettings.filterNotNull().first()
                    notices.fail(scanFailureMessage(settings, failure))
                }
            }
        }
    }

    /** Clears the error once the user moves on to another screen. */
    fun dismissError() = notices.clearError()
}

private fun stateOf(settings: DmtSettings?) = AppUiState(
    loaded = settings != null,
    setupDone = settings?.setupDone == true,
    accent = settings?.accent ?: AccentColor.GREEN,
)

/** The message for a library scan from the source in [settings] that failed with [failure]. */
private fun scanFailureMessage(settings: DmtSettings, failure: Throwable): UiText {
    val mode = settings.sourceMode
    val sessionEnded =
        when (mode) {
            SourceMode.JELLYFIN -> failure.toJellyfinError() == JellyfinError.BAD_CREDENTIALS
            SourceMode.SUBSONIC -> failure.toSubsonicError() == SubsonicError.BAD_CREDENTIALS
            SourceMode.LOCAL -> false
        }
    val server = settings.server
    return if (sessionEnded && server != null) {
        UiText(R.string.sources_session_ended, server.name)
    } else {
        UiText(mode.scanFailedMessage)
    }
}
