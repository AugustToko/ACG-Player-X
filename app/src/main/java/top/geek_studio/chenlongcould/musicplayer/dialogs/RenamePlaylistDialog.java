package top.geek_studio.chenlongcould.musicplayer.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.text.InputType;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.chenlongcould.musicplayer.R;
import top.geek_studio.chenlongcould.musicplayer.util.PlaylistsUtil;

/**
 * Dialog 用于重命名播放列表
 *
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class RenamePlaylistDialog extends DialogFragment {

    /**
     * TAG for {@link Bundle}
     */
    private static final String PLAYLIST_ID = "playlist_id";

    @NonNull
    public static RenamePlaylistDialog create(long playlistId) {
        final RenamePlaylistDialog dialog = new RenamePlaylistDialog();
        final Bundle args = new Bundle();
        args.putLong(PLAYLIST_ID, playlistId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        long playlistId = getArguments().getLong(PLAYLIST_ID);
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.rename_playlist_title)
                .positiveText(R.string.rename_action)
                .negativeText(android.R.string.cancel)
                .inputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PERSON_NAME |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .input(getString(R.string.playlist_name_empty), PlaylistsUtil.getNameForPlaylist(getActivity(), playlistId), false,
                        (materialDialog, charSequence) -> {
                            final String name = charSequence.toString().trim();
                            if (!name.isEmpty()) {
                                long playlistId1 = getArguments().getLong(PLAYLIST_ID);
                                PlaylistsUtil.renamePlaylist(getActivity(), playlistId1, name);
                            }
                        })
                .build();
    }
}