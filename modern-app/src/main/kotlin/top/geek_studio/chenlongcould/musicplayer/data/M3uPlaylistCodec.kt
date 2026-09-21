package top.geek_studio.chenlongcould.musicplayer.data

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.abs
import top.geek_studio.chenlongcould.musicplayer.model.Song

data class M3uEntry(
    val location: String,
    val title: String? = null,
    val artist: String? = null,
    val durationSeconds: Long? = null,
    val explicitMediaId: String? = null,
    val contentUriHint: String? = null,
    val fileNameHint: String? = null,
    val folderHint: String? = null,
)

data class ParsedM3uPlaylist(
    val name: String?,
    val entries: List<M3uEntry>,
    val truncated: Boolean = false,
)

data class M3uResolution(
    val mediaIds: List<String>,
    val matchedCount: Int,
    val preservedUnavailableCount: Int,
    val unmatchedCount: Int,
    val ambiguousCount: Int,
    val duplicateCount: Int,
)

enum class PlaylistImportEntryStatus {
    MATCHED,
    PRESERVED_UNAVAILABLE,
    AMBIGUOUS,
    UNMATCHED,
    DUPLICATE,
}

internal data class M3uEntryResolution(
    val index: Int,
    val entry: M3uEntry,
    val status: PlaylistImportEntryStatus,
    val automaticMediaId: String? = null,
    val candidateMediaIds: List<String> = emptyList(),
    val candidateCount: Int = candidateMediaIds.size,
)

internal data class M3uDetailedResolution(
    val entries: List<M3uEntryResolution>,
)

internal fun parseM3uPlaylist(
    content: String,
    fallbackName: String? = null,
): ParsedM3uPlaylist {
    var playlistName = fallbackName?.trim()?.takeIf(String::isNotEmpty)
    var pendingTitle: String? = null
    var pendingArtist: String? = null
    var pendingDurationSeconds: Long? = null
    var pendingMediaId: String? = null
    var pendingContentUri: String? = null
    var pendingFileName: String? = null
    var pendingFolder: String? = null
    var truncated = false
    val entries = mutableListOf<M3uEntry>()

    content.removePrefix("\uFEFF").lineSequence().forEach { rawLine ->
        if (truncated) return@forEach
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEach

        when {
            line.startsWith(PLAYLIST_PREFIX, ignoreCase = true) -> {
                playlistName =
                    line.substring(PLAYLIST_PREFIX.length)
                        .trim()
                        .takeIf(String::isNotEmpty)
                        ?: playlistName
            }

            line.startsWith(EXTINF_PREFIX, ignoreCase = true) -> {
                val parsed = parseExtInf(line.substring(EXTINF_PREFIX.length))
                pendingTitle = parsed.title
                pendingArtist = parsed.artist
                pendingDurationSeconds = parsed.durationSeconds
            }

            line.startsWith(MEDIA_ID_PREFIX, ignoreCase = true) -> {
                pendingMediaId = normalizeMediaId(line.substring(MEDIA_ID_PREFIX.length))
            }

            line.startsWith(CONTENT_URI_PREFIX, ignoreCase = true) -> {
                pendingContentUri =
                    line.substring(CONTENT_URI_PREFIX.length)
                        .trim()
                        .takeIf(String::isNotEmpty)
            }

            line.startsWith(FILE_NAME_PREFIX, ignoreCase = true) -> {
                pendingFileName =
                    line.substring(FILE_NAME_PREFIX.length)
                        .trim()
                        .takeIf(String::isNotEmpty)
            }

            line.startsWith(FOLDER_PREFIX, ignoreCase = true) -> {
                pendingFolder =
                    line.substring(FOLDER_PREFIX.length)
                        .trim()
                        .takeIf(String::isNotEmpty)
            }

            line.startsWith('#') -> Unit

            else -> {
                if (entries.size >= MAX_M3U_ENTRIES) {
                    truncated = true
                    return@forEach
                }
                entries +=
                    M3uEntry(
                        location = line,
                        title = pendingTitle,
                        artist = pendingArtist,
                        durationSeconds = pendingDurationSeconds,
                        explicitMediaId = pendingMediaId ?: mediaIdFromAcgLocation(line),
                        contentUriHint = pendingContentUri,
                        fileNameHint = pendingFileName,
                        folderHint = pendingFolder,
                    )
                pendingTitle = null
                pendingArtist = null
                pendingDurationSeconds = null
                pendingMediaId = null
                pendingContentUri = null
                pendingFileName = null
                pendingFolder = null
            }
        }
    }

    return ParsedM3uPlaylist(
        name = playlistName,
        entries = entries,
        truncated = truncated,
    )
}

