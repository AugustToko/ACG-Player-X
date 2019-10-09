package top.geek_studio.chenlongcould.musicplayer.util.openimage;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.model.yuepic.YuePic;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;
import top.geek_studio.chenlongcould.musicplayer.util.OkHttpUtils;

/**
 * @author : chenlongcould
 * @date : 2019/10/07/19
 */
public class YuePicUtil {
    private static final String BASE_URL = "https://api.unsplash.com";

    private static final String CLIENT_ID = "6d7c88f20859125c16cda9d294e5258fe7d6599a5d32756feea4c617d707b47b";

    private static final String PARAM_CLIENT_ID = "?client_id=";

    public static void getRandomYuePic(@NonNull final Activity activity, @NonNull final TransDataCallback<YuePic> callback) {
        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(BASE_URL + "/photos/random" + PARAM_CLIENT_ID + CLIENT_ID, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                activity.runOnUiThread(callback::onError);
                e.printStackTrace();
                call.cancel();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final ResponseBody body = response.body();
                if (body == null) {
                    call.cancel();
                    return;
                }

                final Gson gson = new Gson();
                final YuePic yuePic = gson.fromJson(body.string(), YuePic.class);
                activity.runOnUiThread(() -> callback.onTrans(yuePic));
            }
        }));
    }

    @NonNull
    public static String getSaveFilePath(@NonNull Activity activity) {
        return activity.getFilesDir().getAbsolutePath() + File.separator + "yuepic";
    }

    @WorkerThread
    public static void saveYuePicFile(@NonNull Activity activity, @NonNull YuePic data) {
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(YuePicUtil.getSaveFilePath(activity)));
            oos.writeObject(data);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    @Nullable
    public static YuePic readYuePicFile(@NonNull Activity activity) {
        final File yuePicFile = new File(getSaveFilePath(activity));

        if (yuePicFile.exists() && yuePicFile.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(yuePicFile));
                return (YuePic) ois.readObject();
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
