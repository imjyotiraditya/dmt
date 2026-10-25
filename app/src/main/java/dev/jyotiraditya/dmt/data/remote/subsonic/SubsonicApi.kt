package dev.jyotiraditya.dmt.data.remote.subsonic

import dev.jyotiraditya.dmt.domain.model.ServerSource
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ALBUM
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import dev.jyotiraditya.dmt.domain.model.UNKNOWN_TITLE
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.LyricsParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

private const val API_VERSION = "1.16.1"
private const val CLIENT_NAME = "dmt"
private const val PAGE_SIZE = 500
private const val SALT_BYTES = 8
private const val MS_PER_SECOND = 1_000L
private const val BITS_PER_KBIT = 1_000

/** A Subsonic server and the salted password token that signs in to it. */
data class SubsonicAccount(
    val url: String,
    val username: String,
    val salt: String,
    val token: String,
) {
    /** The sign-in data saved for a Subsonic source. */
    val secrets: Map<String, String>
        get() = mapOf("salt" to salt, "token" to token)

    companion object {
        fun create(url: String, username: String, password: String): SubsonicAccount {
            val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes).toHexString()
            return SubsonicAccount(url, username, salt, subsonicToken(password, salt))
        }
    }
}

/** The Subsonic sign-in token: `md5(password + salt)` in hex. */
internal fun subsonicToken(password: String, salt: String): String =
    MessageDigest.getInstance("MD5").digest((password + salt).toByteArray()).toHexString()

/** This source's Subsonic account, or null if it is another kind or signed out. */
fun ServerSource.subsonicAccount(): SubsonicAccount? {
    if (kind != SourceMode.SUBSONIC) return null
    return SubsonicAccount(
        url = url,
        username = username,
        salt = secrets["salt"] ?: return null,
        token = secrets["token"] ?: return null,
    )
}

data class SubsonicSong(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val trackNumber: Int,
    val discNumber: Int,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val coverArt: String?,
    val dateAdded: Long,
    val genre: String?,
    val bitDepth: Int?,
    val sampleRateHz: Int?,
)

/** Client for Subsonic and OpenSubsonic servers such as Navidrome. */
@Singleton
class SubsonicApi @Inject constructor(private val client: HttpClient, private val json: Json) {

    /** Checks that the server answers and accepts [account]; throws when it doesn't. */
    suspend fun ping(account: SubsonicAccount) {
        call(account, "ping")
    }

    suspend fun songs(account: SubsonicAccount): List<SubsonicSong> {
        val songs = mutableListOf<SubsonicSong>()
        do {
            val page =
                call(account, "search3") {
                    parameters.append("query", "")
                    parameters.append("songCount", "$PAGE_SIZE")
                    parameters.append("songOffset", "${songs.size}")
                    parameters.append("artistCount", "0")
                    parameters.append("albumCount", "0")
                }
                    .searchResult3
                    ?.song
                    .orEmpty()
            songs += page.map { it.toSong() }
        } while (page.size == PAGE_SIZE)
        return songs
    }

    fun streamUrl(account: SubsonicAccount, songId: String): String =
        url(account, "stream") { parameters.append("id", songId) }

    fun coverUrl(account: SubsonicAccount, coverArtId: String): String =
        url(account, "getCoverArt") { parameters.append("id", coverArtId) }

    /**
     * The server's lyrics for [song], from `getLyricsBySongId` or else `getLyrics`, or null when it
     * has none; throws if unreachable.
     */
    suspend fun lyrics(account: SubsonicAccount, song: SubsonicSongRef): Lyrics? {
        // Servers without the OpenSubsonic lyrics extension refuse this call.
        val structured = refusedAsNull {
            call(account, "getLyricsBySongId") { parameters.append("id", song.id) }
                .lyricsList
                ?.structuredLyrics
        }
            .orEmpty()
        val best = structured.firstOrNull { it.synced } ?: structured.firstOrNull()
        if (best != null && best.line.any { it.value.isNotBlank() }) {
            return LyricsParser.parse(best.asLrc())
        }

        val plain = refusedAsNull {
            call(account, "getLyrics") {
                parameters.append("artist", song.artist)
                parameters.append("title", song.title)
            }
                .lyrics
                ?.value
        }
        return plain?.takeIf { it.isNotBlank() }?.let(LyricsParser::parse)
    }

