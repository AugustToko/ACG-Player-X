package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.netsearch;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.kabouzeid.chenlongcould.musicplayer.R;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.adapter.song.NetSearchSongAdapter;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.model.NetSearchSong;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.AbsMainActivityFragment;
import top.geek_studio.chenlongcould.musicplayer.util.NetPlayerUtil;
import top.geek_studio.chenlongcould.musicplayer.util.OkHttpUtils;

/**
 * 网络搜索页面
 *
 * @author : chenlongcould
 * @date : 2019/10/04/14
 */
public class NetSearchFragment extends AbsMainActivityFragment implements MainActivity.MainActivityFragmentCallbacks {

    private static final String TAG = NetSearchFragment.class.getSimpleName();

    private Unbinder unbinder;

    private MediaPlayer mediaPlayer;

    private NetSearchSongAdapter adapter;

    private List<MaterialDialog> dialogs = new ArrayList<>();

    @BindView(R.id.recyclerView)
    FastScrollRecyclerView fastScrollRecyclerView;

    @BindView(R.id.searchButton)
    Button button;

    @BindView(R.id.searchBar)
    EditText editText;

    @BindView(R.id.fab)
    FloatingActionButton floatingActionButton;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    public static Fragment newInstance() {
        return new NetSearchFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        initPlayer();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_net_search, container, false);

        unbinder = ButterKnife.bind(this, view);

        fastScrollRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        getMainActivity().setSupportActionBar(toolbar);

        button.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(editText.getText())) {
                search(editText.getText().toString());
            }
        });

        floatingActionButton.setOnClickListener(v -> pauseMusic());

        hideFab();
        progressBar.animate().alpha(0).setDuration(0).start();
        progressBar.setClickable(false);

        return view;
    }

    private synchronized void initPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            casePlayerFailed(getMainActivity(), mediaPlayer);
            return false;
        });

        // 完成重复
        mediaPlayer.setOnCompletionListener(mp -> mp.seekTo(0));
    }

    /**
     * 释放资源 (MUST!)
     */
    private synchronized void releasePlayer() {
        hideFab();

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    private synchronized void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.reset();
        }
        hideFab();
    }

    public synchronized void tryPlay(@NonNull final NetSearchSong.ResultBean.SongsBean songsBean) {
//        Song song = new Song(
//                -1, songsBean.getName(), -1, -1,
//                songsBean.getDuration(),
//                "https://v1.hitokoto.cn/nm/redirect/music/\" + songsBean.getId()",
//                -1, -1, songsBean.getAlbum().getName(), -1, songsBean.getArtists().get(0).getName()
//        );

        if (mediaPlayer == null) initPlayer();

        final MaterialDialog dialog = new MaterialDialog.Builder(getMainActivity())
                .title("Loading...")
                .content("Connecting to " + NetPlayerUtil.BASE_URL_2)
                .cancelable(false)
//                .cancelListener(dialog1 -> NetPlayerUtil.cancelGetSongUrl())
                .build();
        dialogs.add(dialog);
        dialog.show();

        CustomThreadPool.post(() -> {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource("https://v1.hitokoto.cn/nm/redirect/music/" + songsBean.getId());
                mediaPlayer.prepare();
                mediaPlayer.start();
                showFab();
            } catch (IOException e) {
                e.printStackTrace();
                hideFab();
                mediaPlayer.reset();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                releasePlayer();
                initPlayer();
            } finally {
                clearDialogs();
            }
        });

    }

    @UiThread
    private void clearDialogs() {
        getMainActivity().runOnUiThread(() -> {
            for (MaterialDialog materialDialog : dialogs) {
                if (materialDialog != null && materialDialog.isShowing()) {
                    materialDialog.cancel();
                }
            }
        });
    }

    /**
     * 当播放歌曲失败时调用
     */
    private void casePlayerFailed(@NonNull final Activity activity, @NonNull MediaPlayer player) {
        player.reset();
        Toast.makeText(activity, "Playback failed!", Toast.LENGTH_SHORT).show();
    }

    private void search(@Nullable final String key) {
        progressBar.animate().alpha(100).setDuration(1000).start();

        NetPlayerUtil.search(getActivity(), key, new TransDataCallback<NetSearchSong>() {
            @Override
            public void onTrans(NetSearchSong data) {
                adapter = new NetSearchSongAdapter(getMainActivity(), NetSearchFragment.this, data.getResult().getSongs(), R.layout.item_list, false, null);

                getMainActivity().runOnUiThread(() -> {
                    progressBar.animate().alpha(0).setDuration(1000).start();
                    fastScrollRecyclerView.setAdapter(adapter);
                });
            }

            @Override
            public void onError() {
                getActivity().runOnUiThread(() -> progressBar.animate().alpha(0).setDuration(1000).start());
            }
        });

    }

    @Override
    public void onPause() {
        releasePlayer();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        releasePlayer();
        super.onLowMemory();
    }

    @Override
    public void onDetach() {
        releasePlayer();
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        releasePlayer();
        if (unbinder != null) unbinder.unbind();
        clearDialogs();
        super.onDestroyView();
    }

    @Override
    public boolean handleBackPress() {
        return false;
    }

    @UiThread
    public void showFab() {
        getMainActivity().runOnUiThread(() -> {
            if (floatingActionButton != null) floatingActionButton.show();
        });
    }

    @UiThread
    public void hideFab() {
        getMainActivity().runOnUiThread(() -> {
            if (floatingActionButton != null) floatingActionButton.hide();
        });
    }
}
