package top.geek_studio.chenlongcould.musicplayer.service.playback;

import android.media.MediaPlayer;

import androidx.annotation.Nullable;

/**
 * 播放回调
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public interface Playback {

    /** 设置数据源 */
    boolean setDataSource(String path);

    /** 设置下个数据源 */
    void setNextDataSource(@Nullable String path);

    /** 设置播放回调 */
    void setCallbacks(PlaybackCallbacks callbacks);

    /** 是否初始化完成 */
    boolean isInitialized();

    /** 开始播放 */
    boolean start();

    /** 停止播放 */
    void stop();

    /** 释放 media player {@link MediaPlayer#release()} */
    void release();

    /** 暂停 */
    boolean pause();

    /** 是否正在播放 */
    boolean isPlaying();

    /** 媒体长度 */
    int duration();

    /** 媒体位置 */
    int position();

    /** 媒体进度跳转 */
    int seek(int whereto);

    /** 设置音量 */
    boolean setVolume(float vol);

    /** 设置媒体会话 ID */
    boolean setAudioSessionId(int sessionId);

    /** 获得媒体会话 ID */
    int getAudioSessionId();

    /**
     * 播放回调
     */
    interface PlaybackCallbacks {
        void onTrackWentToNext();

        void onTrackEnded();
    }
}
