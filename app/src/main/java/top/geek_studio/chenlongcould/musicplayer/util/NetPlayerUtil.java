package top.geek_studio.chenlongcould.musicplayer.util;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.model.NetMusicComment;
import top.geek_studio.chenlongcould.musicplayer.model.NetSearchSong;
import top.geek_studio.chenlongcould.musicplayer.model.NetSong;
import top.geek_studio.chenlongcould.musicplayer.model.nmlrc.NmLrc2;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;

/**
 * @author : chenlongcould
 * @date : 2019/10/04/17
 */
public class NetPlayerUtil {
    private static final String TAG = NetPlayerUtil.class.getSimpleName();

    public static final String BASE_URL_2 = "https://api.a632079.me";

    private static final String BASE_URL = "https://api.crypto-studio.com";

    private static Runnable getSongUrlTask = null;

    public static void getNetSongDetail(@NonNull Activity activity, long musicId, TransDataCallback<NetSong> transDataCallback) {

        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(BASE_URL + "/song/detail?ids=" + musicId, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                transDataCallback.onError();
                call.cancel();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final ResponseBody responseBody = response.body();
                if (responseBody == null) return;

                final Gson gson = new Gson();
                final NetSong netSong = gson.fromJson(responseBody.string(), NetSong.class);

//                Log.d(TAG, "onResponse: id: " + netSong.getSongs().get(0).getId() + " | " + netSong.getSongs().get(0).getAl().getPicUrl());

                transDataCallback.onTrans(netSong);
                call.cancel();
            }
        }));
    }

//    public static void getNetSongFullDetail(@NonNull Activity activity, long musicId, TransDataCallback<NetSong> transDataCallback) {
//
//        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(BASE_URL_2 + "/nm/summary/" + musicId + "?common=true&lyric=true&quick=true", new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//                activity.runOnUiThread(() -> Toast.makeText(activity, "Get Song Full Detail -> Error!", Toast.LENGTH_SHORT).show());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final ResponseBody responseBody = response.body();
//                if (responseBody == null) return;
//
//                final Gson gson = new Gson();
////                final NetSong netSong = gson.fromJson(responseBody.string(), NetSong.class);
////
//////                Log.d(TAG, "onResponse: id: " + netSong.getSongs().get(0).getId() + " | " + netSong.getSongs().get(0).getAl().getPicUrl());
////
////                transDataCallback.onTrans(netSong);
//
//            }
//        }));
//    }


//    @Deprecated
//    public static void getSongUrl(@NonNull Activity activity, long musicId, TransDataCallback<NetSongUrl> transDataCallback) {
//        cancelGetSongUrl();
//
//        final String target = BASE_URL + "/song/url?id=" + musicId;
//
//        getSongUrlTask = () -> OkHttpUtils.getInstance().get(target, new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//                activity.runOnUiThread(() -> Toast.makeText(activity, "Get Song Url -> Error!", Toast.LENGTH_SHORT).show());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final ResponseBody responseBody = response.body();
//                if (responseBody == null) return;
//
//                final Gson gson = new Gson();
//                final NetSongUrl netSongUrl = gson.fromJson(responseBody.string(), NetSongUrl.class);
//
////                Log.d(TAG, "onResponse: id: " + netSong.getSongs().get(0).getId() + " | " + netSong.getSongs().get(0).getAl().getPicUrl());
//
//                Log.d(TAG, "onResponse: Url: " + target);
//
//                transDataCallback.onTrans(netSongUrl);
//
//                getSongUrlTask = null;
//            }
//        });
//
//        CustomThreadPool.post(getSongUrlTask);
//    }

    public static void getSongComment(@NonNull Activity activity, @NonNull NetSong.SongsBean songsBean, TransDataCallback<NetMusicComment> transDataCallback) {
        getSongComment(activity, songsBean.getId(), transDataCallback);
    }

    /**
     * 获取歌曲评论
     *
     * @param activity          act
     * @param netSongId         id
     * @param transDataCallback cb
     */
    public static void getSongComment(@NonNull Activity activity, long netSongId, TransDataCallback<NetMusicComment> transDataCallback) {
        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get("https://api.a632079.me/nm/comment/music/" + netSongId + "?offset=0&limit=100", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> Toast.makeText(activity, "Get Song Comment -> Error!", Toast.LENGTH_SHORT).show());
                call.cancel();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final ResponseBody responseBody = response.body();
                if (responseBody == null) return;

                final Gson gson = new Gson();
                final NetMusicComment comment = gson.fromJson(responseBody.string(), NetMusicComment.class);

                transDataCallback.onTrans(comment);
                call.cancel();
            }
        }));
    }

    public static void cancelGetSongUrl() {
        if (getSongUrlTask != null) CustomThreadPool.removeTask(getSongUrlTask);
        getSongUrlTask = null;
    }

    public static void search(@NonNull final String key, TransDataCallback<NetSearchSong> transDataCallback) {
        final String kw = "https://api.crypto-studio.com/search?keywords=" + key;
        Log.d(TAG, "search: " + kw);
        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(kw, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                transDataCallback.onError();
                call.cancel();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final ResponseBody responseBody = response.body();
                if (responseBody == null) return;
                final Gson gson = new Gson();

                final String content = responseBody.string();

                final NetSearchSong netSearchSong = gson.fromJson(content, NetSearchSong.class);

                transDataCallback.onTrans(netSearchSong);
                call.cancel();
            }
        }));
    }

    /**
     * 根据网易云音乐歌曲 ID 获取歌词
     *
     * @param activity          activity
     * @param nmSongId          歌曲 id
     * @param transDataCallback callback
     */
    @WorkerThread
    public static void getLrc(final Activity activity, int nmSongId, TransDataCallback<NmLrc2> transDataCallback) {

        final String url = BASE_URL + "/lyric?id=" + nmSongId;
        Log.d(TAG, "getLrc: url : " + url);

        OkHttpUtils.getInstance().get(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (activity != null) activity.runOnUiThread(transDataCallback::onError);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    transDataCallback.onError();
                    return;
                }
                String content = responseBody.string();
//                Log.d(TAG, "onResponse: " + content);
                final Gson gson = new Gson();
                NmLrc2 lrc = gson.fromJson(content, NmLrc2.class);
                transDataCallback.onTrans(lrc);
            }
        });
    }

    /**
     * 根据网易云音乐歌曲 关键词 获取歌词
     *
     * @param activity          activity
     * @param songName          歌曲 id
     * @param transDataCallback callback
     */
    @WorkerThread
    public static void getLrc(Activity activity, String songName, TransDataCallback<NmLrc2> transDataCallback) {
        Log.d(TAG, "getLrc: get Lrc");
    }
}
