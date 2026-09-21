package top.geek_studio.chenlongcould.musicplayer.ui

import java.io.IOException

/** Partial provider results are useful for browsing, but unsafe for statistics replacement. */
internal fun <T> requireCompleteStatisticsMetadata(songs: List<T>, warnings: List<String>): List<T> {
    val problems = warnings.filter(String::isNotBlank)
    if (problems.isNotEmpty()) {
        throw IOException(
            "音乐来源尚未完整读取，未生成统计导入预览。请恢复来源或调整授权后重试：" +
                problems.take(5).joinToString("；").take(2_000),
        )
    }
    return songs
}
