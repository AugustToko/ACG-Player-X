package top.geek_studio.chenlongcould.musicplayer.interfaces;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

/**
 * @author : chenlongcould
 * @date : 2019/10/03/22
 */
public interface TransDataCallback<T> {
    @WorkerThread
    void onTrans(@NonNull T data);
    @WorkerThread
    void onError();
}