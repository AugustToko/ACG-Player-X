package top.geek_studio.chenlongcould.musicplayer.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import top.geek_studio.chenlongcould.musicplayer.MainActivity
import top.geek_studio.chenlongcould.musicplayer.R

object AppShortcuts {
    const val ACTION_OPEN_FAVORITES =
        "top.geek_studio.chenlongcould.musicplayer.action.OPEN_FAVORITES"
    const val ACTION_OPEN_RECENT =
        "top.geek_studio.chenlongcould.musicplayer.action.OPEN_RECENT"

    fun publish(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            publishApi25(context.applicationContext)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun publishApi25(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val favorites =
            ShortcutInfo.Builder(context, SHORTCUT_FAVORITES)
                .setShortLabel("收藏")
                .setLongLabel("打开收藏歌曲")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_favorite))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(ACTION_OPEN_FAVORITES),
                )
                .setRank(0)
                .build()
        val recent =
            ShortcutInfo.Builder(context, SHORTCUT_RECENT)
                .setShortLabel("最近播放")
                .setLongLabel("打开最近播放")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_recent))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(ACTION_OPEN_RECENT),
                )
                .setRank(1)
                .build()

        runCatching {
            shortcutManager.dynamicShortcuts = listOf(favorites, recent)
        }
    }

    private const val SHORTCUT_FAVORITES = "open_favorites"
    private const val SHORTCUT_RECENT = "open_recent"
}
