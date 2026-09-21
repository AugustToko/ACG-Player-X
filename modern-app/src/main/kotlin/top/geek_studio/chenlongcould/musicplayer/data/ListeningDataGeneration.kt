package top.geek_studio.chenlongcould.musicplayer.data

/** Opaque, persisted generations. Capture before sampling; compare inside the write transaction. */
data class ListeningDataGeneration(
    val statistics: String = "",
    val recent: String = "",
) {
    fun acceptsStatistics(expected: ListeningDataGeneration?): Boolean =
        expected == null || statistics == expected.statistics

    fun acceptsRecent(expected: ListeningDataGeneration?): Boolean =
        expected == null || recent == expected.recent
}
