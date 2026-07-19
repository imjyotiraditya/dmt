package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.Track
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val M3U_HEADER = "#EXTM3U"
private const val M3U_EXTENSION = ".m3u8"

@Singleton
class PlaylistRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val dir: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "playlists")
            .apply { mkdirs() }

    fun load(tracks: List<Track>): List<Playlist> {
        val byPath = tracks.associateBy { it.path }

        return dir.listFiles { file -> file.extension == "m3u8" }
            .orEmpty()
            .sortedBy { it.nameWithoutExtension.lowercase() }
            .map { file ->
                Playlist(
                    name = file.nameWithoutExtension,
                    tracks = entriesOf(file).mapNotNull { byPath[it] },
                )
            }
    }

    fun create(name: String): Boolean {
        val file = fileFor(name) ?: return false
        if (file.exists()) return false

        return runCatching { file.writeText(M3U_HEADER + "\n") }.isSuccess
    }

    fun delete(name: String) {
        fileFor(name)?.delete()
    }

    fun addTrack(name: String, track: Track): Boolean {
        val file = fileFor(name) ?: return false
        if (!file.exists() || track.path.isEmpty()) return false
        if (track.path in entriesOf(file)) return false

        return runCatching { file.appendText(track.path + "\n") }.isSuccess
    }

    fun removeTrack(name: String, path: String) {
        val file = fileFor(name) ?: return
        if (!file.exists()) return

        val kept = file.readLines().filterNot { it.trim() == path }
        runCatching { file.writeText(kept.joinToString("\n", postfix = "\n")) }
    }

    private fun entriesOf(file: File): List<String> =
        runCatching { file.readLines() }
            .getOrDefault(emptyList())
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

    private fun fileFor(name: String): File? {
        val safe = name.trim().replace(Regex("""[/\\]"""), "")
        if (safe.isEmpty()) return null

        return File(dir, safe + M3U_EXTENSION)
    }
}