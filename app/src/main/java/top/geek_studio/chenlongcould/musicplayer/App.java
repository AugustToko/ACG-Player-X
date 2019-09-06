package top.geek_studio.chenlongcould.musicplayer;

import android.app.Application;
import android.os.Build;

import top.geek_studio.chenlongcould.musicplayer.appshortcuts.DynamicShortcutManager;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.chenlongcould.musicplayer.R;


/**
 * Application
 *
 * @author chenlongcould (Modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends Application {

    /**
     * 单例
     */
    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        // default theme
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeStore.editTheme(this)
                    .primaryColorRes(R.color.md_indigo_500)
                    .accentColorRes(R.color.md_pink_A400)
                    .commit();
        }

        // 设置动态快捷方式
        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).initDynamicShortcuts();
        }

    }

    /**
     * 是否为专业版
     */
    public static boolean isProVersion() {
        return true;
    }

    /**
     * 单例
     */
    public static App getInstance() {
        return app;
    }

}
