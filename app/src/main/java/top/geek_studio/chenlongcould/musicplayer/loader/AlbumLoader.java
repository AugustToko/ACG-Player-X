package top.geek_studio.chenlongcould.musicplayer.loader;

import android.content.Context;
import android.provider.MediaStore.Audio.AudioColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.model.Album;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * 专辑加载器
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AlbumLoader {

    /**
     * 获取专辑下歌曲排序
     *
     * @return sortOrder
     */
    private static String getSongLoaderSortOrder(Context context) {
        return PreferenceUtil.getInstance(context).getAlbumSortOrder() + ", " + PreferenceUtil.getInstance(context).getAlbumSongSortOrder();
    }

    /**
     * 获取所有专辑
     *
     * @param context ctx
     *
     * @return 专辑 (List)
     */
    @NonNull
    public static List<Album> getAllAlbums(@NonNull final Context context) {
        final List<Song> songs = SongLoader.getSongs(SongLoader.makeSongCursor(
                context,
                null,
                null,
                getSongLoaderSortOrder(context))
        );
        return splitIntoAlbums(songs);
    }

    /**
     * 获取专辑
     *
     * @param context ctx
     * @param query   查询
     *
     * @return 专辑 (List)
     */
    @NonNull
    public static List<Album> getAlbums(@NonNull final Context context, String query) {
        final List<Song> songs = SongLoader.getSongs(SongLoader.makeSongCursor(
                context,
                AudioColumns.ALBUM + " LIKE ?",
                new String[]{"%" + query + "%"},
                getSongLoaderSortOrder(context))
        );
        return splitIntoAlbums(songs);
    }

    /**
     * 获取专辑
     *
     * @param context ctx
     * @param albumId 专辑 ID
     *
     * @return 专辑
     */
    @NonNull
    public static Album getAlbum(@NonNull final Context context, int albumId) {
        // 获取歌曲
        final List<Song> songs = SongLoader.getSongs(SongLoader.makeSongCursor(context, AudioColumns.ALBUM_ID + "=?", new String[]{String.valueOf(albumId)}, getSongLoaderSortOrder(context)));
        final Album album = new Album(songs);
        sortSongsByTrackNumber(album);
        return album;
    }

    /**
     * 获取或创建一个专辑
     *
     * @param albums 专辑
     * @param albumId 专辑id
     *
     * @return 专辑
     * */
    private static Album getOrCreateAlbum(final List<Album> albums, final int albumId) {
        for (final Album album : albums) {
            if (!album.songs.isEmpty() && album.songs.get(0).albumId == albumId) {
                return album;
            }
        }
        final Album album = new Album();
        albums.add(album);
        return album;
    }

    /**
     * 将歌曲筛选成专辑
     *
     * @param songs 歌曲
     *
     * @return 专辑 (List)
     */
    @NonNull
    public static List<Album> splitIntoAlbums(@Nullable final List<Song> songs) {
        final List<Album> albums = new ArrayList<>();
        if (songs != null) {
            for (final Song song : songs) {
                getOrCreateAlbum(albums, song.albumId).songs.add(song);
            }
        }
        for (final Album album : albums) {
            sortSongsByTrackNumber(album);
        }
        return albums;
    }

    /**
     * 根据轨道 ID 将专辑内的歌曲排序
     *
     * @param album 专辑
     * */
    private static void sortSongsByTrackNumber(final Album album) {
        Collections.sort(album.songs, (o1, o2) -> o1.trackNumber - o2.trackNumber);
    }
}
