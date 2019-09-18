package top.geek_studio.chenlongcould.musicplayer.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.chenlongcould.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.util.PlaylistsUtil;

/**
 * Dialog 用于创建播放列表
 *
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class CreatePlaylistDialog extends DialogFragment {

    private static final String SONGS = "songs";

    @NonNull
    public static CreatePlaylistDialog create() {
        return create((Song) null);
    }

    @NonNull
    public static CreatePlaylistDialog create(@Nullable Song song) {
        final List<Song> list = new ArrayList<>();
        if (song != null) list.add(song);
        return create(list);
    }

    @NonNull
    public static CreatePlaylistDialog create(List<Song> songs) {
        final CreatePlaylistDialog dialog = new CreatePlaylistDialog();
        final Bundle args = new Bundle();
        args.putParcelableArrayList(SONGS, new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.new_playlist_title)
                .positiveText(R.string.create_action)
                .negativeText(android.R.string.cancel)
                .inputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PERSON_NAME |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .input(R.string.playlist_name_empty, 0, false, (materialDialog, charSequence) -> {
                    if (getActivity() == null)
                        return;
                    final String name = charSequence.toString().trim();
                    if (!name.isEmpty()) {
                        if (!PlaylistsUtil.doesPlaylistExist(getActivity(), name)) {
                            final int playlistId = PlaylistsUtil.createPlaylist(getActivity(), name);
                            if (getActivity() != null) {
                                List<Song> songs = getArguments().getParcelableArrayList(SONGS);
                                if (songs != null && !songs.isEmpty()) {
                                    PlaylistsUtil.addToPlaylist(getActivity(), songs, playlistId, true);
                                }
                            }
                        } else {
                            Toast.makeText(getActivity(), getActivity().getResources().getString(
                                    R.string.playlist_exists, name), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .build();
    }
}