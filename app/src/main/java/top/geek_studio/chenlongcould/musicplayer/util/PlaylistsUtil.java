package top.geek_studio.chenlongcould.musicplayer.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.helper.M3UWriter;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.model.PlaylistSong;
import top.geek_studio.chenlongcould.musicplayer.model.Song;

import static android.provider.MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

/**
 * @author chenlongcould (modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistsUtil {

    /**
     * 通过 id 查播放列表
     *
     * @param context    ctx
     * @param playlistId playlist id
     *
     * @return exists?
     */
    public static boolean doesPlaylistExist(@NonNull final Context context, final int playlistId) {
        return playlistId != -1 && doesPlaylistExist(context,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[]{String.valueOf(playlistId)});
    }

    /**
     * 通过 播放列表名称 查播放列表
     *
     * @param context ctx
     * @param name    playlist name
     *
     * @return exists
     */
    public static boolean doesPlaylistExist(@NonNull final Context context, final String name) {
        return doesPlaylistExist(context,
                MediaStore.Audio.PlaylistsColumns.NAME + "=?",
                new String[]{name});
    }

    private static boolean doesPlaylistExist(@NonNull final Context context, @NonNull final String selection, @NonNull final String[] values) {
        final Cursor cursor = context.getContentResolver().query(EXTERNAL_CONTENT_URI,
                new String[]{}, selection, values, null);

        boolean exists = false;
        if (cursor != null) {
            exists = cursor.getCount() != 0;
            cursor.close();
        }
        return exists;
    }

    /**
     * 创建播放列表
     *
     * @param context ctx
     * @param name    playlist name
     *
     * @return playlist id
     */
    public static int createPlaylist(@NonNull final Context context, @Nullable final String name) {
        int id = -1;
        if (name != null && name.length() > 0) {
            try {
                final Cursor cursor = context.getContentResolver().query(EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Audio.Playlists._ID},
                        MediaStore.Audio.PlaylistsColumns.NAME + "=?", new String[]{name},
                        null);
                if (cursor == null || cursor.getCount() < 1) {
                    final ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
                    final Uri uri = context.getContentResolver().insert(
                            EXTERNAL_CONTENT_URI,
                            values);
                    if (uri != null) {
                        // Necessary because somehow the MediaStoreObserver is not notified when adding a playlist
                        // 这是必要的，因为在添加播放列表时不会以某种方式通知 MediaStoreObserver
                        Toast.makeText(context, context.getResources().getString(
                                R.string.created_playlist_x, name), Toast.LENGTH_SHORT).show();
                        id = Integer.parseInt(uri.getLastPathSegment());
                    }
                } else {
                    // Playlist exists
                    if (cursor.moveToFirst()) {
                        id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
                    }
                }
                if (cursor != null) cursor.close();
            } catch (SecurityException ignored) {
            }
        }

        if (id == -1) {
            Toast.makeText(context, context.getResources().getString(
                    R.string.could_not_create_playlist), Toast.LENGTH_SHORT).show();
        }

        return id;
    }

    /**
     * 移除多个播放列表
     *
     * @param context   ctx
     * @param playlists 播放列表集合
     */
    public static void deletePlaylists(@NonNull final Context context, @NonNull final List<Playlist> playlists) {
        final StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Playlists._ID + " IN (");
        for (int i = 0; i < playlists.size(); i++) {
            selection.append(playlists.get(i).id);
            if (i < playlists.size() - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        try {
            context.getContentResolver().delete(EXTERNAL_CONTENT_URI, selection.toString(), null);
        } catch (SecurityException ignored) {
        }
    }

    /**
     * 添加到 playlist
     *
     * @param context           ctx
     * @param song              song
     * @param playlistId        id
     * @param showToastOnFinish show playlist
     */
    public static void addToPlaylist(@NonNull final Context context, final Song song, final int playlistId, final boolean showToastOnFinish) {
        final List<Song> helperList = new ArrayList<>();
        helperList.add(song);
        addToPlaylist(context, helperList, playlistId, showToastOnFinish);
    }

    /**
     * 添加到 playlist
     *
     * @param context           ctx
     * @param songs             songs
     * @param playlistId        id
     * @param showToastOnFinish show playlist
     */
    public static void addToPlaylist(@NonNull final Context context, @NonNull final List<Song> songs, final int playlistId, final boolean showToastOnFinish) {
        final int size = songs.size();
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[]{
                "max(" + MediaStore.Audio.Playlists.Members.PLAY_ORDER + ")",
        };
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cursor = null;
        int base = 0;

        try {
            try {
                cursor = resolver.query(uri, projection, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    base = cursor.getInt(0) + 1;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            int numInserted = 0;
            for (int offSet = 0; offSet < size; offSet += 1000)
                numInserted += resolver.bulkInsert(uri, makeInsertItems(songs, offSet, 1000, base));

            if (showToastOnFinish) {
                Toast.makeText(context, context.getResources().getString(
                        R.string.inserted_x_songs_into_playlist_x, numInserted, getNameForPlaylist(context, playlistId)), Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException ignored) {
        }
    }

    @NonNull
    private static ContentValues[] makeInsertItems(@NonNull final List<Song> songs, final int offset, int len, final int base) {
        if (offset + len > songs.size()) {
            len = songs.size() - offset;
        }

        final ContentValues[] contentValues = new ContentValues[len];

        for (int i = 0; i < len; i++) {
            contentValues[i] = new ContentValues();
            contentValues[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset + i);
            contentValues[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs.get(offset + i).id);
        }
        return contentValues;
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final Song song, int playlistId) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
                "external", playlistId);
        String selection = MediaStore.Audio.Playlists.Members.AUDIO_ID + " =?";
        String[] selectionArgs = new String[]{String.valueOf(song.id)};

        try {
            context.getContentResolver().delete(uri, selection, selectionArgs);
        } catch (SecurityException ignored) {
        }
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final List<PlaylistSong> songs) {
        final int playlistId = songs.get(0).playlistId;
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
                "external", playlistId);
        String selectionArgs[] = new String[songs.size()];
        for (int i = 0; i < selectionArgs.length; i++) {
            selectionArgs[i] = String.valueOf(songs.get(i).idInPlayList);
        }
        String selection = MediaStore.Audio.Playlists.Members._ID + " in (";
        //noinspection unused
        for (String selectionArg : selectionArgs) selection += "?, ";
        selection = selection.substring(0, selection.length() - 2) + ")";

        try {
            context.getContentResolver().delete(uri, selection, selectionArgs);
        } catch (SecurityException ignored) {
        }
    }

    public static boolean doPlaylistContains(@NonNull final Context context, final long playlistId, final int songId) {
        if (playlistId != -1) {
            try {
                Cursor c = context.getContentResolver().query(
                        MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                        new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID}, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?", new String[]{String.valueOf(songId)}, null);
                int count = 0;
                if (c != null) {
                    count = c.getCount();
                    c.close();
                }
                return count > 0;
            } catch (SecurityException ignored) {
            }
        }
        return false;
    }

    public static boolean moveItem(@NonNull final Context context, int playlistId, int from, int to) {
        return MediaStore.Audio.Playlists.Members.moveItem(context.getContentResolver(),
                playlistId, from, to);
    }

    public static void renamePlaylist(@NonNull final Context context, final long id, final String newName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Audio.PlaylistsColumns.NAME, newName);
        try {
            context.getContentResolver().update(EXTERNAL_CONTENT_URI,
                    contentValues,
                    MediaStore.Audio.Playlists._ID + "=?",
                    new String[]{String.valueOf(id)});
        } catch (SecurityException ignored) {
        }
    }

    public static String getNameForPlaylist(@NonNull final Context context, final long id) {
        try {
            Cursor cursor = context.getContentResolver().query(EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.PlaylistsColumns.NAME},
                    BaseColumns._ID + "=?",
                    new String[]{String.valueOf(id)},
                    null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (SecurityException ignored) {
        }
        return "";
    }

    public static File savePlaylist(Context context, Playlist playlist) throws IOException {
        return M3UWriter.write(context, new File(Environment.getExternalStorageDirectory(), "Playlists"), playlist);
    }
}