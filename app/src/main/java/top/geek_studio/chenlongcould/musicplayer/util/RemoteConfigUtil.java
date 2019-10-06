package top.geek_studio.chenlongcould.musicplayer.util;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.model.DataViewModel;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;

/**
 * 获取远程服务配置
 *
 * @author : chenlongcould
 * @date : 2019/10/06/16
 */
public class RemoteConfigUtil {
    private static final String TAG = RemoteConfigUtil.class.getSimpleName();

    public static void checkAllowUseNetPlayer(@NonNull final DataViewModel viewModel) {
        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get("https://www.crypto-studio.com/?project=acgplayer-x&allowUseNetPlayer=acgplayer-x", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final ResponseBody body = response.body();
                if (body == null) return;

                final boolean result = body.string().toLowerCase().contains("true");
                viewModel.allowUseNetPlayer.postValue(result);
                Log.d(TAG, "onResponse: allow use net player: " + result);
            }
        }));
    }
}