internal fun encodeM3uPlaylist(
    playlist: UserPlaylist,
    songs: List<Song>,
): String {
    val songsById = songs.associateBy { it.id.toString() }
    return buildString {
        appendLine("#EXTM3U")
        appendLine("$PLAYLIST_PREFIX${sanitizeM3uComment(playlist.name)}")
        playlist.mediaIds.forEach { mediaId ->
            val song = songsById[mediaId]
            if (song == null) {
                appendLine("#EXTINF:-1,Unavailable media $mediaId")
                appendLine("$MEDIA_ID_PREFIX$mediaId")
                appendLine("$ACG_MEDIA_LOCATION_PREFIX$mediaId")
                return@forEach
            }

            val durationSeconds =
                song.durationMs
                    .takeIf { it > 0L }
                    ?.div(1_000L)
                    ?: -1L
            val label =
                listOf(song.artist, song.title)
                    .map(::sanitizeM3uComment)
                    .joinToString(" - ")
            appendLine("#EXTINF:$durationSeconds,$label")
            appendLine("$MEDIA_ID_PREFIX$mediaId")
            appendLine("$CONTENT_URI_PREFIX${sanitizeM3uLocation(song.contentUri)}")
            if (song.displayName.isNotBlank()) {
                appendLine("$FILE_NAME_PREFIX${sanitizeM3uComment(song.displayName)}")
            }
            val folder = displayMusicFolderPath(song.folderPath)
            if (folder.isNotBlank()) {
                appendLine("$FOLDER_PREFIX${sanitizeM3uComment(folder)}")
            }
            appendLine(
                portableM3uLocation(song)
                    ?: sanitizeM3uLocation(song.contentUri),
            )
        }
    }
}

internal fun portableM3uLocation(song: Song): String? {
    val fileName = sanitizePortablePathSegment(song.displayName) ?: return null
    val rawFolder = displayMusicFolderPath(song.folderPath)
    val normalizedFolder = normalizePortableFolder(rawFolder)
    val folderSegments =
        normalizedFolder
            .split('/')
            .mapNotNull(::sanitizePortablePathSegment)
            .map { segment -> if (segment == "..") "_" else segment }
            .filterNot { it == "." }
    return (folderSegments + fileName).joinToString("/")
}

internal fun resolveM3uPlaylist(
    parsed: ParsedM3uPlaylist,
    songs: List<Song>,
): M3uResolution {
    val detailed = resolveM3uPlaylistDetailed(parsed, songs)
    return M3uResolution(
        mediaIds =
            detailed.entries.mapNotNull { resolution ->
                resolution.automaticMediaId.takeIf {
                    resolution.status == PlaylistImportEntryStatus.MATCHED ||
                        resolution.status == PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE
                }
            },
        matchedCount = detailed.entries.count { it.status == PlaylistImportEntryStatus.MATCHED },
        preservedUnavailableCount =
            detailed.entries.count {
                it.status == PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE
            },
        unmatchedCount = detailed.entries.count { it.status == PlaylistImportEntryStatus.UNMATCHED },
        ambiguousCount = detailed.entries.count { it.status == PlaylistImportEntryStatus.AMBIGUOUS },
        duplicateCount = detailed.entries.count { it.status == PlaylistImportEntryStatus.DUPLICATE },
    )
}

