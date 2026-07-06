package dev.jyotiraditya.dmt.domain.model

enum class Accent(val label: String, val launcherAlias: String) {
    ORANGE("orange", "LauncherOrange"),
    MOSS("moss", "LauncherMoss"),
    STEEL("steel", "LauncherSteel"),
    MONO("mono", "LauncherMono"),
    ;

    fun next(): Accent = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromOrdinal(value: Int): Accent = entries[value.mod(entries.size)]
    }
}

data class DmtSettings(
    val wave: Boolean = true,
    val cols: Int = 64,
    val listSpecs: Boolean = true,
    val accent: Accent = Accent.ORANGE,
    val rawArt: Boolean = false,
)

data class LastSession(
    val queueIds: List<Long>,
    val index: Int,
    val positionMs: Long,
)
