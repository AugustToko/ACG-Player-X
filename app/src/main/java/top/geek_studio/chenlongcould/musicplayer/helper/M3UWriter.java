package top.geek_studio.chenlongcould.musicplayer.helper;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistSongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.AbsCustomPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.model.Song;

/**
 * M3U 工具类
 */
public class M3UWriter implements M3UConstants {

    /**
     * 写入 M3U 文件
     *
     * @param context  ctx
     * @param playlist playlist
     * @param dir      存储路径
     *
     * @return M3U file
     */
    public static File write(final Context context, final File dir, final Playlist playlist) throws IOException {
        if (!dir.exists())
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();

        // 创建 m3u 文件
        final File file = new File(dir, playlist.name.concat("." + EXTENSION));

        // 提取歌曲
        List<? extends Song> songs;
        if (playlist instanceof AbsCustomPlaylist) {
            songs = ((AbsCustomPlaylist) playlist).getSongs(context);
        } else {
            songs = PlaylistSongLoader.getPlaylistSongList(context, playlist.id);
        }

        if (songs.size() > 0) {
            final BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            // 头字符
            bw.write(HEADER);

            // data
            for (final Song song : songs) {
                bw.newLine();
                bw.write(ENTRY + song.duration + DURATION_SEPARATOR + song.artistName + " - " + song.title);
                bw.newLine();
                bw.write(song.data);
            }

            bw.close();
        }

        return file;
    }
}
