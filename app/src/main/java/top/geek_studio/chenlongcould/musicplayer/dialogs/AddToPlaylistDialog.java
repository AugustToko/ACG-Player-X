package top.geek_studio.chenlongcould.musicplayer.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import top.geek_studio.chenlongcould.musicplayer.Common.R;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.util.PlaylistsUtil;

/**
 * Dialog 用于添加歌曲到播放列表
 *
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class AddToPlaylistDialog extends DialogFragment {

    /**
     * 单首歌
     *
     * @param song 待添加歌曲
     *
     * @return dialog
     */
    @NonNull
    public static AddToPlaylistDialog create(Song song) {
        final List<Song> list = new ArrayList<>();
        list.add(song);
        return create(list);
    }

    /**
     * 多首歌
     *
     * @param songs 待添加
     *
     * @return dialog
     */
    @NonNull
    public static AddToPlaylistDialog create(List<Song> songs) {
        final AddToPlaylistDialog dialog = new AddToPlaylistDialog();
        final Bundle args = new Bundle();
        args.putParcelableArrayList("songs", new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final List<Playlist> playlists = PlaylistLoader.getAllPlaylists(getActivity());
        // 个数为所有播放列表总和 + 1, 多出来的一个用于 NewPlaylist
        final CharSequence[] playlistNames = new CharSequence[playlists.size() + 1];
        playlistNames[0] = getActivity().getResources().getString(R.string.action_new_playlist);

        for (int i = 1; i < playlistNames.length; i++) {
            playlistNames[i] = playlists.get(i - 1).name;
        }
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.add_playlist_title)
                .items(playlistNames)
                .itemsCallback((materialDialog, view, i, charSequence) -> {
                    final List<Song> songs = getArguments().getParcelableArrayList("songs");
                    if (songs == null) return;
                    if (i == 0) {
                        // 新播放列表
                        materialDialog.dismiss();
                        CreatePlaylistDialog.create(songs).show(getActivity().getSupportFragmentManager(), "ADD_TO_PLAYLIST");
                    } else {
                        // 添加到指定列表
                        materialDialog.dismiss();
                        PlaylistsUtil.addToPlaylist(getActivity(), songs, playlists.get(i - 1).id, true);
                    }
                })
                .build();
    }
}
