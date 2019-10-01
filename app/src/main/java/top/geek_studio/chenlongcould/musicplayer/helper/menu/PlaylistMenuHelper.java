package top.geek_studio.chenlongcould.musicplayer.helper.menu;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import top.geek_studio.chenlongcould.musicplayer.dialogs.AddToPlaylistDialog;
import top.geek_studio.chenlongcould.musicplayer.dialogs.DeletePlaylistDialog;
import top.geek_studio.chenlongcould.musicplayer.dialogs.RenamePlaylistDialog;
import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistSongLoader;
import top.geek_studio.chenlongcould.musicplayer.App;
import com.kabouzeid.chenlongcould.musicplayer.R;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.misc.WeakContextAsyncTask;
import top.geek_studio.chenlongcould.musicplayer.model.AbsCustomPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.util.PlaylistsUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于播放列表 menu
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistMenuHelper {
    public static boolean handleMenuClick(@NonNull AppCompatActivity activity, @NonNull final Playlist playlist, @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play:
                MusicPlayerRemote.openQueue(new ArrayList<>(getPlaylistSongs(activity, playlist)), 0, true);
                return true;
            case R.id.action_play_next:
                MusicPlayerRemote.playNext(new ArrayList<>(getPlaylistSongs(activity, playlist)));
                return true;
            case R.id.action_add_to_current_playing:
                MusicPlayerRemote.enqueue(new ArrayList<>(getPlaylistSongs(activity, playlist)));
                return true;
            case R.id.action_add_to_playlist:
                AddToPlaylistDialog.create(new ArrayList<>(getPlaylistSongs(activity, playlist))).show(activity.getSupportFragmentManager(), "ADD_PLAYLIST");
                return true;
            case R.id.action_rename_playlist:
                RenamePlaylistDialog.create(playlist.id).show(activity.getSupportFragmentManager(), "RENAME_PLAYLIST");
                return true;
            case R.id.action_delete_playlist:
                DeletePlaylistDialog.create(playlist).show(activity.getSupportFragmentManager(), "DELETE_PLAYLIST");
                return true;
            case R.id.action_save_playlist:
                new SavePlaylistAsyncTask(activity).execute(playlist);
                return true;
        }
        return false;
    }

    @NonNull
    private static List<? extends Song> getPlaylistSongs(@NonNull Activity activity, Playlist playlist) {
        return playlist instanceof AbsCustomPlaylist ?
                ((AbsCustomPlaylist) playlist).getSongs(activity) :
                PlaylistSongLoader.getPlaylistSongList(activity, playlist.id);
    }


    private static class SavePlaylistAsyncTask extends WeakContextAsyncTask<Playlist, String, String> {
        public SavePlaylistAsyncTask(Context context) {
            super(context);
        }

        @Override
        protected String doInBackground(Playlist... params) {
            try {
                return String.format(App.Companion.getInstance().getApplicationContext().getString(R.string.saved_playlist_to), PlaylistsUtil.savePlaylist(App.Companion.getInstance().getApplicationContext(), params[0]));
            } catch (IOException e) {
                e.printStackTrace();
                return String.format(App.Companion.getInstance().getApplicationContext().getString(R.string.failed_to_save_playlist), e);
            }
        }

        @Override
        protected void onPostExecute(String string) {
            super.onPostExecute(string);
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show();
            }
        }
    }
}
