package top.geek_studio.chenlongcould.musicplayer.helper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.chenlongcould.musicplayer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

import top.geek_studio.chenlongcould.musicplayer.loader.SongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.service.MusicService;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * Music Player Remote
 *
 * @author chenlongcould (modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MusicPlayerRemote {

    public static final String TAG = MusicPlayerRemote.class.getSimpleName();

    @Nullable
    public static MusicService musicService;

    /**
     * 所有绑定的服务集合
     */
    private static final WeakHashMap<Context, MyServiceConnection> mConnectionMap = new WeakHashMap<>();

    /**
     * 绑定服务
     *
     * @param context  activity
     * @param callback serviceConnection
     *
     * @return token
     */
    public static ServiceToken bindToService(@NonNull final Activity context,
                                             final ServiceConnection callback) {
        Activity realActivity = context.getParent();
        if (realActivity == null) {
            realActivity = context;
        }

        final ContextWrapper contextWrapper = new ContextWrapper(realActivity);

        // start service
        contextWrapper.startService(new Intent(contextWrapper, MusicService.class));

        final MyServiceConnection binder = new MyServiceConnection(callback);

        // bind service
        if (contextWrapper.bindService(new Intent().setClass(contextWrapper, MusicService.class), binder, Context.BIND_AUTO_CREATE)) {
            // 存储绑定 binder
            mConnectionMap.put(contextWrapper, binder);
            return new ServiceToken(contextWrapper);
        }
        return null;
    }

    /**
     * 取消绑定服务
     *
     * @param token token
     */
    public static void unbindFromService(@Nullable final ServiceToken token) {
        if (token == null) {
            return;
        }
        final ContextWrapper mContextWrapper = token.mWrappedContext;

        // 从集合中移除并获取对应的 connection
        final MyServiceConnection myServiceConnection = mConnectionMap.remove(mContextWrapper);
        if (myServiceConnection == null) {
            return;
        }
        mContextWrapper.unbindService(myServiceConnection);
        if (mConnectionMap.isEmpty()) {
            musicService = null;
        }
    }

    /**
     * 包装 connection
     */
    public static final class MyServiceConnection implements ServiceConnection {

        private final ServiceConnection mCallback;

        public MyServiceConnection(final ServiceConnection callback) {
            mCallback = callback;
        }

        /**
         * connect -> callback.connect -> AbsMusicServiceActivity.connect
         */
        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            final MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            musicService = null;
        }
    }

    /**
     * 封装 contextWrapper
     */
    @SuppressWarnings("WeakerAccess")
    public static final class ServiceToken {
        public ContextWrapper mWrappedContext;

        public ServiceToken(final ContextWrapper context) {
            mWrappedContext = context;
        }
    }

    /**
     * Async
     */
    public static void playSongAt(final int position) {
        if (musicService != null) {
            musicService.playSongAt(position);
        }
    }

    /**
     * Async
     */
    public static void setPosition(final int position) {
        if (musicService != null) {
            musicService.setPosition(position);
        }
    }

    public static void pauseSong() {
        if (musicService != null) {
            musicService.pause();
        }
    }

    /**
     * Async
     */
    public static void playNextSong() {
        if (musicService != null) {
            musicService.playNextSong(true);
        }
    }

    /**
     * Async
     */
    public static void playPreviousSong() {
        if (musicService != null) {
            musicService.playPreviousSong(true);
        }
    }

    /**
     * Async
     */
    public static void back() {
        if (musicService != null) {
            musicService.back(true);
        }
    }

    public static boolean isPlaying() {
        return musicService != null && musicService.isPlaying();
    }

    public static void resumePlaying() {
        if (musicService != null) {
            musicService.play();
        }
    }

    /**
     * 设置队列
     *
     * @param queue        数据源 (songs)
     * @param needShuffle  是否随机
     * @param startPlaying 是否播放
     */
    public static void openQueue(@Nullable final List<Song> queue, final boolean needShuffle, final boolean startPlaying) {
        if (queue == null || queue.isEmpty()) return;

        if (needShuffle) {
            MusicPlayerRemote.openAndShuffleQueue(queue, startPlaying);
        } else {
            MusicPlayerRemote.openQueue(queue, 0, startPlaying);
        }

    }

    /**
     * Async
     * <p>
     * 设置当前播放队列
     *
     * @param queue         给定播放列表 (songs) 队列
     * @param startPosition 开始播放位置 (index)
     * @param startPlaying  是否播放
     */
    public static void openQueue(final List<Song> queue, final int startPosition, final boolean startPlaying) {
        // 如果当前队列不同于给定队列
        if (tryToHandleOpenPlayingQueue(queue, startPosition, startPlaying) && musicService != null) {
            musicService.openQueue(queue, startPosition, startPlaying);
            // 设置随机
            if (!PreferenceUtil.getInstance(musicService).rememberShuffle()) {
                setShuffleMode(MusicService.SHUFFLE_MODE_NONE);
            }
        }
    }

    /**
     * Async
     * <p>
     * 设置队列并随机打乱
     *
     * @param queue        给定播放数据 (songs)
     * @param startPlaying 是否播放
     */
    public static void openAndShuffleQueue(@Nullable final List<Song> queue, boolean startPlaying) {
        if (queue == null) return;

        int startPosition = 0;
        if (!queue.isEmpty()) {
            // 随机播放
            startPosition = new Random().nextInt(queue.size());
        }

        // 如果当前队列不同于给定队列
        if (tryToHandleOpenPlayingQueue(queue, startPosition, startPlaying) && musicService != null) {
            openQueue(queue, startPosition, startPlaying);
            setShuffleMode(MusicService.SHUFFLE_MODE_SHUFFLE);
        }
    }

    /**
     * 检测队列
     *
     * @param queue         歌曲队列 (目前播放列表)
     * @param startPlaying  播放 index
     * @param startPosition 是否立即播放
     */
    private static boolean tryToHandleOpenPlayingQueue(final List<Song> queue, final int startPosition, final boolean startPlaying) {
        if (getPlayingQueue() == queue) {
            if (startPlaying) {
                playSongAt(startPosition);
            } else {
                setPosition(startPosition);
            }
            return false;
        }
        return true;
    }

    public static Song getCurrentSong() {
        if (musicService != null) {
            return musicService.getCurrentSong();
        }
        return Song.EMPTY_SONG;
    }

    public static int getPosition() {
        if (musicService != null) {
            return musicService.getPosition();
        }
        return -1;
    }

    public static List<Song> getPlayingQueue() {
        if (musicService != null) {
            return musicService.getPlayingQueue();
        }
        return new ArrayList<>();
    }

    public static int getSongProgressMillis() {
        if (musicService != null) {
            return musicService.getSongProgressMillis();
        }
        return -1;
    }

    public static int getSongDurationMillis() {
        if (musicService != null) {
            return musicService.getSongDurationMillis();
        }
        return -1;
    }

    public static long getQueueDurationMillis(int position) {
        if (musicService != null) {
            return musicService.getQueueDurationMillis(position);
        }
        return -1;
    }

    public static int seekTo(int millis) {
        if (musicService != null) {
            return musicService.seek(millis);
        }
        return -1;
    }

    public static int getRepeatMode() {
        if (musicService != null) {
            return musicService.getRepeatMode();
        }
        return MusicService.REPEAT_MODE_NONE;
    }

    public static int getShuffleMode() {
        if (musicService != null) {
            return musicService.getShuffleMode();
        }
        return MusicService.SHUFFLE_MODE_NONE;
    }

    public static boolean cycleRepeatMode() {
        if (musicService != null) {
            musicService.cycleRepeatMode();
            return true;
        }
        return false;
    }

    public static boolean toggleShuffleMode() {
        if (musicService != null) {
            musicService.toggleShuffle();
            return true;
        }
        return false;
    }

    public static boolean setShuffleMode(final int shuffleMode) {
        if (musicService != null) {
            musicService.setShuffleMode(shuffleMode);
            return true;
        }
        return false;
    }

    public static boolean playNext(Song song) {
        if (musicService != null) {
            if (getPlayingQueue().size() > 0) {
                musicService.addSong(getPosition() + 1, song);
            } else {
                List<Song> queue = new ArrayList<>();
                queue.add(song);
                openQueue(queue, 0, false);
            }
            Toast.makeText(musicService, musicService.getResources().getString(R.string.added_title_to_playing_queue), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public static boolean playNext(@NonNull List<Song> songs) {
        if (musicService != null) {
            if (getPlayingQueue().size() > 0) {
                musicService.addSongs(getPosition() + 1, songs);
            } else {
                openQueue(songs, 0, false);
            }
            final String toast = songs.size() == 1 ? musicService.getResources().getString(R.string.added_title_to_playing_queue) : musicService.getResources().getString(R.string.added_x_titles_to_playing_queue, songs.size());
            Toast.makeText(musicService, toast, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public static boolean enqueue(Song song) {
        if (musicService != null) {
            if (getPlayingQueue().size() > 0) {
                musicService.addSong(song);
            } else {
                List<Song> queue = new ArrayList<>();
                queue.add(song);
                openQueue(queue, 0, false);
            }
            Toast.makeText(musicService, musicService.getResources().getString(R.string.added_title_to_playing_queue), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public static boolean enqueue(@NonNull List<Song> songs) {
        if (musicService != null) {
            if (getPlayingQueue().size() > 0) {
                musicService.addSongs(songs);
            } else {
                openQueue(songs, 0, false);
            }
            final String toast = songs.size() == 1 ? musicService.getResources().getString(R.string.added_title_to_playing_queue) : musicService.getResources().getString(R.string.added_x_titles_to_playing_queue, songs.size());
            Toast.makeText(musicService, toast, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public static boolean removeFromQueue(@NonNull Song song) {
        if (musicService != null) {
            musicService.removeSong(song);
            return true;
        }
        return false;
    }

    public static boolean removeFromQueue(int position) {
        if (musicService != null && position >= 0 && position < getPlayingQueue().size()) {
            musicService.removeSong(position);
            return true;
        }
        return false;
    }

    public static boolean moveSong(int from, int to) {
        if (musicService != null && from >= 0 && to >= 0 && from < getPlayingQueue().size() && to < getPlayingQueue().size()) {
            musicService.moveSong(from, to);
            return true;
        }
        return false;
    }

    public static boolean clearQueue() {
        if (musicService != null) {
            musicService.clearQueue();
            return true;
        }
        return false;
    }

    public static int getAudioSessionId() {
        if (musicService != null) {
            return musicService.getAudioSessionId();
        }
        return -1;
    }

    public static void playFromUri(Uri uri) {
        if (musicService != null) {
            List<Song> songs = null;
            if (uri.getScheme() != null && uri.getAuthority() != null) {
                if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                    String songId = null;
                    if (uri.getAuthority().equals("com.android.providers.media.documents")) {
                        songId = getSongIdFromMediaProvider(uri);
                    } else if (uri.getAuthority().equals("media")) {
                        songId = uri.getLastPathSegment();
                    }
                    if (songId != null) {
                        songs = SongLoader.getSongs(SongLoader.makeSongCursor(
                                musicService,
                                MediaStore.Audio.AudioColumns._ID + "=?",
                                new String[]{songId}
                        ));
                    }
                }
            }
            if (songs == null) {
                File songFile = null;
                if (uri.getAuthority() != null && uri.getAuthority().equals("com.android.externalstorage.documents")) {
                    songFile = new File(Environment.getExternalStorageDirectory(), uri.getPath().split(":", 2)[1]);
                }
                if (songFile == null) {
                    String path = getFilePathFromUri(musicService, uri);
                    if (path != null)
                        songFile = new File(path);
                }
                if (songFile == null && uri.getPath() != null) {
                    songFile = new File(uri.getPath());
                }
                if (songFile != null) {
                    songs = SongLoader.getSongs(SongLoader.makeSongCursor(
                            musicService,
                            MediaStore.Audio.AudioColumns.DATA + "=?",
                            new String[]{songFile.getAbsolutePath()}
                    ));
                }
            }
            if (songs != null && !songs.isEmpty()) {
                openQueue(songs, 0, true);
            } else {
                //TODO the file is not listed in the media store
            }
        }
    }

    @Nullable
    private static String getFilePathFromUri(Context context, Uri uri) {

        final String column = "_data";
        final String[] projection = {
                column
        };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e.getMessage()));
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getSongIdFromMediaProvider(Uri uri) {
        return DocumentsContract.getDocumentId(uri).split(":")[1];
    }

    public static boolean isServiceConnected() {
        return musicService != null;
    }
}
