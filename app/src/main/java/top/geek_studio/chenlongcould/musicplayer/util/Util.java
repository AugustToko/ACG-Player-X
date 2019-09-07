package top.geek_studio.chenlongcould.musicplayer.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.chenlongcould.musicplayer.R;

/**
 * 工具类
 *
 * @author chenlongcould (Modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class Util {

    /**
     * getActionBarSize
     *
     * @param context context
     */
    public static int getActionBarSize(@NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = context.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

    /**
     * getScreenSize
     *
     * @param c context
     */
    public static Point getScreenSize(@NonNull Context c) {
        WindowManager windowManager = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return new Point(0, 0);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    /**
     * setStatusBarTranslucent
     * <p>
     * for {@link Build.VERSION_CODES#KITKAT}
     *
     * @param window window
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void setStatusBarTranslucent(@NonNull Window window) {
        window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    /**
     * setAllowDrawUnderStatusBar
     *
     * @param window window
     * */
    public static void setAllowDrawUnderStatusBar(@NonNull Window window) {
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /**
     * 隐藏软键盘
     *
     * @param activity activity
     * */
    public static void hideSoftKeyboard(@Nullable Activity activity) {
        if (activity != null) {
            View currentFocus = activity.getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (inputMethodManager == null) return;
                inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }

    public static boolean isTablet(@NonNull final Resources resources) {
        return resources.getConfiguration().smallestScreenWidthDp >= 600;
    }

    /**
     * 是否宽屏
     *
     * @param resources res
     * */
    public static boolean isLandscape(@NonNull final Resources resources) {
        return resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static int resolveDimensionPixelSize(@NonNull Context context, @AttrRes int dimenAttr) {
        TypedArray a = context.obtainStyledAttributes(new int[]{dimenAttr});
        int dimensionPixelSize = a.getDimensionPixelSize(0, 0);
        a.recycle();
        return dimensionPixelSize;
    }

    /**
     * 是否为 RTL 模式
     *
     * @param context context
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRTL(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration config = context.getResources().getConfiguration();
            return config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        } else return false;
    }

}
