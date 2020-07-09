//package top.geek_studio.chenlongcould.musicplayer.service;
//
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//
///**
// * @author : chenlongcould
// * @date : 2019/10/06/21
// */
//public class MyFirebaseMessagingService extends FirebaseMessagingService {
//    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
//
//    public MyFirebaseMessagingService() {
//        super();
//    }
//
//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//    }
//
//    @Override
//    public void onDeletedMessages() {
//        super.onDeletedMessages();
//    }
//
//    @Override
//    public void onMessageSent(@NonNull String s) {
//        super.onMessageSent(s);
//    }
//
//    @Override
//    public void onSendError(@NonNull String s, @NonNull Exception e) {
//        super.onSendError(s, e);
//    }
//
//    @Override
//    public void onNewToken(@NonNull String s) {
//        Log.d(TAG, "onNewToken: " + s);
//        super.onNewToken(s);
//    }
//}
