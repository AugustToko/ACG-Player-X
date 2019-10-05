package top.geek_studio.chenlongcould.musicplayer.util;

import android.graphics.Color;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;

/**
 * @author : chenlongcould
 * @date : 2019/10/05/14
 */
public class MatDialogUtil {
    /**
     * 让 NEUTRAL 位置处的 Button 不起作用
     *
     * @param dialog d
     * */
    public static void setNeutralButtonDisnable(final MaterialDialog dialog) {
        final MDButton button = dialog.getActionButton(DialogAction.NEUTRAL);
        button.setEnabled(false);
        button.setClickable(false);
        button.setTextColor(Color.GRAY);
    }
}
