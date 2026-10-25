package dev.jyotiraditya.dmt.presentation.settings

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val APACHE_2 = "Apache-2.0"
private const val SEPARATOR = " · "

/** Something the app is built on or ships, and the license it comes under. */
private data class LicensedWork(
    val key: String,
    val name: String,
    val detail: String,
    val licenseName: String,
    val licenseText: String,
)

private data class LicensedWorks(val bundled: List<LicensedWork>, val libraries: List<LicensedWork>)

/** The files the app bundles, then every library it is built on; tap one for its license text. */
@Composable
internal fun LicensesPage() {
    val context = LocalContext.current
    val works by
        produceState<LicensedWorks?>(initialValue = null) {
            value = context.licensedWorks()
        }
    var open by remember { mutableStateOf<LicensedWork?>(null) }

    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item {
            PageHead(
                title = stringResource(R.string.licenses_title),
                modifier = Modifier.padding(horizontal = Margin),
            )
            Text(
                text = stringResource(R.string.licenses_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
                modifier = Modifier.padding(start = Margin, end = Margin, bottom = 12.dp),
            )
        }
        works?.let { (bundled, libraries) ->
            item { GroupHeading(stringResource(R.string.licenses_bundled)) }
            items(bundled, key = { it.key }) { work -> WorkRow(work) { open = work } }
            item { GroupHeading(stringResource(R.string.licenses_libraries)) }
            items(libraries, key = { it.key }) { work -> WorkRow(work) { open = work } }
        }
    }

    open?.let { work -> LicenseSheet(work, onDismiss = { open = null }) }
}

@Composable
private fun WorkRow(work: LicensedWork, onClick: () -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .pressable(onClick = onClick, sink = 0.985f, label = work.name)
                .padding(horizontal = Margin, vertical = 12.dp),
    ) {
        Text(
            text = work.name,
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = work.detail,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LicenseSheet(work: LicensedWork, onDismiss: () -> Unit) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = work.name, meta = work.licenseName)
        Text(
            text = work.licenseText,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier =
                Modifier.verticalScroll(rememberScrollState())
                    .padding(start = Margin, end = Margin, bottom = 24.dp),
        )
    }
}

/** The bundled works and the library list generated at build time. */
private suspend fun Context.licensedWorks(): LicensedWorks = withContext(Dispatchers.IO) {
    val libs = Libs.Builder().withJson(rawText(R.raw.aboutlibraries)).build()
    val apache = libs.licenses.firstOrNull { it.spdxId == APACHE_2 }
    val bundled =
        listOf(
            LicensedWork(
                key = "bundled:material-symbols",
                name = getString(R.string.licenses_material_symbols),
                detail = getString(R.string.licenses_material_symbols_detail),
                licenseName = apache?.name.orEmpty(),
                licenseText = apache?.licenseContent.orEmpty(),
            ),
            LicensedWork(
                key = "bundled:ffmpeg",
                name = getString(R.string.licenses_ffmpeg),
                detail = getString(R.string.licenses_ffmpeg_detail),
                licenseName = getString(R.string.licenses_gpl3),
                licenseText = rawText(R.raw.license_gpl3),
            ),
            LicensedWork(
                key = "bundled:plus-jakarta-sans",
                name = getString(R.string.licenses_plus_jakarta_sans),
                detail = getString(R.string.licenses_plus_jakarta_sans_detail),
                licenseName = getString(R.string.licenses_ofl),
                licenseText = rawText(R.raw.license_plus_jakarta_sans),
            ),
            LicensedWork(
                key = "bundled:jellyfin-icon",
                name = getString(R.string.licenses_jellyfin_icon),
                detail = getString(R.string.licenses_jellyfin_icon_detail),
                licenseName = getString(R.string.licenses_cc_by_sa_4),
                licenseText = rawText(R.raw.license_cc_by_sa_4),
            ),
            LicensedWork(
                key = "bundled:navidrome-logo",
                name = getString(R.string.licenses_navidrome_logo),
                detail = getString(R.string.licenses_navidrome_logo_detail),
                licenseName = getString(R.string.licenses_gpl3),
                licenseText = rawText(R.raw.license_gpl3),
            ),
        )
    val libraries =
        libs.libraries
            .sortedBy { it.name.lowercase() }
            .map { library ->
                val licenseNames = library.licenses.joinToString(SEPARATOR) { it.name }
                LicensedWork(
                    key = library.uniqueId,
                    name = library.displayName(),
                    detail =
                        listOfNotNull(library.artifactVersion, licenseNames.ifEmpty { null })
                            .joinToString(SEPARATOR),
                    licenseName = licenseNames,
                    licenseText =
                        library.licenses.joinToString("\n\n") {
                            it.licenseContent ?: it.url.orEmpty()
                        },
                )
            }
    LicensedWorks(bundled, libraries)
}

/** The library's name, or its artifact id when it declares no name of its own. */
private fun Library.displayName(): String =
    name.takeUnless { it.isBlank() || it == uniqueId } ?: uniqueId.substringAfterLast(':')

private fun Context.rawText(@RawRes id: Int): String =
    resources.openRawResource(id).bufferedReader().use { it.readText() }
