package dev.jyotiraditya.dmt.presentation.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.source.SourceDisplay
import dev.jyotiraditya.dmt.core.source.SourceFailure

/** How each kind of source is shown, provided at the root of the UI. */
val LocalSourceDisplays = staticCompositionLocalOf<Map<SourceMode, SourceDisplay>> { emptyMap() }

/** How this kind of source is shown. */
internal val SourceMode.display: SourceDisplay
    @Composable get() = LocalSourceDisplays.current.getValue(this)

/** The message for a sign-in to a [display] kind of source that failed this way. */
@StringRes
internal fun SourceFailure.loginMessage(display: SourceDisplay): Int = when (this) {
    SourceFailure.UNREACHABLE -> R.string.source_form_error_unreachable
    SourceFailure.BAD_CREDENTIALS -> R.string.source_form_error_rejected
    SourceFailure.SIGN_IN_UNSUPPORTED -> R.string.source_form_error_token_unsupported
    SourceFailure.WRONG_KIND -> display.wrongKind ?: R.string.source_form_error_unreachable
}
