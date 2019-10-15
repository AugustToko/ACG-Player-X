package top.geek_studio.chenlongcould.musicplayer.ui.fragments.player;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

/**
 * 目前正在播放的页面
 */
public enum NowPlayingScreen {
    CARD(R.string.card, R.drawable.np_card, 0),
    FLAT(R.string.flat, R.drawable.np_flat, 1);

    /**
     * Title
     */
    @StringRes
    public final int titleRes;

    /**
     * Drawable
     */
    @DrawableRes
    public final int drawableResId;

    /**
     * ID (用于标记身份)
     * */
    public final int id;

    NowPlayingScreen(@StringRes int titleRes, @DrawableRes int drawableResId, int id) {
        this.titleRes = titleRes;
        this.drawableResId = drawableResId;
        this.id = id;
    }
}
