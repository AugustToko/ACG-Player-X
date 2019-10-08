package top.geek_studio.chenlongcould.musicplayer.interfaces;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

/**
 * @author : chenlongcould
 * @date : 2019/10/03/22
 */
public interface TransDataCallback<T> {
    @UiThread
    @MainThread
    void onTrans(@NonNull T data);

    @UiThread
    @MainThread
    void onError();
}