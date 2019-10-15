package top.geek_studio.chenlongcould.musicplayer.helper;

import android.content.Context;
import android.view.ViewGroup;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

/**
 * 水平适配器帮助类
 *
 * @author chenlongcould (Modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class HorizontalAdapterHelper {

    /**
     * 布局资源
     */
    public static final int LAYOUT_RES = R.layout.item_grid_card_horizontal;

    public static final int TYPE_FIRST = 1;
    public static final int TYPE_MIDDLE = 2;
    public static final int TYPE_LAST = 3;

    /**
     * 应用布局边距
     *
     * @param context      ctx
     * @param layoutParams lp
     * @param viewType     view 类型
     */
    public static void applyMarginToLayoutParams(Context context, ViewGroup.MarginLayoutParams layoutParams, int viewType) {
        int listMargin = context.getResources().getDimensionPixelSize(R.dimen.default_item_margin);
        if (viewType == TYPE_FIRST) {
            layoutParams.leftMargin = listMargin;
        } else if (viewType == TYPE_LAST) {
            layoutParams.rightMargin = listMargin;
        }
    }

    /**
     * 获得 item 的类型
     *
     * @param position  item 位置
     * @param itemCount item 数量
     */
    public static int getItemViewtype(int position, int itemCount) {
        if (position == 0) {
            return TYPE_FIRST;
        } else if (position == itemCount - 1) {
            return TYPE_LAST;
        } else return TYPE_MIDDLE;
    }
}
