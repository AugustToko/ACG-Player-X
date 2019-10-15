package top.geek_studio.chenlongcould.musicplayer.service.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

import top.geek_studio.chenlongcould.musicplayer.service.MusicService;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * 抽象通知
 */
public abstract class PlayingNotification {

    private static final int NOTIFICATION_ID = 1;

    static final String NOTIFICATION_CHANNEL_ID = "playing_notification";

    /** 前台通知 */
    private static final int NOTIFY_MODE_FOREGROUND = 1;

    /** 后台通知 */
    private static final int NOTIFY_MODE_BACKGROUND = 0;

    /**
     * 通知模式
     */
    private int notifyMode = NOTIFY_MODE_BACKGROUND;

    private NotificationManager notificationManager;

    /**
     * 存储服务引用
     */
    protected MusicService service;

    boolean stopped;

    /**
     * 初始化
     *
     * @param service MusicService
     */
    public synchronized void init(MusicService service) {
        this.service = service;
        notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        // Android 8.0 以上需创建通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    /**
     * 用于更新通知 (制作 {@link Notification}, 然后交给 {@link #updateNotifyModeAndPostNotification(Notification)} 发送)
     * <p>
     * 子类实现细节
     */
    public abstract void update();

    /**
     * 停止通知
     */
    public synchronized void stop() {
        stopped = true;
        service.stopForeground(true);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * 更新通知模式和推送通知
     *
     * @param notification 通知
     */
    @SuppressWarnings("ConstantConditions")
    void updateNotifyModeAndPostNotification(final Notification notification) {
        // 推断通知模式
        int newNotifyMode;
        if (service.isPlaying()) {
            newNotifyMode = NOTIFY_MODE_FOREGROUND;
        } else {
            newNotifyMode = NOTIFY_MODE_BACKGROUND;
        }

        // 移除先前通知
        if (notifyMode != newNotifyMode && newNotifyMode == NOTIFY_MODE_BACKGROUND) {
            service.stopForeground(false);
        }

        // 发出通知
        if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
            service.startForeground(NOTIFICATION_ID, notification);
        } else if (newNotifyMode == NOTIFY_MODE_BACKGROUND) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

        // 更新模式
        notifyMode = newNotifyMode;
    }

    /**
     * 创建通知通道
     */
    @RequiresApi(26)
    private void createNotificationChannel() {
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
        if (notificationChannel == null) {
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, service.getString(R.string.playing_notification_name), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription(service.getString(R.string.playing_notification_description));
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
