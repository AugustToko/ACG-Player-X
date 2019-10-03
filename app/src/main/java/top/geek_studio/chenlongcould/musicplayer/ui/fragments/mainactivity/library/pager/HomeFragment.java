package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.google.gson.Gson;
import com.kabouzeid.appthemehelper.common.views.ATEPrimaryTextView;
import com.kabouzeid.appthemehelper.common.views.ATESecondaryTextView;
import com.kabouzeid.chenlongcould.musicplayer.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.geek_studio.chenlongcould.musicplayer.Constants;
import top.geek_studio.chenlongcould.musicplayer.adapter.HomeAdapter;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.loader.SongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Hitokoto;
import top.geek_studio.chenlongcould.musicplayer.model.Home;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.HistoryPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.LastAddedPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.MyTopTracksPlaylist;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.base.AbsLibraryPagerFragment;
import top.geek_studio.chenlongcould.musicplayer.util.Compressor;
import top.geek_studio.chenlongcould.musicplayer.util.HitokotoUtils;
import top.geek_studio.chenlongcould.musicplayer.util.NavigationUtil;
import top.geek_studio.chenlongcould.musicplayer.util.OkHttpUtils;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;
import top.geek_studio.chenlongcould.musicplayer.views.CircularImageView;

/**
 * HomePage
 *
 * @author : chenlongcould
 * @date : 2019/10/03/10
 */
public class HomeFragment extends AbsLibraryPagerFragment {

    private static final String TAG = HomeFragment.class.getSimpleName();

    private Unbinder unbinder;

    private HomeAdapter homeAdapter;

    private HomeDataManager homeDataManager;

    CircularImageView userImage;

    ATEPrimaryTextView userName;

    LinearLayout hitokotoView;

    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public String getSubTitle() {
        return "Home";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_home, container, false);
        unbinder = ButterKnife.bind(view);

        userImage = view.findViewById(R.id.userImage);
        userName = view.findViewById(R.id.titleWelcome);
        hitokotoView = view.findViewById(R.id.hitokotoView);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        if (appCompatActivity != null) {
            homeAdapter = new HomeAdapter(appCompatActivity, new ArrayList<>(), getDisplayMetrics(appCompatActivity));
            homeDataManager = new HomeDataManager(homeAdapter);

            // set View click
            view.findViewById(R.id.lastAdded).setOnClickListener(v -> NavigationUtil.goToPlaylist(appCompatActivity, new LastAddedPlaylist(appCompatActivity)));
            view.findViewById(R.id.topPlayed).setOnClickListener(v -> NavigationUtil.goToPlaylist(appCompatActivity, new MyTopTracksPlaylist(appCompatActivity)));
            view.findViewById(R.id.actionShuffle).setOnClickListener(v -> MusicPlayerRemote.openAndShuffleQueue(SongLoader.getAllSongs(requireActivity()), true));
            view.findViewById(R.id.history).setOnClickListener(v -> NavigationUtil.goToPlaylist(appCompatActivity, new HistoryPlaylist(appCompatActivity)));

            // first set
            setUpHitokotoView(appCompatActivity);

            hitokotoView.setOnLongClickListener(v -> {
                MaterialDialog materialDialog = new MaterialDialog.Builder(appCompatActivity).title("Hitokoto · 一言")
                        .content("Select a action")
                        .negativeText("Change Me!")
                        .onNegative(((dialog, which) -> setUpHitokotoView(appCompatActivity)))
                        .neutralText("By Hitokoto.cn")
                        .onNeutral((dialog, which) -> { })
                        .build();
                final MDButton button = materialDialog.getActionButton(DialogAction.NEUTRAL);
                button.setEnabled(false);
                button.setClickable(false);
                button.setTextColor(Color.GRAY);
                materialDialog.show();
                return false;
            });
        }