internal fun resolveM3uPlaylistDetailed(
    parsed: ParsedM3uPlaylist,
    songs: List<Song>,
): M3uDetailedResolution {
    val songsById = songs.associateBy { it.id.toString() }
    val songsByUri = songs.groupBy { normalizeUri(it.contentUri) }
    val songsByFileName =
        songs
            .filter { it.displayName.isNotBlank() }
            .groupBy { normalizeFileName(it.displayName) }
    val seenMediaIds = hashSetOf<String>()

    val resolutions =
        parsed.entries.mapIndexed { index, entry ->
            val explicitMediaId = normalizeMediaId(entry.explicitMediaId)
            val explicitSong =
                explicitMediaId
                    ?.let(songsById::get)
                    ?.takeIf { song -> explicitSongMatchesEntry(song, entry) }
            val outcome =
                explicitSong?.let { MatchOutcome(song = it, candidates = listOf(it)) }
                    ?: matchByUri(entry, songsByUri)
                    ?: matchByFileName(entry, songsByFileName)
                    ?: matchByMetadata(entry, songs)

            val candidateMediaIds =
                outcome
                    ?.candidates
                    .orEmpty()
                    .asSequence()
                    .map { it.id.toString() }
                    .distinct()
                    .take(MAX_IMPORT_CANDIDATES)
                    .toList()
            val candidateCount = outcome?.candidates?.distinctBy(Song::id)?.size ?: 0
            val resolvedId = outcome?.song?.id?.toString()

            when {
                resolvedId != null && seenMediaIds.add(resolvedId) -> {
                    M3uEntryResolution(
                        index = index,
                        entry = entry,
                        status = PlaylistImportEntryStatus.MATCHED,
                        automaticMediaId = resolvedId,
                        candidateMediaIds = candidateMediaIds.ifEmpty { listOf(resolvedId) },
                        candidateCount = candidateCount.coerceAtLeast(1),
                    )
                }

                resolvedId != null -> {
                    M3uEntryResolution(
                        index = index,
                        entry = entry,
                        status = PlaylistImportEntryStatus.DUPLICATE,
                        automaticMediaId = resolvedId,
                        candidateMediaIds = candidateMediaIds.ifEmpty { listOf(resolvedId) },
                        candidateCount = candidateCount.coerceAtLeast(1),
                    )
                }

                outcome?.ambiguous == true -> {
                    M3uEntryResolution(
                        index = index,
                        entry = entry,
                        status = PlaylistImportEntryStatus.AMBIGUOUS,
                        candidateMediaIds = candidateMediaIds,
                        candidateCount = candidateCount,
                    )
                }

                explicitMediaId != null && isAcgMediaLocation(entry.location) &&
                    seenMediaIds.add(explicitMediaId) -> {
                    M3uEntryResolution(
                        index = index,
                        entry = entry,
                        status = PlaylistImportEntryStatus.PRESERVED_UNAVAILABLE,
                        automaticMediaId = explicitMediaId,
                    )
                }

                explicitMediaId != null && isAcgMediaLocation(entry.location) -> {
                    M3uEntryResolution(
                        index = index,
                        entry = entry,
                        status = PlaylistImportEntryStatus.DUPLICATE,
                        automaticMediaId = explicitMediaId,
                    )
                }

                else -> {
                    M3uEntryResolution(
                        index = index,
                        entry = entry,
                        status = PlaylistImportEntryStatus.UNMATCHED,
                    )
                }
            }
        }

    return M3uDetailedResolution(entries = resolutions)
}

fun m3uExportFileName(name: String): String {
    val stem =
        buildString {
            name.trim().forEach { character ->
                append(
                    when {
                        character.code < 32 -> '_'
                        character in INVALID_FILE_NAME_CHARACTERS -> '_'
                        else -> character
                    },
                )
            }
        }
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.')
            .take(MAX_EXPORT_FILE_STEM_LENGTH)
            .ifBlank { "playlist" }
    return "$stem.m3u8"
}

private fun explicitSongMatchesEntry(
    song: Song,
    entry: M3uEntry,
): Boolean =
    if (hasPortableIdentityHints(entry)) {
        portableIdentityMatches(song, entry)
    } else {
        sequenceOf(entry.contentUriHint, entry.location)
            .filterNotNull()
            .any { candidate -> normalizeUri(song.contentUri) == normalizeUri(candidate) } ||
            isAcgMediaLocation(entry.location)
    }

private fun hasPortableIdentityHints(entry: M3uEntry): Boolean =
    !entry.fileNameHint.isNullOrBlank() ||
        !entry.title.isNullOrBlank() ||
        !entry.artist.isNullOrBlank() ||
        entry.durationSeconds != null

private fun portableIdentityMatches(
    song: Song,
    entry: M3uEntry,
): Boolean {
    entry.fileNameHint?.takeIf(String::isNotBlank)?.let { fileNameHint ->
        if (
            song.displayName.isBlank() ||
            normalizeFileName(song.displayName) != normalizeFileName(fileNameHint)
        ) {
            return false
        }
    }
    if (
        !entry.title.isNullOrBlank() &&
        normalizeText(song.title) != normalizeText(entry.title)
    ) {
        return false
    }
    if (
        !entry.artist.isNullOrBlank() &&
        normalizeText(song.artist) != normalizeText(entry.artist)
    ) {
        return false
    }
    return durationMatches(song, entry.durationSeconds)
}

