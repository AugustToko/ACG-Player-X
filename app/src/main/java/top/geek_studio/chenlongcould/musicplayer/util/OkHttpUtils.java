package top.geek_studio.chenlongcould.musicplayer.util;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpUtils {
    private static OkHttpUtils okHttpUtil;
    private final OkHttpClient okHttpClient;


    private OkHttpUtils() {
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new LoggingInterceptor())
                .build();
    }

    public static OkHttpUtils getInstance() {
        if (null == okHttpUtil) {
            synchronized (OkHttpUtils.class) {
                if (null == okHttpUtil) {
                    okHttpUtil = new OkHttpUtils();
                }
            }
        }
        return okHttpUtil;
    }

    //get 请求
    public void get(String urlString, Callback callback) {
        Request request = new Request.Builder().url(urlString).get().build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    //post请求
    public void post(String urlString, FormBody formBody, Callback callback) {
        Request request = new Request.Builder().url(urlString).method("POST", formBody).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    //添加拦截器
    class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            String method = request.method();
            Response proceed = chain.proceed(request);
            return proceed;
        }
    }
}
