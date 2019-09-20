package top.geek_studio.chenlongcould.musicplayer.appshortcuts;

import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.IconCompat;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.chenlongcould.musicplayer.R;

import top.geek_studio.chenlongcould.musicplayer.util.ImageUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * 应用快捷方式图标生成器
 *
 * @author Adrian Campos
 */
@RequiresApi(Build.VERSION_CODES.N_MR1)
public final class AppShortcutIconGenerator {

    /**
     * 生成主题化图标
     *
     * @param context ctx
     * @param iconId  图标 ID
     *
     * @return 图标
     */
    public static Icon generateThemedIcon(Context context, int iconId) {
        // 判断是否彩色化 ICON
        if (PreferenceUtil.getInstance(context).coloredAppShortcuts()) {
            return generateUserThemedIcon(context, iconId).toIcon();
        } else {
            return generateDefaultThemedIcon(context, iconId).toIcon();
        }
    }

    /**
     * 生成主题化 Icon
     *
     * @param context         ctx
     * @param iconId          图标 ID
     * @param foregroundColor 前景色
     * @param backgroundColor 背景色
     *
     * @return 图标
     */
    private static IconCompat generateThemedIcon(final Context context, final int iconId, final int foregroundColor, final int backgroundColor) {
        // Get and tint foreground and background drawables
        // 获取前背景着色 Drawable
        final Drawable vectorDrawable = ImageUtil.getTintedVectorDrawable(context, iconId, foregroundColor);
        final Drawable backgroundDrawable = ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_app_shortcut_background, backgroundColor);

        // 如果是 O 以上，启用动态自适应图标
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final AdaptiveIconDrawable adaptiveIconDrawable = new AdaptiveIconDrawable(backgroundDrawable, vectorDrawable);
            return IconCompat.createWithAdaptiveBitmap(ImageUtil.createBitmap(adaptiveIconDrawable));
        } else {
            // Squash the two drawables together
            // 层级Drawable
            final LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, vectorDrawable});

            // Return as an Icon
            // 返回图标
            return IconCompat.createWithBitmap(ImageUtil.createBitmap(layerDrawable));
        }
    }

    /**
     * 生成默认主题图标
     *
     * @param context ctx
     * @param iconId  图标 ID
     *
     * @return 图标
     */
    private static IconCompat generateDefaultThemedIcon(Context context, int iconId) {
        // Return an Icon of iconId with default colors
        return generateThemedIcon(context, iconId,
                context.getColor(R.color.app_shortcut_default_foreground),
                context.getColor(R.color.app_shortcut_default_background)
        );
    }

    /**
     * 生成用户主题化图标
     *
     * @param context ctx
     * @param iconId  图标 ID
     *
     * @return 图标
     */
    private static IconCompat generateUserThemedIcon(Context context, int iconId) {
        // Get background color from context's theme
        final TypedValue typedColorBackground = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorBackground, typedColorBackground, true);

        // Return an Icon of iconId with those colors
        return generateThemedIcon(context, iconId,
                ThemeStore.primaryColor(context),
                typedColorBackground.data
        );
    }

}
