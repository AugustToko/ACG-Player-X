package top.geek_studio.chenlongcould.musicplayer.helper;

import android.view.View;

/**
 * 用于播放(暂停)按钮的实现
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlayPauseButtonOnClickHandler implements View.OnClickListener {
    @Override
    public void onClick(View v) {
        if (MusicPlayerRemote.isPlaying()) {
            MusicPlayerRemote.pauseSong();
        } else {
            MusicPlayerRemote.resumePlaying();
        }
    }
}
