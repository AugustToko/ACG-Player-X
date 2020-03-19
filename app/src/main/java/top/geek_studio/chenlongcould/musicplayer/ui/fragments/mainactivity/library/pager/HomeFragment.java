package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.appthemehelper.common.views.ATEPrimaryTextView;
import com.kabouzeid.appthemehelper.common.views.ATESecondaryTextView;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import top.geek_studio.chenlongcould.musicplayer.Constants;
import top.geek_studio.chenlongcould.musicplayer.adapter.HomeAdapter;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.interfaces.TransDataCallback;
import top.geek_studio.chenlongcould.musicplayer.loader.SongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Hitokoto;
import top.geek_studio.chenlongcould.musicplayer.model.HomeData;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.HistoryPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.LastAddedPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.MyTopTracksPlaylist;
import top.geek_studio.chenlongcould.musicplayer.threadPool.CustomThreadPool;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.base.AbsLibraryPagerFragment;
import top.geek_studio.chenlongcould.musicplayer.util.Compressor;
import top.geek_studio.chenlongcould.musicplayer.util.HitokotoUtil;
import top.geek_studio.chenlongcould.musicplayer.util.MatDialogUtil;
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil;
import top.geek_studio.chenlongcould.musicplayer.util.NavigationUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PlaylistsUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;
import top.geek_studio.chenlongcould.musicplayer.views.CircularImageView;
import top.geek_studio.chenlongcould.musicplayer.views.RetroChip;

/**
 * HomePage
 *
 * @author : chenlongcould
 */
public class HomeFragment extends AbsLibraryPagerFragment {

    private static final String TAG = HomeFragment.class.getSimpleName();

    private Unbinder unbinder;

    private HomeAdapter homeAdapter;

    private HomeDataManager homeDataManager;

    @BindView(R.id.userImage)
    CircularImageView userImage;

    @BindView(R.id.userName)
    ATEPrimaryTextView userName;

    @BindView(R.id.userInfoContainer)
    LinearLayout userInfoContainer;

    ///////////////////// HITOKOTO VIEW /////////////////

    @BindView(R.id.hitokotoView)
    LinearLayout hitokotoView;

    @BindView(R.id.hitokoto)
    ATEPrimaryTextView hitokotoText;

    @BindView(R.id.hitokotoFrom)
    ATESecondaryTextView hitikotoFrom;

    @BindView(R.id.hitokotoChipHead)
    RetroChip hitokotoHead;

    ///////////////////// HITOKOTO VIEW /////////////////

//    /**
//     * 监听 UserData
//     */
//    private Observer<FirebaseUser> userObserver = firebaseUser -> {
//        if (firebaseUser == null) {
//            setUpUserData();
//        }
//    };

    /**
     * Disp
     */
    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public String getSubTitle() {
        return getString(R.string.home);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_home, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final MainActivity appCompatActivity = getLibraryFragment().getMainActivity();

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
                        .onNegative(((dialog, which) -> loadHitokoFromNet(appCompatActivity)))
                        .neutralText("By Hitokoto.cn")
                        .onNeutral((dialog, which) -> {
                        })
                        .build();
                MatDialogUtil.setNeutralButtonDisnable(materialDialog);
                materialDialog.show();
                return false;
            });

//            userInfoContainer.setOnClickListener(v -> {
//
////                try {
////                    //包名
////                    String pkName = appCompatActivity.getPackageName();
////                    //versionName
////                    String versionName = appCompatActivity.getPackageManager().getPackageInfo(pkName, 0).versionName;
////                    //versionCode
////                    int versionCode = appCompatActivity.getPackageManager().getPackageInfo(pkName, 0).versionCode;
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
//
////                if ("zh-rCN".equals(Locale.getDefault().getCountry()) && !appCompatActivity.getPackageName().toLowerCase().contains("debug")) {
////                    Toast.makeText(appCompatActivity, "Service is not available in your country/area.", Toast.LENGTH_SHORT).show();
////                    return;
////                }
//
//                final MainActivity mainActivity = getLibraryFragment().getMainActivity();
//                FirebaseUser user = mainActivity.mViewModel.userData.getValue();
//                if (user != null && user.getDisplayName() != null) {
//                    new MaterialDialog.Builder(mainActivity)
//                            .title("User Info")
//                            .content(user.getDisplayName())
//                            .negativeText("Logout")
//                            .onNegative((dialog, which) -> mainActivity.logout())
//                            .show();
//                } else {
//                    mainActivity.startLoginActivity();
//                }
//            });

            // 监听 userData, 同时设置 User
