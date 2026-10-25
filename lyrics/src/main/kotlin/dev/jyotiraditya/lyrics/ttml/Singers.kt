package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.Voice

/** The singers a document declares, each numbered in the order it first appears. */
internal class Singers {
    private val groups = HashSet<String>()
    private val indices = LinkedHashMap<String, Int>()

    /** Registers a declared singer; a group is numbered only once a line uses it. */
    fun declare(id: String?, type: String?) {
        if (id == null) return
        if (type == TtmlNames.AGENT_TYPE_GROUP) {
            groups += id
        } else {
            indexOf(id)
        }
    }

    fun voiceOf(id: String?): Voice = if (id != null && id in groups) Voice.GROUP else Voice.PRIMARY

    fun indexOf(id: String?): Int = if (id == null) 0 else indices.getOrPut(id) { indices.size }
}
