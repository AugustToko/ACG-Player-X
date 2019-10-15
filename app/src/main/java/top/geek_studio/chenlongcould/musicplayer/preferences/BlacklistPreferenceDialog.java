package top.geek_studio.chenlongcould.musicplayer.preferences;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import top.geek_studio.chenlongcould.musicplayer.Common.R;

import java.io.File;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.dialogs.BlacklistFolderChooserDialog;
import top.geek_studio.chenlongcould.musicplayer.provider.BlacklistStore;

/**
 * Dialog 黑名单对话框
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class BlacklistPreferenceDialog extends DialogFragment implements BlacklistFolderChooserDialog.FolderCallback {

    /**
     * 黑名单路径表
     */
    private List<String> paths;

    /**
     * 工厂
     */
    public static BlacklistPreferenceDialog newInstance() {
        return new BlacklistPreferenceDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final BlacklistFolderChooserDialog blacklistFolderChooserDialog
                = (BlacklistFolderChooserDialog) getChildFragmentManager().findFragmentByTag("FOLDER_CHOOSER");

        if (blacklistFolderChooserDialog != null) {
            blacklistFolderChooserDialog.setCallback(this);
        }

        refreshBlacklistData();

        return new MaterialDialog.Builder(getContext())
                .title(R.string.blacklist)
                .positiveText(android.R.string.ok)
                .neutralText(R.string.clear_action)
                .negativeText(R.string.add_action)
                .items(paths)
                .autoDismiss(false)
                .itemsCallback((materialDialog, view, i, charSequence) -> new MaterialDialog.Builder(getContext())
                        .title(R.string.remove_from_blacklist)
                        .content(Html.fromHtml(getString(R.string.do_you_want_to_remove_from_the_blacklist, charSequence)))
                        .positiveText(R.string.remove_action)
                        .negativeText(android.R.string.cancel)
                        .onPositive((materialDialog12, dialogAction) -> {
                            BlacklistStore.getInstance(getContext()).removePath(new File(charSequence.toString()));
                            refreshBlacklistData();
                        }).show())
                // clear
                .onNeutral((materialDialog, dialogAction) -> new MaterialDialog.Builder(getContext())
                        .title(R.string.clear_blacklist)
                        .content(R.string.do_you_want_to_clear_the_blacklist)
                        .positiveText(R.string.clear_action)
                        .negativeText(android.R.string.cancel)
                        .onPositive((materialDialog1, dialogAction1) -> {
                            BlacklistStore.getInstance(getContext()).clear();
                            refreshBlacklistData();
                        }).show())
                // add
                .onNegative((materialDialog, dialogAction) -> {
                    BlacklistFolderChooserDialog dialog = BlacklistFolderChooserDialog.create();
                    dialog.setCallback(BlacklistPreferenceDialog.this);
                    dialog.show(getChildFragmentManager(), "FOLDER_CHOOSER");
                })
                .onPositive((materialDialog, dialogAction) -> dismiss())
                .build();
    }

    /**
     * 更新黑名单路径表
     */
    private void refreshBlacklistData() {
        final Context context = getContext();
        if (context == null) return;

        paths = BlacklistStore.getInstance(getContext()).getPaths();

        // 获取当前 fragment 中的 dialog
        final MaterialDialog dialog = (MaterialDialog) getDialog();

        if (dialog != null) {
            String[] pathArray = new String[paths.size()];
            pathArray = paths.toArray(pathArray);
            dialog.setItems((CharSequence[]) pathArray);
        }
    }

    /**
     * 添加黑名单路径, 并更新
     * */
    @Override
    public void onFolderSelection(@NonNull BlacklistFolderChooserDialog folderChooserDialog, @NonNull File file) {
        BlacklistStore.getInstance(getContext()).addPath(file);
        refreshBlacklistData();
    }
}