//            appCompatActivity.mViewModel.userData.observeForever(userObserver);
//
//            final FirebaseUser user = appCompatActivity.mViewModel.userData.getValue();

//            if (user != null) {
//                setUpUserData(user);
//            } else {
            setUpUserData();
//            }

        }

        final RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(appCompatActivity));
        recyclerView.setAdapter(homeAdapter);

        setUpHomeData(appCompatActivity);
    }

    /**
     * 设置一言 (默认从已保存文件加载)
     *
     * @param activity act
     */
    // TODO: Check date
    private void setUpHitokotoView(@NonNull Activity activity) {
        final Hitokoto h = getLibraryFragment().getMainActivity().mViewModel.HitokotoData.getValue();
        if (h != null) {
            putIntoHitokotoView(h);
            return;
        }

        CustomThreadPool.post(() -> {
            Hitokoto hitokoto;
            if ((hitokoto = HitokotoUtil.readHitokoFile(activity)) != null) {
                getLibraryFragment().getMainActivity().runOnUiThread(() -> {
                    putIntoHitokotoView(hitokoto);
                    if (activity instanceof MainActivity) {
                        ((MainActivity) activity).mViewModel.HitokotoData.setValue(hitokoto);
                    }
                });
            } else {
                loadHitokoFromNet(activity);
            }
        });

    }

    /**
     * 从网络中获取一言
     *
     * @param activity activity
     */
    private void loadHitokoFromNet(Activity activity) {
        HitokotoUtil.getHitokoto(activity, new TransDataCallback<Hitokoto>() {
            @Override
            public void onTrans(@NotNull Hitokoto data) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    putIntoHitokotoView(data);
                    getLibraryFragment().getMainActivity().mViewModel.HitokotoData.setValue(data);
                });

                CustomThreadPool.post(() -> HitokotoUtil.saveHitokoFile(activity, data));
            }

            @Override
            public void onError() {
                Toast.makeText(activity, "Get Hitokoto -> Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @UiThread
    private void putIntoHitokotoView(@NonNull Hitokoto data) {
        if (!isAdded()) return;

        hitokotoText.setText(data.getHitokoto());
        hitikotoFrom.setText(data.getCreator());
        hitokotoHead.setText(data.getFrom());
    }

    /**
     * 设置用户信息 (Default)
     */
    private void setUpUserData() {
        final AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
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

//    /**
//     * 设置 用户信息
//     *
//     * @param userData data
//     */
//    public void setUpUserData(@Nullable FirebaseUser userData) {
//        if (userData == null) {
//            setUpUserData();
//        } else {
//            userName.setText(userData.getDisplayName());
//            Uri uri = userData.getPhotoUrl();
//            if (uri != null) Glide.with(getActivity()).load(userData.getPhotoUrl()).into(userImage);
//        }
//    }

    private void setUpHomeData(@Nullable MainActivity activity) {

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
            activity.mViewModel.setArtistsUpdateObs(activity, data -> CustomThreadPool.post(() -> {
                if (data.size() == 0) return;

                final HomeData home = new HomeData(
                        0,
                        R.string.recent_artists,
                        0,
                        data,
                        HomeAdapter.RECENT_ARTISTS,
                        R.drawable.ic_artist_white_24dp
                );

                homeDataManager.update(activity, home);
            }));

            // recent albums
            activity.mViewModel.setAlbumsUpdateObs(activity, data -> CustomThreadPool.post(() -> {
                if (data.size() == 0) return;

                final HomeData home = new HomeData(
                        1,
                        R.string.recent_albums,
                        0,
                        data,
                        HomeAdapter.RECENT_ALBUMS,
                        R.drawable.ic_album_white_24dp
                );

                homeDataManager.update(activity, home);
            }));

            // playlists
            activity.mViewModel.setPlaylistsUpdateObs(activity, data -> CustomThreadPool.post(() -> {
                if (data.size() == 0) return;

                final List<Playlist> playlists = new ArrayList<>();
                playlists.add(MusicUtil.getFavoritesPlaylist(requireContext()));

                final HomeData home = new HomeData(
                        4,
                        R.string.favorites,
                        0,
                        playlists,
                        HomeAdapter.PLAYLISTS,
                        R.drawable.ic_playlist_add_white_24dp
                );

                homeDataManager.update(activity, home);
            }));

            // other data listener
        }

    }

//    @Nullable
//    public static String stringMD5(String input) {
//
//        try {
//
//            // 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
//
//            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
//
//
//            // 输入的字符串转换成字节数组
//
//            byte[] inputByteArray = input.getBytes();
//
//
//            // inputByteArray是输入字符串转换得到的字节数组
//
//            messageDigest.update(inputByteArray);
//
//
//            // 转换并返回结果，也是字节数组，包含16个元素
//
//            byte[] resultByteArray = messageDigest.digest();
//
//
//            // 字符数组转换成字符串返回
//
//            return byteArrayToHex(resultByteArray);
//
//
//        } catch (NoSuchAlgorithmException e) {
//
//            return null;
//        }
//
//    }
//
//    public static String byteArrayToHex(byte[] byteArray) {
//
//        // 首先初始化一个字符数组，用来存放每个16进制字符
//
//        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
//
//
//        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
//
//        char[] resultCharArray = new char[byteArray.length * 2];
//
//
//        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
//
//        int index = 0;
//
//        for (byte b : byteArray) {
//
//            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
//
//            resultCharArray[index++] = hexDigits[b & 0xf];
//
//        }
//
//        // 字符数组组合成字符串返回
//
//        return new String(resultCharArray);
//
//    }

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
//        if (getActivity() instanceof MainActivity) {
//            ((MainActivity) getActivity()).mViewModel.userData.removeObserver(userObserver);
//        }
    }

    /**
     * 管理 Home 数据
     */
    private static class HomeDataManager {
        private HashSet<HomeData> homeList = new HashSet<>();

        private HomeData recentArtist;

        private HomeData recentAlbum;

        private HomeData recentPlaylist;

        private HomeAdapter adapter;

        HomeDataManager(HomeAdapter adapter) {
            this.adapter = adapter;
        }

        public synchronized void update(@NonNull final Activity activity, @NonNull HomeData home) {
            boolean need = false;

            if (home.getHomeSection() == HomeAdapter.RECENT_ALBUMS) {
                if (recentAlbum != null) {
                    if (isSame(home, recentAlbum)) return;

                    homeList.remove(recentAlbum);
                }
                recentAlbum = home;
                homeList.add(home);
                need = true;
            }

            if (home.getHomeSection() == HomeAdapter.RECENT_ARTISTS) {
                if (recentArtist != null) {
                    if (isSame(home, recentArtist)) return;
                    homeList.remove(recentArtist);
                }
                recentArtist = home;
                homeList.add(home);
                need = true;
            }

            if (home.getHomeSection() == HomeAdapter.PLAYLISTS) {
//                if (recentPlaylist != null) {
//
//                    final List<?> old = recentPlaylist.getArrayList();
//                    final List<?> newList = home.getArrayList();
//
//                    if (old.size() == newList.size()) {
//                        for (int i = 0; i < old.size(); i++) {
//                            final Playlist o = (Playlist) old.get4LastFM(i);
//                            final Playlist n = (Playlist) newList.get4LastFM(i);
//
//                            if (o.modifyDate != n.modifyDate) {
//                                need = true;
//                                homeList.remove(recentPlaylist);
//                                recentPlaylist = home;
//                                homeList.add(home);
//                                break;
//                            } else {
//                                need = false;
//                            }
//                        }
//                    } else {
//                        need = true;
//                        homeList.remove(recentPlaylist);
//                        recentPlaylist = home;
//                        homeList.add(home);
//                    }
//
//                } else {
//                    need = true;
//                    recentPlaylist = home;
//                    homeList.add(home);
//                }
                if (recentPlaylist != null) {
                    homeList.remove(recentPlaylist);
                }

                need = true;
                recentPlaylist = home;
                homeList.add(home);

            }

            if (need) {
                activity.runOnUiThread(() -> adapter.swapData(new ArrayList<>(homeList)));
            }
        }

        private boolean isSame(HomeData h1, HomeData h2) {
            return h1.getArrayList().size() == h2.getArrayList().size() && h1.getHomeSection() == h2.getHomeSection();
        }
    }
}
