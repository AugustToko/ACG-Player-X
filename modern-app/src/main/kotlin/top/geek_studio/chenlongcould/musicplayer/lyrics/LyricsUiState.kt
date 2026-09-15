package top.geek_studio.chenlongcould.musicplayer.lyrics

data class LyricsUiState(
    val mediaId: String? = null,
    val lines: List<LyricLine> = emptyList(),
    val fileOffsetMs: Long = 0L,
    val userOffsetMs: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val totalOffsetMs: Long
        get() = fileOffsetMs + userOffsetMs

    val hasLyrics: Boolean
        get() = lines.isNotEmpty()
}
