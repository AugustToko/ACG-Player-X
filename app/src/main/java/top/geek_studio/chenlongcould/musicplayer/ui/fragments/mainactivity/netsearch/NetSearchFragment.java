package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.netsearch;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.kabouzeid.chenlongcould.musicplayer.R;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.adapter.song.NetSearchSongAdapter;
import top.geek_studio.chenlongcould.musicplayer.model.NetSearchSong;
import top.geek_studio.chenlongcould.musicplayer.model.NetSong;
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

    private NetSong.SongsBean currentSong = new NetSong.SongsBean();

    private NetSearchSongAdapter adapter;

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
        progressBar.setAlpha(0);
        progressBar.setClickable(false);

        return view;
    }

    private void initPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            casePlayerFailed(getMainActivity(), mediaPlayer, currentSong);
            return false;
        });

        // 完成重复
        mediaPlayer.setOnCompletionListener(mp -> mp.seekTo(0));
    }

    /**
     * 释放资源 (MUST!)
     */
    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.reset();
        }
        hideFab();
    }

    public synchronized void tryPlay(@NonNull final NetSearchSong.ResultBean.SongsBean songsBean) {
        MaterialDialog dialog = new MaterialDialog.Builder(getMainActivity()).title("Loading...").content("Connecting to https://api.a632079.me").cancelable(true).build();
        dialog.show();

        NetPlayerUtil.getNetSong(getActivity(), songsBean, data -> {
            currentSong = data.getSongs().get(0);
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(currentSong.getUrl());
                mediaPlayer.prepare();
                mediaPlayer.start();

                showFab();
                getMainActivity().runOnUiThread(dialog::cancel);

            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 当播放歌曲失败时调用
     */
    private void casePlayerFailed(@NonNull final Activity activity, @NonNull MediaPlayer player, @NonNull NetSong.SongsBean songsBean) {
        player.reset();
        Toast.makeText(activity, "Playback failed!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "casePlayerFailed: " + songsBean.getUrl());
    }

    private void search(@Nullable final String key) {
        progressBar.animate().alpha(100).setDuration(1500).start();

        CustomThreadPool.post(() -> OkHttpUtils.getInstance().get("https://api.a632079.me/nm/search/" + key + "?type=SONG&offset=0&limit=30", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                getMainActivity().runOnUiThread(() -> Toast.makeText(getContext(), "NetWork Error!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final ResponseBody responseBody = response.body();

                if (responseBody == null) return;

                final Gson gson = new Gson();
                final NetSearchSong netSearchSong = gson.fromJson(responseBody.string(), NetSearchSong.class);

                adapter = new NetSearchSongAdapter(getMainActivity(), NetSearchFragment.this, netSearchSong.getResult().getSongs(), R.layout.item_list, false, null);

                getMainActivity().runOnUiThread(() -> {
                    progressBar.animate().alpha(0).setDuration(1500).start();
                    fastScrollRecyclerView.setAdapter(adapter);
                });
            }
        }));
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
        super.onDestroyView();
    }

    @Override
    public boolean handleBackPress() {
        return false;
    }

    public void showFab() {
        getMainActivity().runOnUiThread(() -> {
            if (floatingActionButton != null) floatingActionButton.show();
        });
    }

    public void hideFab() {
        getMainActivity().runOnUiThread(() -> {
            if (floatingActionButton != null) floatingActionButton.hide();
        });
    }
}