    /** The result of [block], or null when the server refuses the call or doesn't know it. */
    private inline fun <T> refusedAsNull(block: () -> T?): T? = try {
        block()
    } catch (_: SubsonicException) {
        null
    } catch (missing: SubsonicHttpException) {
        if (missing.status == HttpStatusCode.NotFound) null else throw missing
    }

    /** Calls [method] and returns its response; a failure the server reports throws. */
    private suspend fun call(
        account: SubsonicAccount,
        method: String,
        params: URLBuilder.() -> Unit = {},
    ): SubsonicResponse {
        val response = client.get(url(account, method, params)).decode()
        if (response.status != "ok") {
            val error = response.error
            throw SubsonicException(error?.code ?: 0, error?.message.orEmpty())
        }
        return response
    }

    private fun url(
        account: SubsonicAccount,
        method: String,
        params: URLBuilder.() -> Unit = {},
    ): String = URLBuilder("${account.url.trimEnd('/')}/rest/$method")
        .apply {
            parameters.append("u", account.username)
            parameters.append("t", account.token)
            parameters.append("s", account.salt)
            parameters.append("v", API_VERSION)
            parameters.append("c", CLIENT_NAME)
            parameters.append("f", "json")
            params()
        }
        .buildString()

    private suspend fun HttpResponse.decode(): SubsonicResponse {
        if (!status.isSuccess()) throw SubsonicHttpException(status)
        return json.decodeFromString<SubsonicEnvelope>(bodyAsText()).response
    }
}

data class SubsonicSongRef(val id: String, val artist: String, val title: String)

internal fun StructuredLyrics.asLrc(): String = line.joinToString("\n") { lyric ->
    val start = lyric.start
    if (synced && start != null) "[${start.lrcTime()}]${lyric.value}" else lyric.value
}

/** These milliseconds as an LRC `mm:ss.cc` time. */
internal fun Long.lrcTime(): String =
    "%02d:%02d.%02d".format(Locale.ROOT, this / 60_000, this / 1_000 % 60, this % 1_000 / 10)

internal fun SongDto.toSong(): SubsonicSong {
    val albumArtist = displayAlbumArtist.orEmpty()
    return SubsonicSong(
        id = id,
        title = title ?: UNKNOWN_TITLE,
        artist =
            (displayArtist ?: artist).orEmpty().ifBlank { albumArtist }.ifBlank { UNKNOWN_ARTIST },
        albumArtist = albumArtist,
        album = album ?: UNKNOWN_ALBUM,
        trackNumber = track ?: 0,
        discNumber = discNumber ?: 0,
        durationMs = (duration ?: 0L) * MS_PER_SECOND,
        mime = mime(),
        bitrate = (bitRate ?: 0) * BITS_PER_KBIT,
        size = size ?: 0L,
        coverArt = coverArt,
        dateAdded =
            created?.let { runCatching { Instant.parse(it).epochSeconds }.getOrNull() } ?: 0L,
        genre = genre,
        bitDepth = bitDepth?.takeIf { it > 0 },
        sampleRateHz = samplingRate?.takeIf { it > 0 },
    )
}

/** The song's MIME type, reporting an MP4 with a bit depth as ALAC. */
internal fun SongDto.mime(): String = when {
    suffix.equals("m4a", ignoreCase = true) && (bitDepth ?: 0) > 0 -> "audio/alac"
    !contentType.isNullOrBlank() -> contentType
    !suffix.isNullOrBlank() -> "audio/$suffix"
    else -> "audio/?"
}
