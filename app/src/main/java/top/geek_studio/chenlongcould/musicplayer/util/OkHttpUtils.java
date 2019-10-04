package top.geek_studio.chenlongcould.musicplayer.util;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import top.geek_studio.chenlongcould.musicplayer.App;

public class OkHttpUtils {
    private static OkHttpUtils okHttpUtil;

    private final OkHttpClient okHttpClient;

//    private final OkHttpClient okHttpClient4LastFM;

    private OkHttpUtils() {
//        okHttpClient4LastFM = new OkHttpClient.Builder()
////                .addInterceptor(new LoggingInterceptor())
//                .retryOnConnectionFailure(false)
//                .connectTimeout(5000, TimeUnit.MILLISECONDS)
//                .sslSocketFactory(getSSLSocketFactory(App.Companion.getInstance()), new MyX509TrustManager(App.Companion.getInstance()))
//                .build();

        okHttpClient = new OkHttpClient.Builder()
//                .addInterceptor(new LoggingInterceptor())
                .retryOnConnectionFailure(false)
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 单例
     * */
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

    // get4LastFM 请求
    public void get(String urlString, Callback callback) {
        final Request request = new Request.Builder().url(urlString).get().build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    // post 请求
    public void post(String urlString, FormBody formBody, Callback callback) {
        final Request request = new Request.Builder().url(urlString).method("POST", formBody).build();
        okHttpClient.newCall(request).enqueue(callback);
    }


//    // get4LastFM 请求
//    public void get4LastFM(String urlString, Callback callback) {
//        final Request request = new Request.Builder().url(urlString).get().build();
//        okHttpClient4LastFM.newCall(request).enqueue(callback);
//    }
//
//    // post 请求
//    public void post4LastFM(String urlString, FormBody formBody, Callback callback) {
//        final Request request = new Request.Builder().url(urlString).method("POST", formBody).build();
//        okHttpClient4LastFM.newCall(request).enqueue(callback);
//    }

//    //添加拦截器
//    class LoggingInterceptor implements Interceptor {
//        @Override
//        public Response intercept(Chain chain) throws IOException {
//            Request request = chain.request();
//            String method = request.method();
//            Response proceed = chain.proceed(request);
//            return proceed;
//        }
//    }

//    private SSLSocketFactory getSSLSocketFactory(Context ctx) throws NoSuchAlgorithmException, KeyManagementException {
//        final SSLContext context = SSLContext.getInstance("TLS");
//        final TrustManager[] trustManagers = {new MyX509TrustManager(ctx)};
//        context.init(null, trustManagers, new SecureRandom());
//        return context.getSocketFactory();
//    }
//
//    private class MyX509TrustManager implements X509TrustManager {
//
//        private Context context;
//
//        public MyX509TrustManager(Context context) {
//            this.context = context;
//        }
//
//        @Override
//        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//            //...
//        }
//
//        @Override
//        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//            if (chain == null) {
//                throw new CertificateException("checkServerTrusted: X509Certificate array is null");
//            }
//            if (chain.length < 1) {
//                throw new CertificateException("checkServerTrusted: X509Certificate is empty");
//            }
//            if (!(null != authType && authType.equals("ECDHE_RSA"))) {
//                throw new CertificateException("checkServerTrusted: AuthType is not ECDHE_RSA");
//            }
//
//            //检查所有证书
//            try {
//                TrustManagerFactory factory = TrustManagerFactory.getInstance("X509");
//                factory.init((KeyStore) null);
//                for (TrustManager trustManager : factory.getTrustManagers()) {
//                    ((X509TrustManager) trustManager).checkServerTrusted(chain, authType);
//                }
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            } catch (KeyStoreException e) {
//                e.printStackTrace();
//            }
//
//            //获取本地证书中的信息
//            String clientEncoded = "";
//            String clientSubject = "";
//            String clientIssUser = "";
//            try {
//                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
//                InputStream inputStream = context.getAssets().open("lastfm.cer");
//                X509Certificate clientCertificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
//                clientEncoded = new BigInteger(1, clientCertificate.getPublicKey().getEncoded()).toString(16);
//                clientSubject = clientCertificate.getSubjectDN().getName();
//                clientIssUser = clientCertificate.getIssuerDN().getName();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            //获取网络中的证书信息
//            X509Certificate certificate = chain[0];
//            PublicKey publicKey = certificate.getPublicKey();
//            String serverEncoded = new BigInteger(1, publicKey.getEncoded()).toString(16);
//
//            if (!clientEncoded.equals(serverEncoded)) {
//                throw new CertificateException("server's PublicKey is not equals to client's PublicKey");
//            }
//            String subject = certificate.getSubjectDN().getName();
//            if (!clientSubject.equals(subject)) {
//                throw new CertificateException("server's subject is not equals to client's subject");
//            }
//            String issuser = certificate.getIssuerDN().getName();
//            if (!clientIssUser.equals(issuser)) {
//                throw new CertificateException("server's issuser is not equals to client's issuser");
//            }
//        }
//
//        @Override
//        public X509Certificate[] getAcceptedIssuers() {
//            return new X509Certificate[0];
//        }
//    }
}
