package dev.jyotiraditya.dmt.data.source.local.lyrics

private val WORD_TIMED_TAG = Regex("""<\d+:\d{1,2}""")
private val LINE_TIMED_TAG = Regex("""\[\d+:\d{1,2}""")
private val ENRICHED_TAG = Regex("""\[bg:|]\s*v\d+:""", RegexOption.IGNORE_CASE)

private fun isLyricsKey(key: String): Boolean =
    key == "LYRICS" ||
            key == "UNSYNCEDLYRICS" ||
            key == "UNSYNCED LYRICS" ||
            key == "ELRC" ||
            key == "LRC" ||
            key.startsWith("LYRICS-")

private fun contentRank(text: String): Int =
    when {
        text.startsWith("<") && text.contains("<tt") -> 4
        WORD_TIMED_TAG.containsMatchIn(text) && ENRICHED_TAG.containsMatchIn(text) -> 3
        WORD_TIMED_TAG.containsMatchIn(text) -> 2
        LINE_TIMED_TAG.containsMatchIn(text) -> 1
        else -> 0
    }

object LyricsTags {

    fun bestOf(tags: Map<String, List<String>>): String? =
        tags
            .filterKeys(::isLyricsKey)
            .values
            .flatten()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .maxByOrNull(::contentRank)
}
