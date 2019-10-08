package top.geek_studio.chenlongcould.musicplayer.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.appthemehelper.util.NavigationViewUtil;
import com.kabouzeid.chenlongcould.musicplayer.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import top.geek_studio.chenlongcould.musicplayer.App;
import top.geek_studio.chenlongcould.musicplayer.dialogs.ChangelogDialog;
import top.geek_studio.chenlongcould.musicplayer.dialogs.ScanMediaFolderChooserDialog;
import top.geek_studio.chenlongcould.musicplayer.glide.SongGlideRequest;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.helper.SearchQueryHelper;
import top.geek_studio.chenlongcould.musicplayer.live2d.LAppLive2DManager;
import top.geek_studio.chenlongcould.musicplayer.live2d.LAppView;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.FileManager;
import top.geek_studio.chenlongcould.musicplayer.live2d.utils.android.SoundManager;
import top.geek_studio.chenlongcould.musicplayer.loader.AlbumLoader;
import top.geek_studio.chenlongcould.musicplayer.loader.ArtistLoader;
import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistSongLoader;
import top.geek_studio.chenlongcould.musicplayer.model.DataViewModel;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.service.MusicService;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.base.AbsSlidingMusicPanelActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.intro.AppIntroActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.AbsMainActivityFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.folders.FoldersFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.LibraryFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.HomeFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.netsearch.NetSearchFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.yuepic.SongPicFragment;
import top.geek_studio.chenlongcould.musicplayer.util.CSutil;
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;
import top.geek_studio.chenlongcould.musicplayer.util.RemoteConfigUtil;

/**
 * MainActivity {@link R.layout#activity_main_drawer_layout}
 *
 * @author chenlongcould (Modify, add notes)
 */
public class MainActivity extends AbsSlidingMusicPanelActivity {

    /**
     * TAG
     */
    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int RC_SIGN_IN = 102;

    /**
     * 标志位, 需要展示 INTRO
     */
    public static final int APP_INTRO_REQUEST = 100;

    // ...
    public static final int PURCHASE_REQUEST = 101;

    /**
     * 展示 {@link LibraryFragment}
     */
    private static final int LIBRARY = 0;

    /**
     * 展示 {@link FoldersFragment}
     */
    private static final int FOLDERS = 1;

    private static final int NET_SEARCH = 2;

    private static final int YUEPIC = 4;

    /**
     * Drawer 菜单点击延迟, 用于点击item, 等待 Drawer 收起, 再进行操作
     */
    private static final short DRAWER_WAIT_TIME = 220;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Nullable
    MainActivityFragmentCallbacks currentFragment;

    ////////////////// LIVE 2D ////////////////////
    private LAppLive2DManager live2DMgr;
    private LAppView mlAppView;
    private FrameLayout mLive2DContent;
    ////////////////// LIVE 2D ////////////////////

    public DataViewModel mViewModel;

    public Handler handler;

    @Nullable
    private View navigationDrawerHeader;

    private boolean blockRequestPermissions;

    /**
     * 用于检测两次返回
     */
    private boolean pressBack = false;

