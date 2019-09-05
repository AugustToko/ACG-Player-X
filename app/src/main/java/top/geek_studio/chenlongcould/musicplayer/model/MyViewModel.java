package top.geek_studio.chenlongcould.musicplayer.model;

import android.content.DialogInterface;

import androidx.lifecycle.ViewModel;

import java.util.LinkedList;
import java.util.List;

/**
 * ViewModel
 *
 * @author : chenlongcould
 * @date : 2019/09/05/15
 */
public class MyViewModel extends ViewModel {

    /**
     * 对话框(临时) 集合
     */
    public List<DialogInterface> dialogs = new LinkedList<>();
}
