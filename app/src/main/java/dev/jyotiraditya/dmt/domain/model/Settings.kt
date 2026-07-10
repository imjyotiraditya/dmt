package dev.jyotiraditya.dmt.domain.model

enum class Accent(val label: String) {
    ORANGE("orange"),
    MOSS("moss"),
    STEEL("steel"),
    MONO("mono"),
    ;

    fun next(): Accent = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromOrdinal(value: Int): Accent = entries[value.mod(entries.size)]
    }
}

enum class SourceMode(val label: String) {
    LOCAL("local"),
    JELLYFIN("jellyfin"),
}

data class DmtSettings(
    val wave: Boolean = true,
    val cols: Int = 64,
    val listSpecs: Boolean = true,
    val romanizedLyrics: Boolean = false,
    val accent: Accent = Accent.ORANGE,
    val rawArt: Boolean = false,
    val sourceMode: SourceMode = SourceMode.LOCAL,
    val jellyfinUrl: String? = null,
    val jellyfinUserId: String? = null,
    val jellyfinToken: String? = null,
)

data class LastSession(
    val queueIds: List<Long>,
    val index: Int,
    val positionMs: Long,
)
