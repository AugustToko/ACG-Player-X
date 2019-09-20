package top.geek_studio.chenlongcould.musicplayer.appshortcuts;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import java.util.Arrays;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.appshortcuts.shortcuttype.LastAddedShortcutType;
import top.geek_studio.chenlongcould.musicplayer.appshortcuts.shortcuttype.ShuffleAllShortcutType;
import top.geek_studio.chenlongcould.musicplayer.appshortcuts.shortcuttype.TopTracksShortcutType;

/**
 * +
 * 动态快捷方式管理
 *
 * @author Adrian Campos
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public class DynamicShortcutManager {

    /**
     * ctx
     */
    private Context context;

    /**
     * 快捷方式管理
     */
    private ShortcutManager shortcutManager;

    /**
     * 构造
     *
     * @param context ctx
     */
    public DynamicShortcutManager(final Context context) {
        this.context = context;
        shortcutManager = this.context.getSystemService(ShortcutManager.class);
    }

    /**
     * 创建快捷方式
     *
     * @param context    ctx
     * @param id         id
     * @param shortLabel 短标题
     * @param longLabel  长标题
     * @param icon       图标
     * @param intent     intent
     *
     * @return ShortcutInfo
     */
    public static ShortcutInfo createShortcut(final Context context, final String id, final String shortLabel, final String longLabel, final Icon icon, final Intent intent) {
        return new ShortcutInfo.Builder(context, id)
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIcon(icon)
                .setIntent(intent)
                .build();
    }

    /**
     * 初始化
     */
    public void initDynamicShortcuts() {
        // 如果快捷方式为 0, 默认加载原始三个快捷方式
        if (shortcutManager.getDynamicShortcuts().size() == 0) {
            shortcutManager.setDynamicShortcuts(getDefaultShortcuts());
        }
    }

    /**
     * 更新快捷方式
     */
    public void updateDynamicShortcuts() {
        shortcutManager.updateShortcuts(getDefaultShortcuts());
    }

    /**
     * 获取默认快捷方式
     *
     * @return 快捷方式组
     */
    public List<ShortcutInfo> getDefaultShortcuts() {
        return (Arrays.asList(
                new ShuffleAllShortcutType(context).getShortcutInfo(),
                new TopTracksShortcutType(context).getShortcutInfo(),
                new LastAddedShortcutType(context).getShortcutInfo()
        ));
    }

    /**
     * 回报 ID
     */
    public static void reportShortcutUsed(Context context, String shortcutId) {
        context.getSystemService(ShortcutManager.class).reportShortcutUsed(shortcutId);
    }
}