private fun matchByUri(
    entry: M3uEntry,
    songsByUri: Map<String, List<Song>>,
): MatchOutcome? {
    val candidates =
        sequenceOf(entry.contentUriHint, entry.location)
            .filterNotNull()
            .flatMap { location -> songsByUri[normalizeUri(location)].orEmpty().asSequence() }
            .distinctBy(Song::id)
            .toList()
    val compatibleCandidates =
        if (hasPortableIdentityHints(entry)) {
            candidates.filter { song -> portableIdentityMatches(song, entry) }
        } else {
            candidates
        }
    return chooseCandidate(compatibleCandidates, entry)
}

private fun matchByFileName(
    entry: M3uEntry,
    songsByFileName: Map<String, List<Song>>,
): MatchOutcome? {
    val fileName =
        entry.fileNameHint
            ?.takeIf(String::isNotBlank)
            ?: fileNameFromLocation(entry.location)
    if (fileName.isNullOrBlank()) return null

    val compatibleCandidates =
        songsByFileName[normalizeFileName(fileName)]
            .orEmpty()
            .filter { song -> portableIdentityMatches(song, entry) }
    return chooseCandidate(compatibleCandidates, entry)
}

private fun matchByMetadata(
    entry: M3uEntry,
    songs: List<Song>,
): MatchOutcome? {
    val title = entry.title?.takeIf(String::isNotBlank) ?: return null
    val candidates =
        songs.filter { song ->
            normalizeText(song.title) == normalizeText(title) &&
                (entry.artist.isNullOrBlank() ||
                    normalizeText(song.artist) == normalizeText(entry.artist)) &&
                durationMatches(song, entry.durationSeconds)
        }
    return when (candidates.size) {
        0 -> null
        1 -> MatchOutcome(song = candidates.single(), candidates = candidates)
        else -> MatchOutcome(song = null, candidates = candidates)
    }
}

private fun chooseCandidate(
    candidates: List<Song>,
    entry: M3uEntry,
): MatchOutcome? {
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) {
        return MatchOutcome(song = candidates.single(), candidates = candidates)
    }

    var narrowed = candidates
    val folderHint =
        entry.folderHint
            ?.takeIf(String::isNotBlank)
            ?: folderFromLocation(entry.location)
    if (!folderHint.isNullOrBlank()) {
        val normalizedFolderHint = normalizePath(folderHint)
        val byFolder =
            narrowed.filter { song ->
                val candidateFolder = normalizePath(displayMusicFolderPath(song.folderPath))
                candidateFolder == normalizedFolderHint ||
                    candidateFolder.endsWith("/$normalizedFolderHint") ||
                    normalizedFolderHint.endsWith("/$candidateFolder")
            }
        if (byFolder.isNotEmpty()) narrowed = byFolder
    }

    entry.title?.takeIf(String::isNotBlank)?.let { title ->
        val byTitle = narrowed.filter { normalizeText(it.title) == normalizeText(title) }
        if (byTitle.isNotEmpty()) narrowed = byTitle
    }
    entry.artist?.takeIf(String::isNotBlank)?.let { artist ->
        val byArtist = narrowed.filter { normalizeText(it.artist) == normalizeText(artist) }
        if (byArtist.isNotEmpty()) narrowed = byArtist
    }
    entry.durationSeconds?.let { duration ->
        val byDuration = narrowed.filter { durationMatches(it, duration) }
        if (byDuration.isNotEmpty()) narrowed = byDuration
    }

    return when (narrowed.size) {
        0 -> null
        1 -> MatchOutcome(song = narrowed.single(), candidates = narrowed)
        else -> MatchOutcome(song = null, candidates = narrowed)
    }
}

private fun durationMatches(
    song: Song,
    durationSeconds: Long?,
): Boolean {
    if (durationSeconds == null || durationSeconds < 0L || song.durationMs <= 0L) return true
    return abs(song.durationMs / 1_000L - durationSeconds) <= DURATION_TOLERANCE_SECONDS
}

private fun parseExtInf(value: String): ExtInf {
    val durationPart = value.substringBefore(',', missingDelimiterValue = value).trim()
    val label = value.substringAfter(',', missingDelimiterValue = "").trim()
    val duration = durationPart.toLongOrNull()?.takeIf { it >= 0L }
    val separatorIndex = label.indexOf(" - ")
    val artist =
        if (separatorIndex > 0) {
            label.substring(0, separatorIndex).trim().takeIf(String::isNotEmpty)
        } else {
            null
        }
    val title =
        if (separatorIndex > 0) {
            label.substring(separatorIndex + 3).trim().takeIf(String::isNotEmpty)
        } else {
            label.takeIf(String::isNotEmpty)
        }
    return ExtInf(title = title, artist = artist, durationSeconds = duration)
}

