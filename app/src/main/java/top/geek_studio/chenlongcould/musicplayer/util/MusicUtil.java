package top.geek_studio.chenlongcould.musicplayer.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.kabouzeid.chenlongcould.musicplayer.R;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistLoader;
import top.geek_studio.chenlongcould.musicplayer.loader.SongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Album;
import top.geek_studio.chenlongcould.musicplayer.model.Artist;
import top.geek_studio.chenlongcould.musicplayer.model.Genre;
import top.geek_studio.chenlongcould.musicplayer.model.NetSearchSong;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.model.lyrics.AbsSynchronizedLyrics;

/**
 * Music Util
 *
 * 音乐工具合集
 *
 * @author chenlongcould (Modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MusicUtil {

    @SuppressWarnings("unused")
    private static final String TAG = "MusicUtil";

    /**
     * 获取专辑图像 Uri
     *
     * @param albumId 专辑 ID
     */
    public static Uri getMediaStoreAlbumCoverUri(int albumId) {
        final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

        return ContentUris.withAppendedId(sArtworkUri, albumId);
    }

    /**
     * 获取歌曲文件 URI
     *
     * @param songId 歌曲 ID
     */
    public static Uri getSongFileUri(int songId) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
    }

    /**
     * 创建分享文件 Intent
     */
    @NonNull
    public static Intent createShareSongFileIntent(@NonNull final Song song, Context context) {
        try {
            return new Intent()
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName(), new File(song.data)))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("audio/*");
        } catch (IllegalArgumentException e) {
            // TODO the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
            e.printStackTrace();
            Toast.makeText(context, "Could not share this file, I'm aware of the issue.", Toast.LENGTH_SHORT).show();
            return new Intent();
        }
    }

    /**
     * 获取艺术家信息
     *
     * @param context ctx
     * @param artist  artist
     *
     * @return info (string type)
     */
    @NonNull
    public static String getArtistInfoString(@NonNull final Context context, @NonNull final Artist artist) {
        int albumCount = artist.getAlbumCount();
        int songCount = artist.getSongCount();

        return MusicUtil.buildInfoString(
                MusicUtil.getAlbumCountString(context, albumCount),
                MusicUtil.getSongCountString(context, songCount)
        );
    }

    /**
     * 获取专辑信息
     *
     * @param context ctx
     * @param album   album
     *
     * @return info
     */
    @NonNull
    public static String getAlbumInfoString(@NonNull final Context context, @NonNull final Album album) {
        int songCount = album.getSongCount();

        return MusicUtil.buildInfoString(
                album.getArtistName(),
                MusicUtil.getSongCountString(context, songCount)
        );
    }

    /**
     * 获取歌曲信息
     *
     * @param song song
     *
     * @return info
     */
    @NonNull
    public static String getSongInfoString(@NonNull final Song song) {
        return MusicUtil.buildInfoString(
                song.artistName,
                song.albumName
        );
    }

    /**
     * 获取歌曲信息
     *
     * @param song song
     *
     * @return info
     */
    @NonNull
    public static String getSongInfoString(@NonNull final NetSearchSong.ResultBean.SongsBean song) {
        return MusicUtil.buildInfoString(
                song.getArtists().get(0).getName(),
                song.getAlbum().getName()
        );
    }

    @NonNull
    public static String getGenreInfoString(@NonNull final Context context, @NonNull final Genre genre) {
        int songCount = genre.songCount;
        return MusicUtil.getSongCountString(context, songCount);
    }

    /**
     * 获取播放列表信息
     *
     * @param context ctx
     * @param songs   songs
     *
     * @return playlist info
     */
    @NonNull
    public static String getPlaylistInfoString(@NonNull final Context context, @NonNull List<Song> songs) {
        final long duration = getTotalDuration(songs);

        return MusicUtil.buildInfoString(
                MusicUtil.getSongCountString(context, songs.size()),
                MusicUtil.getReadableDurationString(duration)
        );
    }

    /**
     * 获取歌曲数量
     * <p>
     * 根据数量来判读是使用 songs 还是 song
     *
     * @param context   ctx
     * @param songCount 歌曲数量
     *
     * @return string
     */
    @NonNull
    public static String getSongCountString(@NonNull final Context context, int songCount) {
        final String songString = songCount == 1 ? context.getResources().getString(R.string.song) : context.getResources().getString(R.string.songs);
        return songCount + " " + songString;
    }

    /**
     * Same as {@link #getSongCountString(Context, int)}
     */
    @NonNull
    public static String getAlbumCountString(@NonNull final Context context, int albumCount) {
        final String albumString = albumCount == 1 ? context.getResources().getString(R.string.album) : context.getResources().getString(R.string.albums);
        return albumCount + " " + albumString;
    }

    /**
     * 获取年份
     *
     * @return string
     */
    @NonNull
    public static String getYearString(int year) {
        return year > 0 ? String.valueOf(year) : "-";
    }

    /**
     * 获取指定歌曲总长度
     *
     * @param songs 歌曲
     */
    public static long getTotalDuration(@NonNull List<Song> songs) {
        long duration = 0;
        for (int i = 0; i < songs.size(); i++) {
            duration += songs.get(i).duration;
        }
        return duration;
    }

    /**
     * 获取可读的音乐时长
     *
     * @param songDurationMillis 音乐长度
     *
     * @return 长度 (string)
     */
    public static String getReadableDurationString(long songDurationMillis) {
        long minutes = (songDurationMillis / 1000) / 60;
        long seconds = (songDurationMillis / 1000) % 60;
        if (minutes < 60) {
            return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds);
        } else {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    /**
     * 根据提供的参数构建连接字符串预期目的是显示音乐库项目的额外注释。 例如：对于给定的专辑 - >
     * buildInfoString（album.artist，album.songCount）
     * <p>
     * Build a concatenated string from the provided arguments
     * The intended purpose is to show extra annotations
     * to a music library item.
     * Ex: for a given album --> buildInfoString(album.artist, album.songCount)
     * <p>
     * like ACG-Player-X • ACG-Player-X
     */
    @NonNull
    public static String buildInfoString(@Nullable final String string1, @Nullable final String string2) {
        // Skip empty strings
        if (TextUtils.isEmpty(string1)) {
            return TextUtils.isEmpty(string2) ? "" : string2;
        }
        if (TextUtils.isEmpty(string2)) {
            return TextUtils.isEmpty(string1) ? "" : string1;
        }

        return string1 + "  •  " + string2;
    }

    //iTunes uses for example 1002 for track ic_launcher CD1 or 3011 for track 11 CD3.
    //this method converts those values to normal tracknumbers
    public static int getFixedTrackNumber(int trackNumberToFix) {
        return trackNumberToFix % 1000;
    }

    /**
     * 插入专辑图
     *
     * @param context ctx
     * @param albumId albumId
     * @param path    path
     */
    public static void insertAlbumArt(@NonNull Context context, int albumId, String path) {
        final ContentResolver contentResolver = context.getContentResolver();

        final Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null);

        final ContentValues values = new ContentValues();
        values.put("album_id", albumId);
        values.put("_data", path);

        contentResolver.insert(artworkUri, values);
    }

    /**
     * 删除专辑图
     *
     * @param context ctx
     * @param albumId albumId
     */
    public static void deleteAlbumArt(@NonNull Context context, int albumId) {
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri localUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(localUri, albumId), null, null);
    }

    /**
     * 创建专辑文件
     */
    @NonNull
    public static File createAlbumArtFile() {
        return new File(createAlbumArtDir(), String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 专辑封面目录
     */
    @NonNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File createAlbumArtDir() {
        // FIXME: fix on Android Q
        File albumArtDir = new File(Environment.getExternalStorageDirectory(), "/albumthumbs/");
        if (!albumArtDir.exists()) {
            albumArtDir.mkdirs();
            try {
                new File(albumArtDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return albumArtDir;
    }

    /**
     * 删除歌曲
     *
     * @param context ctx
     * @param songs   被删除歌曲
     */
    public static void deleteTracks(@NonNull final Context context, @NonNull final List<Song> songs) {
        final String[] projection = new String[]{
                // FIXME: fix on Android Q
                BaseColumns._ID, MediaStore.MediaColumns.DATA
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < songs.size(); i++) {

            // 计入选择区
            selection.append(songs.get(i).id);
            if (i < songs.size() - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        try {
            final Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(),
                    null, null);
            if (cursor != null) {
                // Step 1: Remove selected tracks from the current playlist, as well
                // as from the album art cache
                // 从当前播放列表中删除所选曲目，如同从专辑封面缓存中删除
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    final int id = cursor.getInt(0);
                    final Song song = SongLoader.getSong(context, id);

                    // 从队列中移除
                    MusicPlayerRemote.removeFromQueue(song);
                    cursor.moveToNext();
                }

                // Step ic_launcher: Remove selected tracks from the database
                // 从数据库中删除选定的曲目
                context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        selection.toString(), null);

                // Step 3: Remove files from sdcard
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    final String name = cursor.getString(1);
                    try { // File.delete can throw a security exception
                        final File f = new File(name);
                        if (!f.delete()) {
                            // I'm not sure if we'd ever get4LastFM here (deletion would
                            // have to fail, but no exception thrown)
                            Log.e("MusicUtils", "Failed to delete file " + name);
                        }
                        cursor.moveToNext();
                    } catch (@NonNull final SecurityException ex) {
                        cursor.moveToNext();
                    } catch (NullPointerException e) {
                        Log.e("MusicUtils", "Failed to find file " + name);
                    }
                }
                cursor.close();
            }
            context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
            Toast.makeText(context, context.getString(R.string.deleted_x_songs, songs.size()), Toast.LENGTH_SHORT).show();
        } catch (SecurityException ignored) {
            // ...
        }
    }

    /**
     * 是否为喜爱列表
     *
     * @param context  ctx
     * @param playlist 播放列表
     */
    public static boolean isFavoritePlaylist(@NonNull final Context context, @NonNull final Playlist playlist) {
        return playlist.name != null && playlist.name.equals(context.getString(R.string.favorites));
    }

    /**
     * 获取喜爱列表
     *
     * @param context ctx
     *
     * @return playlist
     */
    private static Playlist getFavoritesPlaylist(@NonNull final Context context) {
        return PlaylistLoader.getPlaylist(context, context.getString(R.string.favorites));
    }

    /**
     * 获取(创建) 喜爱列表
     *
     * @return playlist
     */
    private static Playlist getOrCreateFavoritesPlaylist(@NonNull final Context context) {
        return PlaylistLoader.getPlaylist(context, PlaylistsUtil.createPlaylist(context, context.getString(R.string.favorites)));
    }

    /**
     * 指定歌曲是否为喜爱歌曲
     *
     * @param context ctx
     * @param song    song
     *
     * @return bool
     */
    public static boolean isFavorite(@NonNull final Context context, @NonNull final Song song) {
        return PlaylistsUtil.doPlaylistContains(context, getFavoritesPlaylist(context).id, song.id);
    }

    /**
     * 切换歌曲是否为喜爱
     *
     * @param song    target song
     * @param context ctx
     */
    public static void toggleFavorite(@NonNull final Context context, @NonNull final Song song) {
        if (isFavorite(context, song)) {
            PlaylistsUtil.removeFromPlaylist(context, song, getFavoritesPlaylist(context).id);
        } else {
            PlaylistsUtil.addToPlaylist(context, song, getOrCreateFavoritesPlaylist(context).id, false);
        }
    }

    /**
     * 是否为位置艺术家
     * <p>
     * 用于检测元数据不完整歌曲, 或某些看似是歌曲的 mp3 文件
     *
     * @param artistName artist
     *
     * @return bool
     */
    public static boolean isArtistNameUnknown(@Nullable String artistName) {
        if (TextUtils.isEmpty(artistName)) return false;
        if (artistName.equals(Artist.UNKNOWN_ARTIST_DISPLAY_NAME)) return true;
        artistName = artistName.trim().toLowerCase();
        return artistName.equals("unknown") || artistName.equals("<unknown>");
    }

    @NonNull
    public static String getSectionName(@Nullable String musicMediaTitle) {
        if (TextUtils.isEmpty(musicMediaTitle)) return "";
        musicMediaTitle = musicMediaTitle.trim().toLowerCase();
        if (musicMediaTitle.startsWith("the ")) {
            musicMediaTitle = musicMediaTitle.substring(4);
        } else if (musicMediaTitle.startsWith("a ")) {
            musicMediaTitle = musicMediaTitle.substring(2);
        }
        if (musicMediaTitle.isEmpty()) return "";
        return String.valueOf(musicMediaTitle.charAt(0)).toUpperCase();
    }

    /**
     * 获取歌词
     *
     * @param song song
     *
     * @return 歌词
     */
    @Nullable
    public static String getLyrics(Song song) {
        String lyrics = null;

        File file = new File(song.data);

        try {
            lyrics = AudioFileIO.read(file).getTagOrCreateDefault().getFirst(FieldKey.LYRICS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (lyrics == null || lyrics.trim().isEmpty() || !AbsSynchronizedLyrics.isSynchronized(lyrics)) {
            File dir = file.getAbsoluteFile().getParentFile();

            if (dir != null && dir.exists() && dir.isDirectory()) {
                String format = ".*%s.*\\.(lrc|txt)";
                String s = FileUtil.stripExtension(file.getName());
                if (s == null) return "";
                String filename = Pattern.quote(s);
                String songtitle = Pattern.quote(song.title);

                final List<Pattern> patterns = new ArrayList<>();
                patterns.add(Pattern.compile(String.format(format, filename), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                patterns.add(Pattern.compile(String.format(format, songtitle), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

                File[] files = dir.listFiles(f -> {
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(f.getName()).matches()) return true;
                    }
                    return false;
                });

                if (files != null && files.length > 0) {
                    for (File f : files) {
                        try {
                            String newLyrics = FileUtil.read(f);
                            if (newLyrics != null && !newLyrics.trim().isEmpty()) {
                                if (AbsSynchronizedLyrics.isSynchronized(newLyrics)) {
                                    return newLyrics;
                                }
                                lyrics = newLyrics;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return lyrics;
    }
}
