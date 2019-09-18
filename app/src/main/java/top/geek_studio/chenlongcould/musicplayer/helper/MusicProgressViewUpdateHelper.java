package top.geek_studio.chenlongcould.musicplayer.helper;

import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;

/**
 * 进度条
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MusicProgressViewUpdateHelper extends Handler {
    private static final int CMD_REFRESH_PROGRESS_VIEWS = 1;

    private static final int MIN_INTERVAL = 20;
    private static final int UPDATE_INTERVAL_PLAYING = 1000;
    private static final int UPDATE_INTERVAL_PAUSED = 500;

    /**
     * 回调
     */
    private Callback callback;

    private int intervalPlaying;

    private int intervalPaused;

    /**
     * 构造
     *
     * @param callback 回调
     */
    public MusicProgressViewUpdateHelper(Callback callback) {
        this.callback = callback;
        this.intervalPlaying = UPDATE_INTERVAL_PLAYING;
        this.intervalPaused = UPDATE_INTERVAL_PAUSED;
    }

    /**
     * 构造
     *
     * @param callback 回调
     */
    public MusicProgressViewUpdateHelper(Callback callback, int intervalPlaying, int intervalPaused) {
        this.callback = callback;
        this.intervalPlaying = intervalPlaying;
        this.intervalPaused = intervalPaused;
    }

    public void start() {
        queueNextRefresh(1);
    }

    public void stop() {
        removeMessages(CMD_REFRESH_PROGRESS_VIEWS);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if (msg.what == CMD_REFRESH_PROGRESS_VIEWS) {
            queueNextRefresh(refreshProgressViews());
        }
    }

    /**
     * 刷新 view
     * */
    private int refreshProgressViews() {
        final int progressMillis = MusicPlayerRemote.getSongProgressMillis();
        final int totalMillis = MusicPlayerRemote.getSongDurationMillis();

        callback.onUpdateProgressViews(progressMillis, totalMillis);

        if (!MusicPlayerRemote.isPlaying()) {
            return intervalPaused;
        }

        final int remainingMillis = intervalPlaying - progressMillis % intervalPlaying;

        return Math.max(MIN_INTERVAL, remainingMillis);
    }

    private void queueNextRefresh(final long delay) {
        final Message message = obtainMessage(CMD_REFRESH_PROGRESS_VIEWS);
        removeMessages(CMD_REFRESH_PROGRESS_VIEWS);
        sendMessageDelayed(message, delay);
    }

    /**
     * 回调
     * */
    public interface Callback {
        void onUpdateProgressViews(int progress, int total);
    }
}
