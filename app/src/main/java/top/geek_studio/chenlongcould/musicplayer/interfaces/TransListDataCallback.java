package top.geek_studio.chenlongcould.musicplayer.interfaces;

import java.util.List;

/**
 * @author : chenlongcould
 * @date : 2019/10/03/22
 */
public interface TransListDataCallback<T> {
    void onTrans(List<T> data);
}