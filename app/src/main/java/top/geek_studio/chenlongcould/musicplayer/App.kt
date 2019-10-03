package top.geek_studio.chenlongcould.musicplayer

import android.os.Build
import androidx.multidex.MultiDexApplication
import com.kabouzeid.appthemehelper.ThemeStore
import com.kabouzeid.chenlongcould.musicplayer.R
import top.geek_studio.chenlongcould.musicplayer.appshortcuts.DynamicShortcutManager


/**
 * Application
 *
 * @author chenlongcould (Modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // default theme
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeStore.editTheme(this)
                    .primaryColorRes(R.color.md_indigo_500)
                    .accentColorRes(R.color.md_pink_A400)
                    .commit()
        }

        // 设置动态快捷方式
        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            DynamicShortcutManager(this).initDynamicShortcuts()
        }

    }


    companion object {

        /**
         * 单例
         */
        var instance: App? = null
            private set

        /**
         * 是否为专业版
         */
        val isProVersion: Boolean
            get() = true
    }

}
