package top.geek_studio.chenlongcould.musicplayer.appshortcuts.shortcuttype;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import top.geek_studio.chenlongcould.musicplayer.Common.R;
import top.geek_studio.chenlongcould.musicplayer.appshortcuts.AppShortcutIconGenerator;
import top.geek_studio.chenlongcould.musicplayer.appshortcuts.AppShortcutLauncherActivity;

/**
 * 全部随即歌曲
 *
 * @author Adrian Campos
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public final class ShuffleAllShortcutType extends BaseShortcutType {

    /**
     * 构造
     */
    public ShuffleAllShortcutType(Context context) {
        super(context);
    }

    /**
     * 获取 ID
     */
    public static String getId() {
        return ID_PREFIX + "shuffle_all";
    }

    /**
     * @see super#getShortcutInfo()
     */
    public ShortcutInfo getShortcutInfo() {
        return new ShortcutInfo.Builder(context, getId())
                .setShortLabel(context.getString(R.string.app_shortcut_shuffle_all_short))
                .setLongLabel(context.getString(R.string.action_shuffle_all))
                .setIcon(AppShortcutIconGenerator.generateThemedIcon(context, R.drawable.ic_app_shortcut_shuffle_all))
                .setIntent(getPlaySongsIntent(AppShortcutLauncherActivity.SHORTCUT_TYPE_SHUFFLE_ALL))
                .build();
    }
}
