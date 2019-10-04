package top.geek_studio.chenlongcould.musicplayer.util;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.glide.NetSongGlideRequest;
import top.geek_studio.chenlongcould.musicplayer.glide.PhonographColoredTarget;
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.model.NetSearchSong;
import top.geek_studio.chenlongcould.musicplayer.model.NetSong;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;

/**
 * @author : chenlongcould
 * @date : 2019/10/04/17
 */
public class NetPlayerUtil {
    private static final String TAG = NetPlayerUtil.class.getSimpleName();

    public static void getNetSong(Activity activity, NetSearchSong.ResultBean.SongsBean songsBean, TransDataCallback<NetSong> transDataCallback) {

        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get("https://api.a632079.me/nm/summary/" + songsBean.getId() + "?common=true&lyric=true&quick=true", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> Toast.makeText(activity, "Get Song info -> Error!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final ResponseBody responseBody = response.body();
                if (responseBody == null) return;

                final Gson gson = new Gson();
                NetSong netSong = gson.fromJson(responseBody.string(), NetSong.class);

                Log.d(TAG, "onResponse: url " + netSong.getSongs().get(0).getAlbum().getPicture());

                transDataCallback.onTrans(netSong);
            }
        }));
    }
}
