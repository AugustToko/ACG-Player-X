package top.geek_studio.chenlongcould.musicplayer.data

data class MusicFolder(
    val name: String,
    val path: String,
)

internal fun resolveMusicFolder(
    rawPath: String?,
    isRelativePath: Boolean,
): MusicFolder {
    val normalized = rawPath.orEmpty().replace('\\', '/')
    val directory =
        if (isRelativePath) {
            normalized.trim('/')
        } else {
            normalized.substringBeforeLast('/', missingDelimiterValue = "")
        }
    val path = directory.ifBlank { ROOT_FOLDER }
    val name =
        if (path == ROOT_FOLDER) {
            ROOT_FOLDER
        } else {
            path.trimEnd('/').substringAfterLast('/').ifBlank { ROOT_FOLDER }
        }

    return MusicFolder(name = name, path = path)
}

const val ROOT_FOLDER = "根目录"
