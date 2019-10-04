package top.geek_studio.chenlongcould.musicplayer.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class Playlist implements Parcelable {
    private static final String TAG = Playlist.class.getSimpleName();

    public final int id;
    public final String name;
    public final long modifyDate;

    public Playlist(final int id, final String name, final long modifyDate) {
        this.id = id;
        this.name = name;
        this.modifyDate = modifyDate;

//        Log.d(TAG, "Playlist: date : " + new Date(modifyDate));
    }

    public Playlist() {
        this.id = -1;
        this.name = "";
        this.modifyDate = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Playlist playlist = (Playlist) o;

        if (id != playlist.id) return false;
        return name != null ? name.equals(playlist.name) : playlist.name == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @NotNull
    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeLong(this.modifyDate);
    }

    protected Playlist(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.modifyDate = in.readLong();
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        public Playlist createFromParcel(Parcel source) {
            return new Playlist(source);
        }

        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };
}
