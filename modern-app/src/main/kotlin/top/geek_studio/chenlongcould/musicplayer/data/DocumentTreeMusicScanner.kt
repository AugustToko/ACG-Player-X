package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Locale
import top.geek_studio.chenlongcould.musicplayer.model.Song

data class DocumentTreeScanResult(
    val songs: List<Song>,
    val warnings: List<String> = emptyList(),
)

class DocumentTreeMusicScanner(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    fun scan(folder: AuthorizedFolder): DocumentTreeScanResult {
        if (!folder.isAvailable) {
            return DocumentTreeScanResult(
                songs = emptyList(),
                warnings = listOf("${folder.displayName} 的目录授权已失效，请移除后重新授权"),
            )
        }

        return runCatching {
            scanTree(folder)
        }.getOrElse { throwable ->
            DocumentTreeScanResult(
                songs = emptyList(),
                warnings =
                    listOf(
                        "无法读取 ${folder.displayName}：" +
                            (throwable.localizedMessage ?: "文档提供程序返回错误"),
                    ),
            )
        }
    }

    private fun scanTree(folder: AuthorizedFolder): DocumentTreeScanResult {
        val treeUri = folder.uri
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootName = normalizePathSegment(folder.displayName, "授权音乐目录")
        val queue = ArrayDeque<PendingDirectory>()
        val visitedDirectories = hashSetOf<String>()
        val songs = mutableListOf<Song>()
        val warnings = mutableListOf<String>()
        var inspectedEntries = 0
        var entryLimitReached = false
        var depthLimitReached = false

        queue.add(
            PendingDirectory(
                documentId = rootDocumentId,
                relativePath = "",
                depth = 0,
            ),
        )

        while (queue.isNotEmpty() && !entryLimitReached) {
            val directory = queue.removeFirst()
            if (!visitedDirectories.add(directory.documentId)) continue

            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    directory.documentId,
                )

            resolver.query(
                childrenUri,
                DOCUMENT_PROJECTION,
                null,
                null,
                null,
            )?.use { cursor ->
                val documentIdIndex =
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val displayNameIndex =
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeTypeIndex =
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val lastModifiedIndex =
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (documentIdIndex < 0 || displayNameIndex < 0 || mimeTypeIndex < 0) {
                    warnings += "${folder.displayName} 的文档提供程序缺少必要字段"
                    return@use
                }

                while (cursor.moveToNext()) {
                    inspectedEntries += 1
                    if (inspectedEntries > MAX_DOCUMENT_ENTRIES) {
                        entryLimitReached = true
                        break
                    }

                    val documentId = cursor.getString(documentIdIndex) ?: continue
                    val displayName =
                        normalizePathSegment(
                            cursor.getString(displayNameIndex),
                            "未命名文件",
                        )
                    val mimeType = cursor.getString(mimeTypeIndex).orEmpty()
                    val lastModifiedMs =
                        if (lastModifiedIndex >= 0) {
                            cursor.getLong(lastModifiedIndex).coerceAtLeast(0L)
                        } else {
                            0L
                        }

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (directory.depth >= MAX_DIRECTORY_DEPTH) {
                            depthLimitReached = true
                        } else {
                            queue.add(
                                PendingDirectory(
                                    documentId = documentId,
                                    relativePath =
                                        appendPath(
                                            directory.relativePath,
                                            displayName,
                                        ),
                                    depth = directory.depth + 1,
                                ),
                            )
                        }
                        continue
                    }

                    if (!isSupportedAudioDocument(displayName, mimeType)) continue

                    val documentUri =
                        DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            documentId,
                        )
                    val displayFolderPath =
                        appendPath(rootName, directory.relativePath)
                    val folderName =
                        directory.relativePath
                            .substringAfterLast('/')
                            .ifBlank { rootName }
                    val metadata =
                        readAudioMetadata(
                            uri = documentUri,
                            fallbackTitle = displayName.substringBeforeLast('.', displayName),
                        )

                    songs +=
                        Song(
                            id = stableDocumentSongId(documentUri.toString()),
                            title = metadata.title,
                            artist = metadata.artist,
                            album = metadata.album,
                            durationMs = metadata.durationMs,
                            contentUri = documentUri.toString(),
                            albumArtUri = null,
                            folderName = folderName,
                            folderPath =
                                encodeAuthorizedFolderPath(
                                    treeUriString = treeUri.toString(),
                                    displayPath = displayFolderPath,
                                ),
                            dateAddedMs = lastModifiedMs,
                        )
                }
            }
        }

        if (entryLimitReached) {
            warnings +=
                "${folder.displayName} 超过 $MAX_DOCUMENT_ENTRIES 个目录项，本次只加载前半部分"
        }
        if (depthLimitReached) {
            warnings +=
                "${folder.displayName} 存在超过 $MAX_DIRECTORY_DEPTH 层的目录，本次已跳过过深内容"
        }

        return DocumentTreeScanResult(
            songs = songs,
            warnings = warnings.distinct(),
        )
    }

    private fun readAudioMetadata(
        uri: Uri,
        fallbackTitle: String,
    ): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            AudioMetadata(
                title =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        .cleanMetadata()
                        ?: fallbackTitle.ifBlank { "未知曲目" },
                artist =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        .cleanMetadata()
                        ?: retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                            .cleanMetadata()
                        ?: "未知艺术家",
                album =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        .cleanMetadata()
                        ?: "未知专辑",
                durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.coerceAtLeast(0L)
                        ?: 0L,
            )
        } catch (_: Throwable) {
            AudioMetadata(
                title = fallbackTitle.ifBlank { "未知曲目" },
                artist = "未知艺术家",
                album = "未知专辑",
                durationMs = 0L,
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun String?.cleanMetadata(): String? =
        this
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it != "<unknown>" }

    private data class PendingDirectory(
        val documentId: String,
        val relativePath: String,
        val depth: Int,
    )

    private data class AudioMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
    )

    private companion object {
        const val MAX_DOCUMENT_ENTRIES = 20_000
        const val MAX_DIRECTORY_DEPTH = 32

        val DOCUMENT_PROJECTION =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )
    }
}

