package top.geek_studio.chenlongcould.musicplayer.ui

import top.geek_studio.chenlongcould.musicplayer.model.Song

fun filterSongs(
    songs: List<Song>,
    query: String,
): List<Song> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return songs

    return songs.filter { song ->
        song.title.contains(normalized, ignoreCase = true) ||
            song.artist.contains(normalized, ignoreCase = true) ||
            song.album.contains(normalized, ignoreCase = true) ||
            song.folderName.contains(normalized, ignoreCase = true) ||
            song.folderPath.contains(normalized, ignoreCase = true)
    }
}
