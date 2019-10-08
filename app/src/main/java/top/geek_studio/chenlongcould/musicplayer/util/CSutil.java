package top.geek_studio.chenlongcould.musicplayer.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.kabouzeid.appthemehelper.color.MaterialColor;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;

/**
 * Crypto Studio Utils
 *
 * @author : chenlongcould
 * @date : 2019/10/08/12
 */
public class CSutil {
    private static final String TAG = CSutil.class.getSimpleName();

    public static void checkUpdate(MainActivity activity) {
        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get("https://www.crypto-studio.com/acg-player/appinfo.html", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body == null) return;
                final Document document = Jsoup.parse(body.string());
                final Elements elements = document.select("div[class=appInfo]");
                final String info = elements.select("h4").text();
                final Long integer = Long.valueOf(info.split(" ")[2]);

                Log.d(TAG, "checkUpdate: remote code is: " + info);

                int currentVersionCode = getVersionCode(activity);

                if (currentVersionCode < integer) {
                    activity.runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle("Need to update!")
                                .setMessage(String.format(Locale.CHINA, "This version of %d is need to update!"
                                        , currentVersionCode))
                                .setCancelable(true)
                                .setNegativeButton("OK", (dialog, which) -> {
                                    final Uri uri = Uri.parse("https://www.crypto-studio.com/acg-player/release.apk");
                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                });
                        builder.show();
                        Toast.makeText(activity, "Need to Update!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }));
    }


    /**
     * 获取当前本地apk的版本
     *
     * @param mContext ctx
     * @return version
     */
    public static int getVersionCode(Context mContext) {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = mContext.getPackageManager().
                    getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
}
