package top.geek_studio.chenlongcould.musicplayer.helper;

import androidx.annotation.NonNull;

import top.geek_studio.chenlongcould.musicplayer.model.Song;

import java.util.Collections;
import java.util.List;

/**
 * 随机播放帮助类
 *
 * @author chenlongcould (Modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ShuffleHelper {

    /**
     * 随机列表
     *
     * @param listToShuffle 列表
     * @param current       当前歌曲 index
     */
    public static void makeShuffleList(@NonNull List<Song> listToShuffle, final int current) {
        if (listToShuffle.isEmpty()) return;
        if (current >= 0) {
            // 删除并获取当前歌曲
            final Song song = listToShuffle.remove(current);
            // 随机
            Collections.shuffle(listToShuffle);
            // 添加被删除歌曲到 0
            listToShuffle.add(0, song);
        } else {
            Collections.shuffle(listToShuffle);
        }
    }
}
