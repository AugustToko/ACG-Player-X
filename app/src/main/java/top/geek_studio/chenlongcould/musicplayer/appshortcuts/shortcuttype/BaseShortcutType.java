package top.geek_studio.chenlongcould.musicplayer.appshortcuts.shortcuttype;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Bundle;

import top.geek_studio.chenlongcould.musicplayer.appshortcuts.AppShortcutLauncherActivity;

/**
 * 快捷方式基类
 *
 * @author Adrian Campos
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public abstract class BaseShortcutType {

    /**
     * ID
     */
    static final String ID_PREFIX = "com.kabouzeid.gramophone.appshortcuts.id.";

    /**
     * CTX
     */
    Context context;

    /**
     * 构造
     */
    public BaseShortcutType(Context context) {
        this.context = context;
    }

    /**
     * 获取 ID
     * */
    static public String getId() {
        return ID_PREFIX + "invalid";
    }

    /**
     * 获取快捷方式
     * */
    abstract ShortcutInfo getShortcutInfo();

    /**
     * Creates an Intent that will launch MainActivtiy and immediately play {@param songs} in either shuffle or normal mode
     *
     * 创建一个Intent，它将启动MainActivtiy并立即以shuffle或普通模式播放 {@param songs}
     *
     * @param shortcutType Describes the type of shortcut to create (ShuffleAll, TopTracks, custom playlist, etc.)
     *                     描述要创建的快捷方式的类型（ShuffleAll，TopTracks，自定义播放列表等）
     *
     * @return intent
     */
    Intent getPlaySongsIntent(int shortcutType) {
        Intent intent = new Intent(context, AppShortcutLauncherActivity.class);
        intent.setAction(Intent.ACTION_VIEW);

        final Bundle b = new Bundle();
        b.putInt(AppShortcutLauncherActivity.KEY_SHORTCUT_TYPE, shortcutType);

        intent.putExtras(b);

        return intent;
    }
}
