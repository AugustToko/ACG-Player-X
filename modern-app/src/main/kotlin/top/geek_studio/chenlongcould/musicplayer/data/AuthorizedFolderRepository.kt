package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AuthorizedFolder(
    val uriString: String,
    val displayName: String,
    val isAvailable: Boolean,
) {
    val uri: Uri
        get() = Uri.parse(uriString)
}

private val Context.authorizedFoldersDataStore by
    preferencesDataStore(name = "authorized_music_folders")

class AuthorizedFolderRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    val folders: Flow<List<AuthorizedFolder>> =
        appContext.authorizedFoldersDataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                val persistedReadUris =
                    resolver.persistedUriPermissions
                        .asSequence()
                        .filter { it.isReadPermission }
                        .map { it.uri.toString() }
                        .toSet()

                preferences[FOLDER_URIS]
                    .orEmpty()
                    .map { uriString ->
                        val uri = Uri.parse(uriString)
                        AuthorizedFolder(
                            uriString = uriString,
                            displayName = resolveDisplayName(uri),
                            isAvailable = uriString in persistedReadUris,
                        )
                    }
                    .sortedBy { it.displayName.lowercase() }
            }
            .flowOn(Dispatchers.IO)

    suspend fun addFolder(uri: Uri): AuthorizedFolder = withContext(Dispatchers.IO) {
        require(uri.scheme == ContentScheme) {
            "所选目录不是可持久化的文档目录"
        }
        require(isDocumentTreeUri(uri)) {
            "请选择一个目录，而不是单个文件"
        }

        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (securityException: SecurityException) {
            throw IllegalStateException(
                "目录读取授权无法持久化，请重新选择支持 SAF 的目录",
                securityException,
            )
        }

        appContext.authorizedFoldersDataStore.edit { preferences ->
            preferences[FOLDER_URIS] =
                preferences[FOLDER_URIS]
                    .orEmpty()
                    .plus(uri.toString())
        }

        AuthorizedFolder(
            uriString = uri.toString(),
            displayName = resolveDisplayName(uri),
            isAvailable = true,
        )
    }

    suspend fun removeFolder(uriString: String) = withContext(Dispatchers.IO) {
        appContext.authorizedFoldersDataStore.edit { preferences ->
            preferences[FOLDER_URIS] =
                preferences[FOLDER_URIS]
                    .orEmpty()
                    .minus(uriString)
        }

        val uri = Uri.parse(uriString)
        runCatching {
            resolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun resolveDisplayName(treeUri: Uri): String {
        val documentUri =
            runCatching {
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
            }.getOrDefault(treeUri)

        val queriedName =
            runCatching {
                resolver.query(
                    documentUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val index =
                        cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        )
                    if (index >= 0) cursor.getString(index) else null
                }
            }.getOrNull()

        return queriedName
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: runCatching { DocumentsContract.getTreeDocumentId(treeUri) }
                .getOrNull()
                ?.substringAfterLast(':')
                ?.substringAfterLast('/')
                ?.takeIf(String::isNotBlank)
            ?: "授权音乐目录"
    }

    private companion object {
        const val ContentScheme = "content"
        val FOLDER_URIS = stringSetPreferencesKey("folder_uris")
    }
}

internal fun isDocumentTreeUri(uri: Uri): Boolean =
    runCatching {
        DocumentsContract.getTreeDocumentId(uri).isNotBlank()
    }.getOrDefault(false)