        final RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(appCompatActivity));
        recyclerView.setAdapter(homeAdapter);

        setUpUserData(appCompatActivity);

        setUpHomeData(appCompatActivity);

    }

    private void setUpHitokotoView(Activity activity) {
        HitokotoUtils.getHitokoto(activity, data -> activity.runOnUiThread(() -> {
            ((ATEPrimaryTextView) hitokotoView.findViewById(R.id.hitokoto)).setText(data.getHitokoto());
            ((ATESecondaryTextView) hitokotoView.findViewById(R.id.hitokotoFrom)).setText(data.getFrom());
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).mViewModel.HitokotoData.setValue(data);
            }
        }));
    }

    /**
     * 设置用户信息
     *
     * @param appCompatActivity activity
     */
    private void setUpUserData(@Nullable AppCompatActivity appCompatActivity) {
        if (appCompatActivity == null) return;

        final File userImageFile = new File(PreferenceUtil.getInstance(requireContext()).getProfileImage(), Constants.USER_PROFILE);

        if (userImageFile.exists()) {
            disposable.add(new Compressor(appCompatActivity)
                    .setMaxHeight(300)
                    .setMaxWidth(300)
                    .setQuality(75)
                    .setCompressFormat(Bitmap.CompressFormat.WEBP)
                    .compressToBitmapAsFlowable(userImageFile)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(it -> {
                        if (it != null) {
                            userImage.setImageBitmap(it);
                        } else {
                            userImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_person_flat));
                        }
                    })
            );
        } else {
            userImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_person_flat));
        }

        userName.setText("User");
    }

    private void setUpHomeData(@Nullable AppCompatActivity appCompatActivity) {

        final MainActivity activity = (MainActivity) appCompatActivity;
        if (activity != null) {
//            activity.mViewModel.setSongsUpdateObs(activity, data -> {
//                // do sth
//                homesData.add(0, new Home(
//                        0,
//                        R.string.songs,
//                        R.string.songs,
//                        (ArrayList<?>) data,
//                        HomeAdapter.,
//                        R.drawable.ic_bookmark_music_white_24dp
//                ));
//                homeAdapter.swapData(homesData);
//            });

            // recent artists
            activity.mViewModel.setArtistsUpdateObs(activity, data -> {

                final Home home = new Home(
                        0,
                        R.string.recent_artists,
                        0,
                        (ArrayList<?>) data,
                        HomeAdapter.RECENT_ARTISTS,
                        R.drawable.ic_artist_white_24dp
                );

                homeDataManager.update(home);
            });

            // recent albums
            activity.mViewModel.setAlbumsUpdateObs(activity, data -> {

                Home home = new Home(
                        1,
                        R.string.recent_albums,
                        0,
                        (ArrayList<?>) data,
                        HomeAdapter.RECENT_ALBUMS,
                        R.drawable.ic_album_white_24dp
                );

                homeDataManager.update(home);
            });

//            // playlists
//            activity.mViewModel.setPlaylistsUpdateObs(activity, data -> {
//                Log.d(TAG, "setUpHomeData playlist count: " + data.size());
//
//                // do sth
//                homesData.remove(4);
//                homesData.add(4, new Home(
//                        4,
//                        R.string.playlists,
//                        0,
//                        (ArrayList<?>) data,
//                        HomeAdapter.PLAYLISTS,
//                        R.drawable.ic_bookmark_music_white_24dp
//                ));
//                homeAdapter.swapData(homesData);
//            });

            // other data listener
        }

    }

    private DisplayMetrics getDisplayMetrics(@NonNull Activity activity) {
        final Display display = activity.getWindowManager().getDefaultDisplay();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);
        return displayMetrics;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    /**
     * 管理 Home 数据
     */
    private static class HomeDataManager {
        private HashSet<Home> homeList = new HashSet<>();

        private Home recentArtist;

        private Home recentAlbum;

        private HomeAdapter adapter;

        HomeDataManager(HomeAdapter adapter) {
            this.adapter = adapter;
        }

        public void update(@NonNull Home home) {
            if (home.getHomeSection() == HomeAdapter.RECENT_ALBUMS) {
                if (recentAlbum != null) {
                    if (isSame(home, recentAlbum)) return;

                    homeList.remove(recentAlbum);
                }
                recentAlbum = home;
                homeList.add(home);
                adapter.swapData(new ArrayList<>(homeList));
            }

            if (home.getHomeSection() == HomeAdapter.RECENT_ARTISTS) {
                if (recentArtist != null) {
                    if (isSame(home, recentArtist)) return;
                    homeList.remove(recentArtist);
                }
                recentArtist = home;
                homeList.add(home);
                adapter.swapData(new ArrayList<>(homeList));
            }
        }

        private boolean isSame(Home h1, Home h2) {
            return h1.getArrayList().size() == h2.getArrayList().size() && h1.getHomeSection() == h2.getHomeSection();
        }
    }
}
