package top.geek_studio.chenlongcould.musicplayer.util;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.Constants;
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.model.Hitokoto;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;

/**
 * HitokotoUtils
 *
 * @author : chenlongcould
 * @date : 2019/10/03/22
 */
public class HitokotoUtils {

    public static void getHitokoto(@NonNull final Activity activity, @NonNull final TransDataCallback<Hitokoto> callback) {
        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(Constants.HITOKOTO_URL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final ResponseBody responseBody = response.body();
                if (responseBody == null) return;

                final Gson gson = new Gson();
                final Hitokoto hitokoto = gson.fromJson(responseBody.string(), Hitokoto.class);

                callback.onTrans(hitokoto);
            }
        }));
    }

}
