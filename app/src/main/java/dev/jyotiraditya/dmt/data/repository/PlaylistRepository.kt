package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.di.ApplicationScope
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val M3U_HEADER = "#EXTM3U"
private const val M3U_EXTENSION = "m3u8"
private val PATH_SEPARATORS = Regex("""[/\\]""")

/** Playlists saved as M3U8 files of track paths in the app's storage. */
@Singleton
class PlaylistRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    /** The saved playlists, holding the library tracks they list. */
    val playlists: StateFlow<List<Playlist>>
        field = MutableStateFlow<List<Playlist>>(emptyList())

    private val dir: File
        get() =
            File(context.getExternalFilesDir(null) ?: context.filesDir, "playlists").apply {
                mkdirs()
            }

    init {
        scope.launch { libraryRepository.library.collectLatest { reload() } }
    }

    suspend fun create(name: String) = edit {
        val file = fileFor(name) ?: return@edit
        if (!file.exists()) runCatching { file.writeText(M3U_HEADER + "\n") }
    }

    suspend fun delete(name: String) = edit { fileFor(name)?.delete() }

    suspend fun addTrack(name: String, track: Track) = edit {
        val file = fileFor(name) ?: return@edit
        if (!file.exists() || track.path.isEmpty() || track.path in entriesOf(file)) return@edit
        runCatching { file.appendText(track.path + "\n") }
    }

    suspend fun removeTrack(name: String, path: String) = edit {
        val file = fileFor(name)?.takeIf { it.exists() } ?: return@edit
        val kept = file.readLines().filterNot { it.trim() == path }
        runCatching { file.writeText(kept.joinToString("\n", postfix = "\n")) }
    }

    /** Runs [change] on the playlist files, then reads them again, even if the caller leaves. */
    private suspend fun edit(change: () -> Unit) {
        scope
            .launch {
                withContext(Dispatchers.IO) { change() }
                reload()
            }
            .join()
    }

    private suspend fun reload() {
        val byPath = libraryRepository.library.value.tracks.associateBy { it.path }
        playlists.value =
            withContext(Dispatchers.IO) {
                dir.listFiles { file -> file.extension == M3U_EXTENSION }
                    .orEmpty()
                    .sortedBy { it.nameWithoutExtension.lowercase() }
                    .map { file ->
                        Playlist(
                            name = file.nameWithoutExtension,
                            tracks = entriesOf(file).mapNotNull { byPath[it] },
                        )
                    }
            }
    }

    private fun entriesOf(file: File): List<String> = runCatching {
        file.readLines()
    }
        .getOrDefault(emptyList())
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }

    private fun fileFor(name: String): File? {
        val safe = name.trim().replace(PATH_SEPARATORS, "")
        if (safe.isEmpty()) return null

        return File(dir, "$safe.$M3U_EXTENSION")
    }
}
