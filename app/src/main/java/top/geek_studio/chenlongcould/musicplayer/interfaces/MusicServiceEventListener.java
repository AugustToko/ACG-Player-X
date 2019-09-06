package top.geek_studio.chenlongcould.musicplayer.interfaces;

/**
 * 服务事件监听
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public interface MusicServiceEventListener {

    /**
     * 服务连接完成
     */
    void onServiceConnected();

    /**
     * 服务断开
     */
    void onServiceDisconnected();

    /**
     * 队列改变回调
     */
    void onQueueChanged();

    /**
     * 元数据改变回调
     * */
    void onPlayingMetaChanged();

    /**
     * 播放状态改变回调
     * */
    void onPlayStateChanged();

    /**
     * 重复模式改变
     * */
    void onRepeatModeChanged();

    /**
     * 随机模式改变
     * */
    void onShuffleModeChanged();

    /**
     * 媒体存储改变
     * */
    void onMediaStoreChanged();
}
