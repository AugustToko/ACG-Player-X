package top.geek_studio.chenlongcould.musicplayer.ui

import android.app.Application
import android.net.Uri
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.geek_studio.chenlongcould.musicplayer.data.AuthorizedFolder
import top.geek_studio.chenlongcould.musicplayer.data.LibraryStateRepository
import top.geek_studio.chenlongcould.musicplayer.data.MusicRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsExportFormat
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportDocument
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportMode
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsImportRepository
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStatisticsSnapshot
import top.geek_studio.chenlongcould.musicplayer.data.PlaybackStats
import top.geek_studio.chenlongcould.musicplayer.data.encodePlaybackStatistics
import top.geek_studio.chenlongcould.musicplayer.model.Song

/** Injectable I/O boundary; production keeps the existing repositories and privacy transactions. */
internal interface StatisticsOperations {
    suspend fun loadMetadata(includeMediaStore: Boolean, folders: List<AuthorizedFolder>): List<Song>
    suspend fun readImport(uri: Uri): PlaybackStatisticsImportDocument
    suspend fun importStatistics(stats: Map<String, PlaybackStats>, mode: PlaybackStatisticsImportMode): Int
    suspend fun export(uri: Uri, format: PlaybackStatisticsExportFormat, snapshot: PlaybackStatisticsSnapshot)
    suspend fun clearListeningData(clearRecent: Boolean, clearPlaybackStats: Boolean)
}

internal class RepositoryStatisticsOperations(application: Application) : StatisticsOperations {
    private val libraryStateRepository = LibraryStateRepository(application)
    private val musicRepository = MusicRepository(application)
    private val importRepository = PlaybackStatisticsImportRepository(application)
    private val contentResolver = application.contentResolver

    override suspend fun loadMetadata(includeMediaStore: Boolean, folders: List<AuthorizedFolder>): List<Song> =
        if (!includeMediaStore && folders.none(AuthorizedFolder::isAvailable)) {
            emptyList()
        } else {
            musicRepository.loadSongs(
                includeMediaStore = includeMediaStore,
                authorizedFolders = folders,
                refreshAuthorizedFolders = false,
            ).songs
        }

    override suspend fun readImport(uri: Uri): PlaybackStatisticsImportDocument = importRepository.read(uri)

    override suspend fun importStatistics(stats: Map<String, PlaybackStats>, mode: PlaybackStatisticsImportMode): Int =
        libraryStateRepository.importPlaybackStatistics(imported = stats, mode = mode)

    override suspend fun export(uri: Uri, format: PlaybackStatisticsExportFormat, snapshot: PlaybackStatisticsSnapshot) {
        withContext(Dispatchers.IO) {
            val output = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("无法打开导出文件")
            output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.write(encodePlaybackStatistics(snapshot, format))
            }
        }
    }

    override suspend fun clearListeningData(clearRecent: Boolean, clearPlaybackStats: Boolean) {
        libraryStateRepository.clearListeningData(clearRecent = clearRecent, clearPlaybackStats = clearPlaybackStats)
    }
}