internal fun isSupportedAudioDocument(
    displayName: String,
    mimeType: String,
): Boolean {
    if (mimeType.startsWith("audio/", ignoreCase = true)) return true
    if (mimeType.equals("application/ogg", ignoreCase = true)) return true

    val extension =
        displayName
            .substringAfterLast('.', "")
            .lowercase(Locale.ROOT)
    return extension in SUPPORTED_AUDIO_EXTENSIONS
}

internal fun stableDocumentSongId(uriString: String): Long {
    val digest =
        MessageDigest.getInstance("SHA-256")
            .digest(uriString.toByteArray(Charsets.UTF_8))
    var value = 0L
    for (index in 0 until Long.SIZE_BYTES) {
        value = (value shl 8) or (digest[index].toLong() and 0xffL)
    }
    val positive = (value and Long.MAX_VALUE).takeIf { it != 0L } ?: 1L
    return -positive
}

internal fun encodeAuthorizedFolderPath(
    treeUriString: String,
    displayPath: String,
): String =
    "$AUTHORIZED_FOLDER_PATH_PREFIX${shortStableHash(treeUriString)}:${displayPath.trim('/')}"

fun isAuthorizedFolderPath(path: String): Boolean =
    path.startsWith(AUTHORIZED_FOLDER_PATH_PREFIX)

fun displayMusicFolderPath(path: String): String =
    if (isAuthorizedFolderPath(path)) {
        path.substringAfter(':').substringAfter(':')
    } else {
        path
    }

private fun shortStableHash(value: String): String {
    val digest =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
    return digest
        .take(6)
        .joinToString(separator = "") { byte ->
            String.format(Locale.ROOT, "%02x", byte.toInt() and 0xff)
        }
}

private fun appendPath(
    parent: String,
    child: String,
): String =
    listOf(parent.trim('/'), child.trim('/'))
        .filter(String::isNotBlank)
        .joinToString("/")

private fun normalizePathSegment(
    value: String?,
    fallback: String,
): String =
    value
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.replace('/', '／')
        ?: fallback

private const val AUTHORIZED_FOLDER_PATH_PREFIX = "saf:"

private val SUPPORTED_AUDIO_EXTENSIONS =
    setOf(
        "aac",
        "alac",
        "amr",
        "ape",
        "flac",
        "m4a",
        "mid",
        "midi",
        "mp3",
        "oga",
        "ogg",
        "opus",
        "wav",
        "wma",
    )
