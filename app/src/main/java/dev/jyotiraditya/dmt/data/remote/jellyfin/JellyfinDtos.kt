package dev.jyotiraditya.dmt.data.remote.jellyfin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthenticateRequest(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val password: String,
)

@Serializable
internal data class AuthenticationResult(
    @SerialName("User") val user: UserDto,
    @SerialName("AccessToken") val accessToken: String,
)

@Serializable
internal data class UserDto(@SerialName("Id") val id: String, @SerialName("Name") val name: String)

@Serializable
internal data class ItemsResult(@SerialName("Items") val items: List<ItemDto> = emptyList())

@Serializable
internal data class ItemDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String? = null,
    @SerialName("Artists") val artists: List<String> = emptyList(),
    @SerialName("AlbumArtist") val albumArtist: String? = null,
    @SerialName("Album") val album: String? = null,
    @SerialName("AlbumId") val albumId: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSourceDto> = emptyList(),
    @SerialName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
    @SerialName("DateCreated") val dateCreated: String? = null,
)

@Serializable
internal data class MediaSourceDto(
    @SerialName("Bitrate") val bitrate: Int? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("MediaStreams") val streams: List<MediaStreamDto> = emptyList(),
)

@Serializable
internal data class MediaStreamDto(
    @SerialName("Type") val type: String? = null,
    @SerialName("Codec") val codec: String? = null,
    @SerialName("BitDepth") val bitDepth: Int? = null,
    @SerialName("SampleRate") val sampleRate: Int? = null,
)

@Serializable
internal data class LyricsResult(@SerialName("Lyrics") val lines: List<LyricLineDto> = emptyList())

/** One lyric line; [start] is in ticks and absent for unsynced lyrics. */
@Serializable
internal data class LyricLineDto(
    @SerialName("Text") val text: String = "",
    @SerialName("Start") val start: Long? = null,
)
