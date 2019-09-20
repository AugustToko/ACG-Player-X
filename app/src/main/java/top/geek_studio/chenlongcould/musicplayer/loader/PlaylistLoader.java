package top.geek_studio.chenlongcould.musicplayer.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.PlaylistsColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.model.Playlist;

/**
 * 播放列表加载器
 */
public class PlaylistLoader {

    /**
     * 获取全部播放列表
     *
     * @param context ctx
     *
     * @return playlist
     */
    @NonNull
    public static List<Playlist> getAllPlaylists(@NonNull final Context context) {
        return getAllPlaylists(makePlaylistCursor(context, null, null));
    }

    /**
     * 通过 ID 获取播放列表
     *
     * @param context    ctx
     * @param playlistId playlist ID
     *
     * @return playlist
     */
    @NonNull
    public static Playlist getPlaylist(@NonNull final Context context, final int playlistId) {
        return getPlaylist(makePlaylistCursor(
                context,
                BaseColumns._ID + "=?",
                new String[]{
                        String.valueOf(playlistId)
                }
        ));
    }

    /**
     * 获取指定名称播放列表
     *
     * @param context      ctx
     * @param playlistName 播放列表名称
     *
     * @return playlist
     */
    @NonNull
    public static Playlist getPlaylist(@NonNull final Context context, final String playlistName) {
        return getPlaylist(makePlaylistCursor(
                context,
                PlaylistsColumns.NAME + "=?",
                new String[]{
                        playlistName
                }
        ));
    }

    /**
     * 通过 Cursor 获取播放列表
     *
     * @param cursor cursor
     *
     * @return playlist
     */
    @NonNull
    private static Playlist getPlaylist(@Nullable final Cursor cursor) {
        Playlist playlist = new Playlist();

        if (cursor != null && cursor.moveToFirst()) {
            playlist = getPlaylistFromCursorImpl(cursor);
        }
        if (cursor != null)
            cursor.close();
        return playlist;
    }

    /**
     * 获取全部播放列表
     *
     * @param cursor cursor
     *
     * @return 播放列表 (List)
     */
    @NonNull
    private static List<Playlist> getAllPlaylists(@Nullable final Cursor cursor) {
        final List<Playlist> playlists = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // ADD TO LIST
                playlists.add(getPlaylistFromCursorImpl(cursor));
            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        return playlists;
    }

    /**
     * 从 Cursor 获取播放列表
     *
     * @param cursor cursor
     *
     * @return playlist
     *
     * @see #makePlaylistCursor(Context, String, String[])
     */
    @NonNull
    private static Playlist getPlaylistFromCursorImpl(@NonNull final Cursor cursor) {
        // playlist id
        final int id = cursor.getInt(0);
        // playlist name
        final String name = cursor.getString(1);
        return new Playlist(id, name);
    }

    /**
     * Playlist cursor
     *
     * @param context   ctx
     * @param selection selection
     * @param values    val
     *
     * @return cursor
     */
    @Nullable
    private static Cursor makePlaylistCursor(@NonNull final Context context, final String selection, final String[] values) {
        try {
            return context.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    new String[]{
                            /* 0 */
                            BaseColumns._ID,
                            /* 1 */
                            PlaylistsColumns.NAME
                    }, selection, values, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
        } catch (SecurityException e) {
            return null;
        }
    }
}