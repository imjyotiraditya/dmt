package dev.jyotiraditya.dmt.domain.model

fun List<Track>.toFolders(): List<Folder> =
    asSequence()
        .filter { it.path.isNotEmpty() }
        .groupBy { it.path.substringBeforeLast('/') }
        .map { (dir, tracks) ->
            Folder(
                name = dir.removePrefix("/storage/emulated/0/").ifEmpty { "/" },
                path = dir,
                tracks = tracks,
            )
        }
        .sortedBy { it.name.lowercase() }

private const val FOLDER_TREE_ROOT = "/storage/emulated/0"

/**
 * Builds a folder tree from indexed tracks by walking each track's absolute
 * path — no storage is scanned again. Runs in O(n) over tracks (times the
 * constant depth of a path), and only ever produces nodes for directories
 * that actually contain indexed music.
 */
fun List<Track>.toFolderTree(): List<FolderNode> {
    class Builder(val name: String, val absolutePath: String, val parentPath: String?) {
        val children = LinkedHashMap<String, Builder>()
        val songs = mutableListOf<Track>()
    }

    val root = Builder(name = "", absolutePath = FOLDER_TREE_ROOT, parentPath = null)

    for (track in this) {
        if (track.path.isEmpty()) continue
        val dir = track.path.substringBeforeLast('/')
        val relative = dir.removePrefix(FOLDER_TREE_ROOT).trim('/')
        if (relative.isEmpty()) continue

        var node = root
        var path = FOLDER_TREE_ROOT
        for (segment in relative.split('/')) {
            path = "$path/$segment"
            val parent = node
            node = node.children.getOrPut(segment) {
                Builder(
                    name = segment,
                    absolutePath = path,
                    parentPath = if (parent === root) null else parent.absolutePath,
                )
            }
        }
        node.songs += track
    }

    fun Builder.toNode(): FolderNode {
        val childNodes = children.values.map { it.toNode() }.sortedBy { it.name.lowercase() }
        return FolderNode(
            id = absolutePath,
            name = name,
            absolutePath = absolutePath,
            parentPath = parentPath,
            childFolderCount = childNodes.size,
            songCount = songs.size + childNodes.sumOf { it.songCount },
            artwork = songs.firstNotNullOfOrNull { it.coverUri }
                ?: childNodes.firstNotNullOfOrNull { it.artwork },
            lastModified = maxOf(
                songs.maxOfOrNull { it.dateModified } ?: 0L,
                childNodes.maxOfOrNull { it.lastModified } ?: 0L,
            ),
            children = childNodes,
            songs = songs.sortedBy { it.trackNumber },
        )
    }

    return root.children.values.map { it.toNode() }.sortedBy { it.name.lowercase() }
}

/** Depth-first flattening of a folder tree, used to search folders by name. */
fun List<FolderNode>.flatten(): List<FolderNode> =
    flatMap { listOf(it) + it.children.flatten() }

/** Finds a node anywhere in the tree by its [FolderNode.absolutePath]. */
fun List<FolderNode>.findNode(path: String): FolderNode? {
    for (node in this) {
        if (node.absolutePath == path) return node
        node.children.findNode(path)?.let { return it }
    }
    return null
}

/** All songs in this folder, including every descendant subfolder. */
fun FolderNode.allSongs(): List<Track> =
    songs + children.flatMap { it.allSongs() }

fun List<Track>.toArtists(): List<Artist> =
    groupBy { it.artist }
        .map { (name, tracks) ->
            Artist(
                name = name,
                albums = tracks.map { it.album }.distinct().size,
                tracks = tracks.sortedWith(
                    compareBy({ it.album.lowercase() }, { it.trackNumber }),
                ),
            )
        }
        .sortedBy { it.name.lowercase() }

fun List<Track>.toAlbums(): List<Album> =
    groupBy { it.album }
        .map { (name, tracks) ->
            val artists = tracks.map { it.artist }.distinct()
            Album(
                name = name,
                artist = artists.singleOrNull() ?: "various artists",
                tracks = tracks.sortedBy { it.trackNumber },
            )
        }
        .sortedBy { it.name.lowercase() }
