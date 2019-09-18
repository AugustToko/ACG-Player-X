package top.geek_studio.chenlongcould.musicplayer.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.text.Html;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.chenlongcould.musicplayer.R;
import top.geek_studio.chenlongcould.musicplayer.model.PlaylistSong;
import top.geek_studio.chenlongcould.musicplayer.util.PlaylistsUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog 从播放列表中删除歌曲对话框
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class RemoveFromPlaylistDialog extends DialogFragment {

    @NonNull
    public static RemoveFromPlaylistDialog create(PlaylistSong song) {
        final List<PlaylistSong> list = new ArrayList<>();
        list.add(song);
        return create(list);
    }

    @NonNull
    public static RemoveFromPlaylistDialog create(final List<PlaylistSong> songs) {
        final RemoveFromPlaylistDialog dialog = new RemoveFromPlaylistDialog();
        final Bundle args = new Bundle();
        args.putParcelableArrayList("songs", new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<PlaylistSong> songs = getArguments().getParcelableArrayList("songs");
        int title;
        CharSequence content;
        if (songs.size() > 1) {
            title = R.string.remove_songs_from_playlist_title;
            content = Html.fromHtml(getString(R.string.remove_x_songs_from_playlist, songs.size()));
        } else {
            title = R.string.remove_song_from_playlist_title;
            content = Html.fromHtml(getString(R.string.remove_song_x_from_playlist, songs.get(0).title));
        }
        return new MaterialDialog.Builder(getActivity())
                .title(title)
                .content(content)
                .positiveText(R.string.remove_action)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> {
                    if (getActivity() == null)
                        return;
                    PlaylistsUtil.removeFromPlaylist(getActivity(), songs);
                })
                .build();
    }
}
