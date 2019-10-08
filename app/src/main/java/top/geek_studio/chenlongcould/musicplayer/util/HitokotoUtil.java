package top.geek_studio.chenlongcould.musicplayer.util;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.Constants;
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.model.Hitokoto;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;

/**
 * HitokotoUtil
 *
 * @author : chenlongcould
 * @date : 2019/10/03/22
 */
public class HitokotoUtil {

    private static final String TAG = HitokotoUtil.class.getSimpleName();

    /**
     * 获取保存路径
     */
    public static String getSaveFilePath(Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "hitokoto";
    }

    public static void getHitokoto(@NonNull final Activity activity, @NonNull final TransDataCallback<Hitokoto> callback) {
        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(Constants.HITOKOTO_URL, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                activity.runOnUiThread(callback::onError);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final ResponseBody responseBody = response.body();
                if (responseBody == null) return;

                final Gson gson = new Gson();
                final Hitokoto hitokoto = gson.fromJson(responseBody.string(), Hitokoto.class);

                activity.runOnUiThread(() -> callback.onTrans(hitokoto));
            }
        }));
    }

    @WorkerThread
    public static void saveHitokoFile(@NonNull Activity activity, @NonNull Hitokoto data) {
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HitokotoUtil.getSaveFilePath(activity)));
            oos.writeObject(data);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    public static Hitokoto readHitokoFile(@NonNull Activity activity) {
        final File hitokotoFile = new File(getSaveFilePath(activity));

        if (hitokotoFile.exists() && hitokotoFile.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(hitokotoFile));
                return (Hitokoto) ois.readObject();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (ois != null) ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            return null;
        }
    }

}
