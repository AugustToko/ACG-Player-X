package top.geek_studio.chenlongcould.musicplayer.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import top.geek_studio.chenlongcould.musicplayer.Common.R;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil;

/**
 * Dialog 用于删除歌曲对话框
 *
 * @author chenlongcould (Modify, add notes)
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class DeleteSongsDialog extends DialogFragment {

    /**
     * 工厂 A
     *
     * @param song song
     *
     * @return dialog
     */
    @NonNull
    public static DeleteSongsDialog create(Song song) {
        final List<Song> list = new ArrayList<>();
        list.add(song);
        return create(list);
    }

    /**
     * 工厂 B
     *
     * @param songs songs
     * @return dialog
     * */
    @NonNull
    public static DeleteSongsDialog create(final List<Song> songs) {
        final DeleteSongsDialog dialog = new DeleteSongsDialog();
        final Bundle args = new Bundle();
        args.putParcelableArrayList("songs", new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 获得歌曲
        final List<Song> songs = getArguments().getParcelableArrayList("songs");
        int title;
        CharSequence content;
        if (songs.size() > 1) {
            title = R.string.delete_songs_title;
            content = Html.fromHtml(getString(R.string.delete_x_songs, songs.size()));
        } else {
            title = R.string.delete_song_title;
            content = Html.fromHtml(getString(R.string.delete_song_x, songs.get(0).title));
        }

        return new MaterialDialog.Builder(getActivity())
                .title(title)
                .content(content)
                .positiveText(R.string.delete_action)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> {
                    if (getActivity() == null)
                        return;
                    MusicUtil.deleteTracks(getActivity(), songs);
                })
                .build();
    }
}
