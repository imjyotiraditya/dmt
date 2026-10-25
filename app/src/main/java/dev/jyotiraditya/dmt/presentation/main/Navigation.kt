package dev.jyotiraditya.dmt.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.library.BackLink
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.background
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface

/** The top-level destinations, with the label and icon they are shown with. */
enum class Place(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    LISTEN(R.string.nav_listen, DmtIcons.Home),
    LIBRARY(R.string.nav_library, DmtIcons.Library),
    SETTINGS(R.string.nav_settings, DmtIcons.Settings),
}

internal val BackRowHeight = 52.dp

/** The wordmark at the top level or a back link inside a page, with the destinations at the end. */
@Composable
internal fun TopBar(
    place: Place,
    back: BackLink?,
    onBack: () -> Unit,
    onPlace: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .background(Ground)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(60.dp)
                .padding(horizontal = Margin),
    ) {
        AnimatedContent(
            targetState = back?.label,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "barLead",
            modifier = Modifier.weight(1f),
        ) { label ->
            if (label == null) {
                Box(contentAlignment = Alignment.CenterStart) {
                    Wordmark(
                        modifier =
                            Modifier.pressable(
                                onClick = { onPlace(Place.LISTEN) },
                                label = "dmt",
                            ),
                    )
                }
            } else {
                BackLinkLabel(label = label, onBack = onBack)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Place.entries.forEach { p ->
                val color by animateColorAsState(if (p == place) Ink else Muted, label = "navWord")
                Text(
                    text = stringResource(p.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    modifier =
                        Modifier.pressable(
                            onClick = { onPlace(p) },
                            role = Role.Tab,
                            label = stringResource(p.label),
                        )
                            .padding(vertical = 14.dp),
                )
            }
        }
    }
}

/** The back link above a page in a wide window, where there is no top bar. */
@Composable
internal fun BackRow(label: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.fillMaxWidth().height(BackRowHeight).padding(horizontal = Margin),
    ) {
        BackLinkLabel(label = label, onBack = onBack)
    }
}

@Composable
private fun BackLinkLabel(label: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .offset(x = (-6).dp)
                .pressable(onClick = onBack, label = stringResource(R.string.nav_back))
                .padding(vertical = 12.dp),
    ) {
        DmtIcon(icon = DmtIcons.ChevronLeft, tint = Ink, size = 24.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The navigation rail for wide windows: the wordmark, then the destinations. */
@Composable
internal fun SideRail(place: Place, onPlace: (Place) -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            modifier
                .width(184.dp)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 12.dp, top = 8.dp),
    ) {
        Wordmark(
            modifier =
                Modifier.padding(start = 12.dp, bottom = 16.dp)
                    .pressable(
                        onClick = { onPlace(Place.LISTEN) },
                        label = stringResource(R.string.app_name),
                    ),
        )
        Place.entries.forEach { p ->
            val on = p == place
            val fill by
                animateColorAsState(
                    if (on) Surface else Color.Transparent,
                    label = "railFill",
                )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .pressable(
                            onClick = { onPlace(p) },
                            role = Role.Tab,
                            label = stringResource(p.label),
                        )
                        .background { fill }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                DmtIcon(icon = p.icon, tint = if (on) Ink else Muted, size = 22.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(p.label),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (on) Ink else Muted,
                )
            }
        }
    }
}

/** The lowercase wordmark, scaled by [size]. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, color: Color = Ink, size: Float = 1f) {
    Text(
        text = stringResource(R.string.app_name),
        style =
            MaterialTheme.typography.headlineMedium.copy(
                fontSize = MaterialTheme.typography.headlineMedium.fontSize * size,
                letterSpacing = (-0.06).em,
            ),
        color = color,
        modifier = modifier,
    )
}
