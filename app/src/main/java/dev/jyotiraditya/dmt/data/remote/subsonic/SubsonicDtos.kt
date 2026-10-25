package dev.jyotiraditya.dmt.data.remote.subsonic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubsonicEnvelope(
    @SerialName("subsonic-response") val response: SubsonicResponse,
)

@Serializable
internal data class SubsonicResponse(
    val status: String,
    val error: SubsonicErrorDto? = null,
    val searchResult3: SearchResult3? = null,
    val lyricsList: LyricsList? = null,
    val lyrics: PlainLyrics? = null,
)

@Serializable internal data class SubsonicErrorDto(val code: Int, val message: String = "")

@Serializable internal data class SearchResult3(val song: List<SongDto> = emptyList())

@Serializable
internal data class SongDto(
    val id: String,
    val title: String? = null,
    val artist: String? = null,
    val displayArtist: String? = null,
    val displayAlbumArtist: String? = null,
    val album: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    /** Length in seconds. */
    val duration: Long? = null,
    /** Bitrate in kbit/s. */
    val bitRate: Int? = null,
    val size: Long? = null,
    val suffix: String? = null,
    val contentType: String? = null,
    val coverArt: String? = null,
    val created: String? = null,
    val genre: String? = null,
    val bitDepth: Int? = null,
    val samplingRate: Int? = null,
)

@Serializable
internal data class LyricsList(val structuredLyrics: List<StructuredLyrics> = emptyList())

@Serializable
internal data class StructuredLyrics(
    val synced: Boolean = false,
    val line: List<LyricLineDto> = emptyList(),
)

/** One lyric line; [start] is in milliseconds and absent for unsynced lyrics. */
@Serializable internal data class LyricLineDto(val start: Long? = null, val value: String = "")

@Serializable internal data class PlainLyrics(val value: String? = null)
