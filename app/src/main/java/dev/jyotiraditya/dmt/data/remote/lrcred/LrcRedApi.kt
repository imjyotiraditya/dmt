package dev.jyotiraditya.dmt.data.remote.lrcred

import dev.jyotiraditya.dmt.data.remote.DMT_USER_AGENT
import dev.jyotiraditya.dmt.data.remote.found
import dev.jyotiraditya.dmt.data.remote.knownOrNull
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ALBUM
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_TITLE
import dev.jyotiraditya.dmt.domain.model.primaryArtist
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

private const val BASE_URL = "https://lrc.red"

/** How far a hit's length may differ from the track's for it to be the same recording. */
private val DURATION_TOLERANCE_MS = 3.seconds.inWholeMilliseconds

/** A bracketed version or guest note in a title, such as `(feat. X)` or `[Live]`. */
private val TITLE_DECORATION = Regex("""\s*[(\[][^)\]]*[)\]]""")

/** The marks left behind once accented letters are decomposed. */
private val COMBINING_MARKS = Regex("""\p{Mn}+""")

@Serializable private data class LrcRedSearch(val hits: List<LrcRedHit> = emptyList())

@Serializable
internal data class LrcRedHit(
    val isrc: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Double? = null,
)

/** Client for the lrc.red lyrics catalogue, which serves word-synced TTML by ISRC. */
@Singleton
class LrcRedApi @Inject constructor(private val client: HttpClient, private val json: Json) {

    /** The TTML lyrics of [track], or null when lrc.red has no matching recording. */
    suspend fun fetchLyrics(track: Track): String? {
        val title = track.title.knownOrNull(UNKNOWN_TITLE) ?: return null
        val artist = track.primaryArtist.knownOrNull(UNKNOWN_ARTIST) ?: return null

        val search =
            client.get("$BASE_URL/search.json") {
                parameter("q", "$artist $title")
                header(HttpHeaders.UserAgent, DMT_USER_AGENT)
            }
        if (!search.found()) return null
        val hits = json.decodeFromString<LrcRedSearch>(search.bodyAsText()).hits
        val album = track.album.knownOrNull(UNKNOWN_ALBUM)
        val hit = bestMatch(hits, title, artist, track.durationMs, album) ?: return null

        val lyrics =
            client.get("$BASE_URL/s/${hit.isrc}.ttml") {
                header(HttpHeaders.UserAgent, DMT_USER_AGENT)
            }
        if (!lyrics.found()) return null
        return lyrics.bodyAsText().ifBlank { null }
    }
}

/**
 * The hit that is the same recording: title and artist agree once case, accents and version notes
 * are set aside, and the length is close. One from the same [album] wins, then the closest length.
 */
internal fun bestMatch(
    hits: List<LrcRedHit>,
    title: String,
    artist: String,
    durationMs: Long,
    album: String? = null,
): LrcRedHit? {
    val wantedTitle = title.comparable(stripDecoration = true)
    val wantedArtist = artist.comparable()
    return hits
        .filter { it.title.comparable(stripDecoration = true) == wantedTitle }
        .filter { hit ->
            val hitArtist = hit.artist.comparable()
            hitArtist.contains(wantedArtist) || wantedArtist.contains(hitArtist)
        }
        .mapNotNull { hit ->
            val hitMs = hit.duration?.let { (it * 1000).toLong() }
            val difference = if (hitMs == null || durationMs <= 0) 0L else abs(hitMs - durationMs)
            (hit to difference).takeIf { difference <= DURATION_TOLERANCE_MS }
        }
        .minWithOrNull(
            compareBy<Pair<LrcRedHit, Long>> { (hit, _) -> !hit.album.isSameAlbumAs(album) }
                .thenBy { (_, difference) -> difference },
        )
        ?.first
}

private fun String.isSameAlbumAs(other: String?): Boolean =
    other != null && comparable(stripDecoration = true) == other.comparable(stripDecoration = true)

/** This text as lower-case letters and digits only, for comparing names written differently. */
private fun String.comparable(stripDecoration: Boolean = false): String {
    val base = if (stripDecoration) replace(TITLE_DECORATION, "") else this
    return Normalizer.normalize(base, Normalizer.Form.NFKD)
        .replace(COMBINING_MARKS, "")
        .lowercase()
        .filter(Char::isLetterOrDigit)
}