private fun normalizeMediaId(value: String?): String? =
    value
        ?.trim()
        ?.takeIf { it.toLongOrNull() != null }

private fun mediaIdFromAcgLocation(location: String): String? =
    location
        .takeIf(::isAcgMediaLocation)
        ?.substring(ACG_MEDIA_LOCATION_PREFIX.length)
        ?.let(::normalizeMediaId)

private fun isAcgMediaLocation(location: String): Boolean =
    location.startsWith(ACG_MEDIA_LOCATION_PREFIX, ignoreCase = true)

private fun normalizeUri(value: String): String = value.trim()

private fun normalizeFileName(value: String): String =
    decodeUrl(value)
        .trim()
        .lowercase(Locale.ROOT)

private fun normalizeText(value: String?): String =
    value.orEmpty().trim().lowercase(Locale.ROOT)

private fun normalizePath(value: String): String =
    decodeUrl(value)
        .replace('\\', '/')
        .trim('/')
        .lowercase(Locale.ROOT)

private fun fileNameFromLocation(location: String): String? {
    if (isAcgMediaLocation(location)) return null
    val cleaned =
        location
            .substringBefore('#')
            .substringBefore('?')
            .replace('\\', '/')
            .trimEnd('/')
    return cleaned.substringAfterLast('/').takeIf(String::isNotBlank)?.let(::decodeUrl)
}

private fun folderFromLocation(location: String): String? {
    if (isAcgMediaLocation(location)) return null
    val cleaned =
        location
            .substringBefore('#')
            .substringBefore('?')
            .replace('\\', '/')
            .trimEnd('/')
    val parent = cleaned.substringBeforeLast('/', missingDelimiterValue = "")
    return parent.takeIf(String::isNotBlank)?.let(::decodeUrl)
}

private fun normalizePortableFolder(value: String): String {
    var normalized =
        value
            .replace('\\', '/')
            .trim()
    normalized = normalized.replace(WINDOWS_DRIVE_PREFIX, "")
    normalized = normalized.replace(ANDROID_PRIMARY_STORAGE_PREFIX, "")
    normalized = normalized.replace(ANDROID_REMOVABLE_STORAGE_PREFIX, "")
    return normalized.trim('/')
}

private fun sanitizePortablePathSegment(value: String): String? {
    val cleaned =
        sanitizeM3uLocation(value)
            .trim()
            .replace('/', '／')
            .replace('\\', '＼')
    return cleaned.takeIf(String::isNotBlank)
}

private fun decodeUrl(value: String): String =
    runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }
        .getOrDefault(value)

private fun sanitizeM3uComment(value: String): String =
    value.replace('\r', ' ').replace('\n', ' ').trim()

private fun sanitizeM3uLocation(value: String): String =
    value.replace('\r', ' ').replace('\n', ' ').trim()

private data class ExtInf(
    val title: String?,
    val artist: String?,
    val durationSeconds: Long?,
)

private data class MatchOutcome(
    val song: Song?,
    val candidates: List<Song>,
) {
    val ambiguous: Boolean
        get() = song == null && candidates.size > 1
}

private const val MAX_M3U_ENTRIES = 20_000
private const val MAX_IMPORT_CANDIDATES = 100
private const val DURATION_TOLERANCE_SECONDS = 3L
private const val MAX_EXPORT_FILE_STEM_LENGTH = 72
private const val PLAYLIST_PREFIX = "#PLAYLIST:"
private const val EXTINF_PREFIX = "#EXTINF:"
private const val MEDIA_ID_PREFIX = "#ACGPLAYER-MEDIA-ID:"
private const val CONTENT_URI_PREFIX = "#ACGPLAYER-CONTENT-URI:"
private const val FILE_NAME_PREFIX = "#ACGPLAYER-FILE-NAME:"
private const val FOLDER_PREFIX = "#ACGPLAYER-FOLDER:"
private const val ACG_MEDIA_LOCATION_PREFIX = "acg-player://media/"
private val INVALID_FILE_NAME_CHARACTERS = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
private val WINDOWS_DRIVE_PREFIX = Regex("^[A-Za-z]:/+", RegexOption.IGNORE_CASE)
private val ANDROID_PRIMARY_STORAGE_PREFIX =
    Regex("^/?(?:storage/emulated/0|sdcard)/+", RegexOption.IGNORE_CASE)
private val ANDROID_REMOVABLE_STORAGE_PREFIX =
    Regex("^/?storage/[^/]+/+", RegexOption.IGNORE_CASE)