    private DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            // 设置模糊位移
            MainActivity.super.setBlur(slideOffset, false);
            MainActivity.super.translationRootView(slideOffset, 'x');
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {

        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {

        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            navigationView.setFitsSystemWindows(false); // for header to go below statusbar
        }

        setUpDrawerLayout();

        if (savedInstanceState == null) {
            setMusicChooser(PreferenceUtil.getInstance(this).getLastMusicChooser());
        } else {
            restoreCurrentFragment();
        }

        if (!checkShowIntro()) {
            showChangelog();
        }

        mViewModel = ViewModelProviders.of(this).get(DataViewModel.class);

        RemoteConfigUtil.checkAllowUseNetPlayer(mViewModel);

        CSutil.checkUpdate(this);

        handler = new Handler();

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
            Toast.makeText(this, "Google Play Services is not available!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置 Live2D
     */
    private void setUpLive2D() {
        FileManager.init(this);
        SoundManager.init(this);

        live2DMgr = new LAppLive2DManager();

        mlAppView = live2DMgr.createView(this, true);
        mlAppView.setBackground(null);

        mLive2DContent = new FrameLayout(this);
        mLive2DContent.addView(mlAppView);

        navigationView.addView(mLive2DContent);

        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mlAppView.getLayoutParams();
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.width = 400;
        lp.height = 800;

        mlAppView.setLongClickable(true);
        mlAppView.setOnLongClickListener(view -> {
            new MaterialDialog.Builder(MainActivity.this).title("Hi")
                    .content("这里是 ACG Player 助手酱 ~")
                    .showListener(dialogInterface -> mViewModel.dialogs.add(dialogInterface))
                    .dismissListener(dialogInterface -> mViewModel.dialogs.remove(dialogInterface))
                    .negativeText("Change Me!")
                    .onNegative((dialog, which) -> live2DMgr.changeModel())
                    .show();
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mlAppView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        setUpLive2D();
        if (mlAppView != null) mlAppView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        FileManager.clear();
//        SoundManager.release();
//        mLive2DContent.removeAllViews();
//        mlAppView = null;
//        mLive2DContent = null;

        for (final DialogInterface d : mViewModel.dialogs) {
            if (d != null) d.dismiss();
        }

        mViewModel.dialogs.clear();
    }

    /**
     * 音乐选择项, 用于判断加载何种 Fragment
     *
     * @param key {@link LibraryFragment} and {@link FoldersFragment}
     */
    private void setMusicChooser(int key) {

        // 非 PRO 版本禁用文件夹
        if (!App.Companion.isProVersion() && key == FOLDERS) {
            Toast.makeText(this, R.string.folder_view_is_a_pro_feature, Toast.LENGTH_LONG).show();
            startActivityForResult(new Intent(this, PurchaseActivity.class), PURCHASE_REQUEST);
            key = LIBRARY;
        }

        // 更新最后选择
        PreferenceUtil.getInstance(this).setLastMusicChooser(key);

        AbsMainActivityFragment targetFrag = null;

        switch (key) {
            case LIBRARY:
//                if (currentFragment instanceof LibraryFragment) return;

                navigationView.setCheckedItem(R.id.nav_library);

                targetFrag = LibraryFragment.newInstance();
                break;
            case FOLDERS:
//                if (currentFragment instanceof FoldersFragment) return;

                navigationView.setCheckedItem(R.id.nav_folders);

                targetFrag = FoldersFragment.newInstance(this);
                break;
            case NET_SEARCH:
//                if (currentFragment instanceof NetSearchFragment) return;

                // 检测服务是否可以访问
                if (mViewModel != null && mViewModel.allowUseNetPlayer.getValue() != null
                        && !mViewModel.allowUseNetPlayer.getValue()) {
                    Toast.makeText(this, "Not Allow!", Toast.LENGTH_SHORT).show();
                    return;
                }

                navigationView.setCheckedItem(R.id.nav_net_search);

                targetFrag = NetSearchFragment.newInstance();
                break;
            case YUEPIC:
                navigationView.setCheckedItem(R.id.nav_yuepic);

                targetFrag = SongPicFragment.newInstance();

                break;
        }

        if (targetFrag != null) {
//            setCurrentFragment(targetFrag);

            final AbsMainActivityFragment finalTargetFrag = targetFrag;
            if (currentFragment != null) {
                currentFragment.hide(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        setCurrentFragment(finalTargetFrag);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        setCurrentFragment(finalTargetFrag);
                    }
                });
            } else {
                setCurrentFragment(finalTargetFrag);
            }

        }
    }

    /**
     * 设置 Fragment
     *
     * @param fragment frag
     */
    private void setCurrentFragment(@NonNull final Fragment fragment) {
        // 置换
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, null)
                .commit();

        // 更新
        currentFragment = (MainActivityFragmentCallbacks) fragment;
    }

    /**
     * 重置当前 Fragment
     */
    private void restoreCurrentFragment() {
        // 置换
        currentFragment = (MainActivityFragmentCallbacks) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode);

        switch (requestCode) {
            case APP_INTRO_REQUEST:
                blockRequestPermissions = false;
                if (!hasPermissions()) {
                    requestPermissions();
                }
                checkSetUpPro(); // good chance that pro version check was delayed on first start

                break;
            case PURCHASE_REQUEST:
                if (resultCode == RESULT_OK) {
                    checkSetUpPro();
                }
                break;
            case RC_SIGN_IN:
                IdpResponse response = IdpResponse.fromResultIntent(data);
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "onActivityResult: OK");
                    // Successfully signed in
                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        mViewModel.userData.postValue(user);

                        if (currentFragment instanceof LibraryFragment) {
                            final Fragment fragment = ((LibraryFragment) currentFragment).getCurrentFragment();
                            if (fragment instanceof HomeFragment) {
                                ((HomeFragment) fragment).setUpUserData(user);
                            }
                        }
                    }
                } else {
                    // Sign in failed. If response is null the user canceled the
                    // sign-in flow using the back button. Otherwise check
                    // response.getError().getErrorCode() and handle the error.
                    // ...
                    Log.d(TAG, "onActivityResult: Failed");
                    if (response != null) {
                        FirebaseUiException exception = response.getError();
                        if (exception != null) {
                            Toast.makeText(this, exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions();
    }

    @Override
    protected View createContentView() {
        @SuppressLint("InflateParams") final View contentView = getLayoutInflater().inflate(R.layout.activity_main_drawer_layout, null);
        final ViewGroup drawerContent = contentView.findViewById(R.id.drawer_content_container);
        drawerContent.addView(wrapSlidingMusicPanel(R.layout.activity_main_content));
        return contentView;
    }

    /**
     * 设置 {@link NavigationView}
     */
    private void setUpNavigationView() {
        // 获取强调色
        int accentColor = ThemeStore.accentColor(this);

        // 设置 NavView 选中项的颜色
        NavigationViewUtil.setItemIconColors(navigationView, ATHUtil.resolveColor(this, R.attr.iconColor, ThemeStore.textColorSecondary(this)), accentColor);
        NavigationViewUtil.setItemTextColors(navigationView, ThemeStore.textColorPrimary(this), accentColor);

        checkSetUpPro();

        // 侧滑菜单
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            drawerLayout.closeDrawers();
            switch (menuItem.getItemId()) {
                case R.id.nav_library:
                    new Handler().postDelayed(() -> setMusicChooser(LIBRARY), DRAWER_WAIT_TIME);
                    break;
                case R.id.nav_folders:
                    new Handler().postDelayed(() -> setMusicChooser(FOLDERS), DRAWER_WAIT_TIME);
                    break;
                case R.id.nav_net_search:
                    new Handler().postDelayed(() -> setMusicChooser(NET_SEARCH), DRAWER_WAIT_TIME);
                    break;
                case R.id.nav_yuepic:
                    new Handler().postDelayed(() -> setMusicChooser(YUEPIC), DRAWER_WAIT_TIME);
                    break;
                case R.id.buy_pro:
                    new Handler().postDelayed(() -> startActivityForResult(new Intent(MainActivity.this, PurchaseActivity.class), PURCHASE_REQUEST), DRAWER_WAIT_TIME);
                    break;
                case R.id.action_scan:
                    new Handler().postDelayed(() -> {
                        ScanMediaFolderChooserDialog dialog = ScanMediaFolderChooserDialog.create();
                        dialog.show(getSupportFragmentManager(), "SCAN_MEDIA_FOLDER_CHOOSER");
                    }, DRAWER_WAIT_TIME);
                    break;
                case R.id.nav_settings:
                    new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)), DRAWER_WAIT_TIME);
                    break;
                case R.id.nav_about:
                    new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, AboutActivity.class)), DRAWER_WAIT_TIME);
                    break;
                case R.id.nav_data_overview:
                    // TODO: DATA OVERVIEW
                    new MaterialDialog.Builder(MainActivity.this).title("Building").content("Building...").showListener(dialog -> mViewModel.dialogs.add(dialog)).show();
                    break;
                case R.id.nav_exit:
                    stopService(new Intent(MainActivity.this, MusicService.class));
                    finish();
                    break;
            }
            return true;
        });
    }

    /**
     * 检查是否为专业版
     */
    private void checkSetUpPro() {
        if (App.Companion.isProVersion()) {
            setUpPro();
        }
    }

    /**
     * 设置 PRO
     */
    private void setUpPro() {
        navigationView.getMenu().removeGroup(R.id.navigation_drawer_menu_category_buy_pro);
    }

    /**
     * 设置 {@link DrawerLayout}
     */
    private void setUpDrawerLayout() {
        setUpNavigationView();

        // 设置覆盖颜色
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        drawerLayout.addDrawerListener(drawerListener);
    }

    /**
     * 更新 HeaderView
     */
    private void updateNavigationDrawerHeader() {
        // 如果播放队列不为空则加载专辑图否则移除 HeaderView
        if (!MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            final Song song = MusicPlayerRemote.getCurrentSong();
            if (navigationDrawerHeader == null) {
                navigationDrawerHeader = navigationView.inflateHeaderView(R.layout.navigation_drawer_header);
                navigationDrawerHeader.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    new Handler().postDelayed(() -> {
                        if (getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                            expandPanel();
                        }
                    }, DRAWER_WAIT_TIME);

                });
            }
            ((TextView) navigationDrawerHeader.findViewById(R.id.title)).setText(song.title);
            ((TextView) navigationDrawerHeader.findViewById(R.id.text)).setText(MusicUtil.getSongInfoString(song));
            SongGlideRequest.Builder.from(Glide.with(this), song)
                    .checkIgnoreMediaStore(this).build()
                    .into(((ImageView) navigationDrawerHeader.findViewById(R.id.image)));
        } else {
            if (navigationDrawerHeader != null) {
                navigationView.removeHeaderView(navigationDrawerHeader);
                navigationDrawerHeader = null;
            }
        }
    }

    /**
     * 当媒体改变时，变更 HeaderView
     */
    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        updateNavigationDrawerHeader();
    }

    /**
     * 当服务连接时，变更 HeaderView. 检测 Intent 传递
     */
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        updateNavigationDrawerHeader();
        handlePlaybackIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handleBackPress() {

        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
            return true;
        }

        // 处理上层及当前 Fragment 返回
        if (super.handleBackPress() || (currentFragment != null && currentFragment.handleBackPress()))
            return true;

        // 按两次退出
        if (!pressBack) {
            pressBack = true;
            Toast.makeText(this, "Press again to exit!", Toast.LENGTH_SHORT).show();
            final short delayMillis = 2000;
            new Handler().postDelayed(() -> pressBack = false, delayMillis);
            return true;
        } else {
            finish();
            return true;
        }
    }

    /**
     * 检测 Intent，参考文件管理器打开 {@code .mp3} 文件
     *
     * @param intent intent
     */
    private void handlePlaybackIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        final Uri uri = intent.getData();

        // 数据媒体类型
        final String mimeType = intent.getType();

        // 标志位，是否处理完数据
        boolean handled = false;

        final Bundle ext = intent.getExtras();
        if (ext == null) {
            Toast.makeText(this, "handlePlaybackIntent: Ext is null", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检测是否来自搜索
        if (intent.getAction() != null && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            final List<Song> songs = SearchQueryHelper.getSongs(this, ext);
            MusicPlayerRemote.openQueue(songs, MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE, true);
            handled = true;
        }

        final int position = intent.getIntExtra("position", 0);

        // 播放指定 URI 歌曲
        if (uri != null && uri.toString().length() > 0) {
            MusicPlayerRemote.playFromUri(uri);
            handled = true;

            // 检测是否匹配到播放列表
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "playlistId", "playlist");
            if (id >= 0) {
                final List<Song> songs = new ArrayList<>(PlaylistSongLoader.getPlaylistSongList(this, id));
                MusicPlayerRemote.openQueue(songs, position, true);
                handled = true;
            }

            // 检测是否匹配专辑
        } else if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "albumId", "album");
            if (id >= 0) {
                MusicPlayerRemote.openQueue(AlbumLoader.getAlbum(this, id).songs, position, true);
                handled = true;
            }

            // 检测是否匹配艺术家
        } else if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "artistId", "artist");
            if (id >= 0) {
                MusicPlayerRemote.openQueue(ArtistLoader.getArtist(this, id).getSongs(), position, true);
                handled = true;
            }
        }

        // 如果处理到数据，执行
        if (handled) {
            setIntent(new Intent());
        }
    }

    /**
     * 获取 ID
     *
     * @param intent    intent
     * @param longKey   k1
     * @param stringKey k2
     *
     * @return ID
     */
    private long parseIdFromIntent(@NonNull Intent intent, String longKey,
                                   String stringKey) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            final String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, String.valueOf(e.getMessage()));
                }
            }
        }
        return id;
    }

    /**
     * {@link SlidingUpPanelLayout} 滑动模式回调
     */
    @Override
    public void onPanelExpanded(View view) {
        super.onPanelExpanded(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    /**
     * {@link SlidingUpPanelLayout} 滑动模式回调
     */
    @Override
    public void onPanelCollapsed(View view) {
        super.onPanelCollapsed(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    /**
     * 检测是否需要显示 Intro {@link AppIntroActivity}
     */
    private boolean checkShowIntro() {
        if (!PreferenceUtil.getInstance(this).introShown()) {
            PreferenceUtil.getInstance(this).setIntroShown();
            ChangelogDialog.setChangelogRead(this);
            blockRequestPermissions = true;
            new Handler().postDelayed(() -> startActivityForResult(new Intent(MainActivity.this, AppIntroActivity.class), APP_INTRO_REQUEST), 50);
            return true;
        }
        return false;
    }

    /**
     * 显示变更对话框
     */
    private void showChangelog() {
        try {
            final PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int currentVersion = pInfo.versionCode;
            // 当版本不一致时, 显示变更对话框
            if (currentVersion != PreferenceUtil.getInstance(this).getLastChangelogVersion()) {
                ChangelogDialog.create().show(getSupportFragmentManager(), "CHANGE_LOG_DIALOG");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void startLoginActivity() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build()
//                        new AuthUI.IdpConfig.GoogleBuilder().build(),
//                        new AuthUI.IdpConfig.FacebookBuilder().build(),
//                        new AuthUI.IdpConfig.TwitterBuilder().build()
        );

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.mipmap.ic_launcher_round)      // Set logo drawable
                        .setTheme(PreferenceUtil.getInstance(getApplicationContext()).getGeneralTheme())
                        .setTosAndPrivacyPolicyUrls(
                                "https://example.com/terms.html",
                                "https://example.com/privacy.html")
                        .build(),
                MainActivity.RC_SIGN_IN);
    }

    /**
     * 退出登陆
     */
    public void logout() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> mViewModel.userData.setValue(null));
    }

    /**
     * 用于 Fragment 回调
     */
    public interface MainActivityFragmentCallbacks {
        boolean handleBackPress();

        void hide(@Nullable AnimatorListenerAdapter adapter);

        void show(@Nullable AnimatorListenerAdapter adapter);
    }
}
