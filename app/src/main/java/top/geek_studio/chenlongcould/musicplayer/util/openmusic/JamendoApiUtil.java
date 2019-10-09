package top.geek_studio.chenlongcould.musicplayer.util.openmusic;

import android.app.Activity;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;
import top.geek_studio.chenlongcould.musicplayer.util.OkHttpUtils;

/**
 * <a href="https://developer.jamendo.com">Jamendo</a>
 *
 * @author : chenlongcould
 * @date : 2019/10/08/20
 */
public class JamendoApiUtil {

    public static final String CLIENT_ID = "b6b68d0d";

    public static final String BASE_URL_3_0 = "https://api.jamendo.com/v3.0";

    public static class Album {

        public static final String SUB_PARAM = "/albums";

        public static void getAlbumGobal(@NonNull Activity activity) {
            CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(
                    BASE_URL_3_0 + SUB_PARAM + "/?client_id=" + CLIENT_ID + "&format=jsonpretty&artist_name=we+are+fm"
                    , new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {

                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                        }
                    }));
        }

        public static void getFile(@NonNull Activity activity, final int albumId) {
            CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(
                    BASE_URL_3_0 + SUB_PARAM + "/file/?client_id=" + CLIENT_ID + "&id=" + albumId
                    , new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {

                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                        }
                    }));
        }

        public static void getTrack(@NonNull Activity activity, @NonNull final String artistName) {
            CustomThreadPool.post(() -> OkHttpUtils.getInstance().get(
                    BASE_URL_3_0 + SUB_PARAM
                            + "/tracks/?client_id=" + CLIENT_ID
                            + "&format=jsonpretty&limit=1&artist_name=" + artistName
                    , new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {

                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                        }
                    }));
        }
    }
}
