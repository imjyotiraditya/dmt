package dev.jyotiraditya.metadata

object TagKey {
    const val TITLE = "TITLE"
    const val ARTIST = "ARTIST"
    const val ALBUM = "ALBUM"
    const val ALBUM_ARTIST = "ALBUMARTIST"
    const val DATE = "DATE"
    const val TRACK = "TRACKNUMBER"
    const val DISC = "DISCNUMBER"
    const val GENRE = "GENRE"
    const val COMPOSER = "COMPOSER"
    const val COMMENT = "COMMENT"
    const val LYRICS = "LYRICS"
    const val REPLAYGAIN_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
}

internal const val USLT = "USLT"
internal const val TXXX = "TXXX"

internal val ID3_KEYS = mapOf(
    "TIT2" to TagKey.TITLE,
    "TPE1" to TagKey.ARTIST,
    "TALB" to TagKey.ALBUM,
    "TPE2" to TagKey.ALBUM_ARTIST,
    "TDRC" to TagKey.DATE,
    "TYER" to TagKey.DATE,
    "TRCK" to TagKey.TRACK,
    "TPOS" to TagKey.DISC,
    "TCON" to TagKey.GENRE,
    "TCOM" to TagKey.COMPOSER,
)

internal fun id3FrameFor(key: String, version: Int): String? = when (key) {
    TagKey.LYRICS -> USLT
    TagKey.DATE -> if (version == 4) "TDRC" else "TYER"
    else -> ID3_KEYS.entries.firstOrNull { it.value == key && it.key != "TYER" }?.key
}

internal val MP4_KEYS = mapOf(
    "©nam" to TagKey.TITLE,
    "©ART" to TagKey.ARTIST,
    "©alb" to TagKey.ALBUM,
    "aART" to TagKey.ALBUM_ARTIST,
    "©day" to TagKey.DATE,
    "©gen" to TagKey.GENRE,
    "©wrt" to TagKey.COMPOSER,
    "©cmt" to TagKey.COMMENT,
    "©lyr" to TagKey.LYRICS,
)

internal val MP4_ATOMS: Map<String, String> =
    MP4_KEYS.entries.associate { (atom, key) -> key to atom }
