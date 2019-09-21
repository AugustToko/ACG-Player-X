package top.geek_studio.chenlongcould.musicplayer.helper;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.loader.SongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Song;

/**
 * 歌曲查找帮助类
 *
 * @author Karim Abou Zeid (kabouzeid), chenlongcould (add notes)
 */
public class SearchQueryHelper {
    private static final String TITLE_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.TITLE + ") = ?";
    private static final String ALBUM_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ALBUM + ") = ?";
    private static final String ARTIST_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ARTIST + ") = ?";
    private static final String AND = " AND ";

    /**
     * 根据给定数据寻找歌曲
     *
     * @param context ctx
     * @param extras  给定数据 {@link MediaStore#EXTRA_MEDIA_ARTIST} ,
     *                {@link MediaStore#EXTRA_MEDIA_ALBUM},
     *                {@link MediaStore#EXTRA_MEDIA_ARTIST}
     *                {@link MediaStore#EXTRA_MEDIA_TITLE}
     *
     * @see SongLoader#makeSongCursor(Context, String, String[])
     * @see SongLoader#getSong(Cursor)
     */
    @NonNull
    public static List<Song> getSongs(@NonNull final Context context, @NonNull final Bundle extras) {
        final String query = extras.getString(SearchManager.QUERY, null);
        final String artistName = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null);
        final String albumName = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null);
        final String titleName = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null);

        List<Song> songs = new ArrayList<>();

        if (artistName != null && albumName != null && titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST_SELECTION + AND + ALBUM_SELECTION + AND + TITLE_SELECTION, new String[]{artistName.toLowerCase().trim(), albumName.toLowerCase().trim(), titleName.toLowerCase().trim()}));
        }

        if (!songs.isEmpty()) {
            return songs;
        }

        //////////////////////////////

        if (artistName != null && titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST_SELECTION + AND + TITLE_SELECTION, new String[]{artistName.toLowerCase().trim(), titleName.toLowerCase().trim()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        /////////////////////////////

        if (albumName != null && titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ALBUM_SELECTION + AND + TITLE_SELECTION, new String[]{albumName.toLowerCase().trim(), titleName.toLowerCase().trim()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        /////////////////////////////

        if (artistName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST_SELECTION, new String[]{artistName.toLowerCase().trim()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        ////////////////////////////

        if (albumName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ALBUM_SELECTION, new String[]{albumName.toLowerCase().trim()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        ///////////////////////////

        if (titleName != null) {
            songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, TITLE_SELECTION, new String[]{titleName.toLowerCase().trim()}));
        }
        if (!songs.isEmpty()) {
            return songs;
        }

        ///////////////////////////

        songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ARTIST_SELECTION, new String[]{query.toLowerCase().trim()}));
        if (!songs.isEmpty()) {
            return songs;
        }

        ///////////////////////////

        songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, ALBUM_SELECTION, new String[]{query.toLowerCase().trim()}));
        if (!songs.isEmpty()) {
            return songs;
        }

        ///////////////////////////

        songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, TITLE_SELECTION, new String[]{query.toLowerCase().trim()}));
        if (!songs.isEmpty()) {
            return songs;
        }

        return SongLoader.getSongs(context, query);
    }
}
