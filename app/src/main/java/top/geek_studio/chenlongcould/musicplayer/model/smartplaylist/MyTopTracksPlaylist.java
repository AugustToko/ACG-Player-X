package top.geek_studio.chenlongcould.musicplayer.model.smartplaylist;

import android.content.Context;
import android.os.Parcel;
import androidx.annotation.NonNull;

import top.geek_studio.chenlongcould.musicplayer.loader.TopAndRecentlyPlayedTracksLoader;
import top.geek_studio.chenlongcould.musicplayer.provider.SongPlayCountStore;
import com.kabouzeid.chenlongcould.musicplayer.R;
import top.geek_studio.chenlongcould.musicplayer.model.Song;

import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MyTopTracksPlaylist extends AbsSmartPlaylist {

    public MyTopTracksPlaylist(@NonNull Context context) {
        super(context.getString(R.string.my_top_tracks), R.drawable.ic_trending_up_white_24dp);
    }

    @NonNull
    @Override
    public List<Song> getSongs(@NonNull Context context) {
        return TopAndRecentlyPlayedTracksLoader.getTopTracks(context);
    }

    @Override
    public void clear(@NonNull Context context) {
        SongPlayCountStore.getInstance(context).clear();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    protected MyTopTracksPlaylist(Parcel in) {
        super(in);
    }

    public static final Creator<MyTopTracksPlaylist> CREATOR = new Creator<MyTopTracksPlaylist>() {
        public MyTopTracksPlaylist createFromParcel(Parcel source) {
            return new MyTopTracksPlaylist(source);
        }

        public MyTopTracksPlaylist[] newArray(int size) {
            return new MyTopTracksPlaylist[size];
        }
    };
}
