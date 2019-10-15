package top.geek_studio.chenlongcould.musicplayer.helper.menu;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

import top.geek_studio.chenlongcould.musicplayer.dialogs.AddToPlaylistDialog;
import top.geek_studio.chenlongcould.musicplayer.dialogs.DeleteSongsDialog;
import top.geek_studio.chenlongcould.musicplayer.dialogs.SongDetailDialog;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.interfaces.PaletteColorHolder;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.tageditor.AbsTagEditorActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.tageditor.SongTagEditorActivity;
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil;
import top.geek_studio.chenlongcould.musicplayer.util.NavigationUtil;
import top.geek_studio.chenlongcould.musicplayer.util.RingtoneManager;

/**
 * 用于歌曲 menu
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongMenuHelper {

    /**
     * For {@link top.geek_studio.chenlongcould.musicplayer.adapter.song.SongAdapter}
     */
    public static final int MENU_RES = R.menu.menu_item_song;

    /**
     * For {@link top.geek_studio.chenlongcould.musicplayer.adapter.song.NetSearchSongAdapter}
     */
    public static final int NET_MENU_RES = R.menu.menu_item_net_song;

    /**
     * 检测匹配到的 menuItem
     *
     * @param activity   act
     * @param song       song
     * @param menuItemId id
     *
     * @return bool
     */
    public static boolean handleMenuClick(@NonNull FragmentActivity activity, @NonNull Song song, int menuItemId) {
        switch (menuItemId) {
            case R.id.action_set_as_ringtone:
                if (RingtoneManager.requiresDialog(activity)) {
                    RingtoneManager.showDialog(activity);
                } else {
                    RingtoneManager ringtoneManager = new RingtoneManager();
                    ringtoneManager.setRingtone(activity, song.id);
                }
                return true;
            case R.id.action_share:
                activity.startActivity(Intent.createChooser(MusicUtil.createShareSongFileIntent(song, activity), null));
                return true;
            case R.id.action_delete_from_device:
                DeleteSongsDialog.create(song).show(activity.getSupportFragmentManager(), "DELETE_SONGS");
                return true;
            case R.id.action_add_to_playlist:
                AddToPlaylistDialog.create(song).show(activity.getSupportFragmentManager(), "ADD_PLAYLIST");
                return true;
            case R.id.action_play_next:
                MusicPlayerRemote.playNext(song);
                return true;
            case R.id.action_add_to_current_playing:
                MusicPlayerRemote.enqueue(song);
                return true;
            case R.id.action_tag_editor:
                Intent tagEditorIntent = new Intent(activity, SongTagEditorActivity.class);
                tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id);
                if (activity instanceof PaletteColorHolder)
                    tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_PALETTE, ((PaletteColorHolder) activity).getPaletteColor());
                activity.startActivity(tagEditorIntent);
                return true;
            case R.id.action_details:
                SongDetailDialog.Companion.create(song).show(activity.getSupportFragmentManager(), "SONG_DETAILS");
                return true;
            case R.id.action_go_to_album:
                NavigationUtil.goToAlbum(activity, song.albumId);
                return true;
            case R.id.action_go_to_artist:
                NavigationUtil.goToArtist(activity, song.artistId);
                return true;
        }
        return false;
    }

    public static abstract class OnClickSongMenu implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        private AppCompatActivity activity;

        public OnClickSongMenu(@NonNull AppCompatActivity activity) {
            this.activity = activity;
        }

        public int getMenuRes() {
            return MENU_RES;
        }

        @Override
        public void onClick(View v) {
            PopupMenu popupMenu = new PopupMenu(activity, v);
            popupMenu.inflate(getMenuRes());
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return handleMenuClick(activity, getSong(), item.getItemId());
        }

        public abstract Song getSong();
    }
}
