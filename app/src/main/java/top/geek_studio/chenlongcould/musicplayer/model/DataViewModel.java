package top.geek_studio.chenlongcould.musicplayer.model;

import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import java.util.LinkedList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.model.yuepic.YuePic;

/**
 * ViewModel
 *
 * @author : chenlongcould
 * @date : 2019/09/05/15
 */
public class DataViewModel extends ViewModel {

    /**
     * 对话框(临时) 集合
     */
    public List<DialogInterface> dialogs = new LinkedList<>();

    /**
     * SongsData
     * */
    private MutableLiveData<List<Song>> songsData = new MutableLiveData<>();

    private MutableLiveData<List<Album>> albumsData = new MutableLiveData<>();

    private MutableLiveData<List<Playlist>> playlistsData = new MutableLiveData<>();

    private MutableLiveData<List<Artist>> artistsData = new MutableLiveData<>();

    /**
     * 缓存一言
     * */
    public MutableLiveData<Hitokoto> HitokotoData = new MutableLiveData<>();

//    public MutableLiveData<FirebaseUser> userData = new MutableLiveData<>();

    public MutableLiveData<YuePic> yuePicData = new MutableLiveData<>();

    public MutableLiveData<Boolean> allowUseNetPlayer = new MutableLiveData<>();

    public MutableLiveData<Boolean> allowGetYuePic = new MutableLiveData<>();

    public MutableLiveData<List<String>> lrcData = new MutableLiveData<>();

    //////////////////////// songs /////////////////////////

    public void putSongs(@NonNull List<Song> songs) {
        songsData.postValue(songs);
    }

    @NonNull
    public MutableLiveData<List<Song>> getSongData() {
        return songsData;
    }

    public void setSongsUpdateObs(AppCompatActivity activity, DataUpdateCallback<Song> callback) {
        songsData.observe(activity, callback::onUpdate);
    }

    //////////////////////// songs /////////////////////////

    //////////////////////// album /////////////////////////

    public void putAlbums(@NonNull List<Album> data) {
        albumsData.postValue(data);
    }

    @NonNull
    public MutableLiveData<List<Album>> getAlbumsData() {
        return albumsData;
    }

    public void setAlbumsUpdateObs(AppCompatActivity activity, DataUpdateCallback<Album> callback) {
        albumsData.observe(activity, callback::onUpdate);
    }

    //////////////////////// album /////////////////////////

    //////////////////////// artist /////////////////////////

    public void putArtists(@NonNull List<Artist> data) {
        artistsData.postValue(data);
    }

    @NonNull
    public MutableLiveData<List<Artist>> getArtistsData() {
        return artistsData;
    }

    public void setArtistsUpdateObs(AppCompatActivity activity, DataUpdateCallback<Artist> callback) {
        artistsData.observe(activity, callback::onUpdate);
    }

    //////////////////////// artist /////////////////////////

    //////////////////////// playlist /////////////////////////

    public void putPlaylists(@NonNull List<Playlist> data) {
        playlistsData.postValue(data);
    }

    @NonNull
    public MutableLiveData<List<Playlist>> getPlaylistData() {
        return playlistsData;
    }

    public void setPlaylistsUpdateObs(AppCompatActivity activity, DataUpdateCallback<Playlist> callback) {
        playlistsData.observe(activity, callback::onUpdate);
    }

    //////////////////////// playlist /////////////////////////

    /**
     * 回调
     *
     * @param <T> type
     *
     * @deprecated Use {@link top.geek_studio.chenlongcould.musicplayer.interfaces.TransListDataCallback}
     * */
    @Deprecated
    public interface DataUpdateCallback <T> {
        void onUpdate(List<T> data);
    }
}
