package top.geek_studio.chenlongcould.musicplayer;

import android.app.Application;
import android.os.Build;

import top.geek_studio.chenlongcould.musicplayer.appshortcuts.DynamicShortcutManager;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.chenlongcould.musicplayer.R;


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends Application {

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

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).initDynamicShortcuts();
        }

    }

    public static boolean isProVersion() {
        return true;
    }

    public static App getInstance() {
        return app;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
