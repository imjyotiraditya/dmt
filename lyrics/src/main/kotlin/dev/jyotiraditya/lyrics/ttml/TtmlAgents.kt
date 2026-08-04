package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.Voice

/** The singers a document names, numbered in the order the document first names them. */
internal class Agents {

    private val types = mutableMapOf<String, String>()
    private val order = mutableListOf<String>()

    fun register(id: String?, type: String?) {
        if (id == null) return
        if (type != null) types[id] = type
        if (types[id] != AGENT_TYPE_GROUP && id !in order) order += id
    }

    fun voiceFor(agentId: String?): Voice {
        if (agentId == null) return Voice.PRIMARY
        return if (types[agentId] == AGENT_TYPE_GROUP) Voice.GROUP else Voice.PRIMARY
    }

    fun singerFor(agentId: String?): Int {
        if (agentId == null) return 0

        if (agentId !in order) order += agentId

        return order.indexOf(agentId)
    }
}
