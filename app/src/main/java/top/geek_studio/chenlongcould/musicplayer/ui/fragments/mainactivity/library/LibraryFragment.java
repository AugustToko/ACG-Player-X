package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialcab.MaterialCab;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.common.ATHToolbarActivity;
import com.kabouzeid.appthemehelper.util.TabLayoutUtil;
import com.kabouzeid.appthemehelper.util.ToolbarContentTintHelper;
import com.kabouzeid.chenlongcould.musicplayer.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import top.geek_studio.chenlongcould.musicplayer.adapter.MusicLibraryPagerAdapter;
import top.geek_studio.chenlongcould.musicplayer.dialogs.CreatePlaylistDialog;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.helper.SortOrder;
import top.geek_studio.chenlongcould.musicplayer.interfaces.CabHolder;
import top.geek_studio.chenlongcould.musicplayer.loader.SongLoader;
import top.geek_studio.chenlongcould.musicplayer.preferences.LibraryPreferenceDialog;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.SearchActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.AbsMainActivityFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.AbsLibraryPagerFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.AbsLibraryPagerRecyclerViewCustomGridSizeFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.AlbumsFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.ArtistsFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.PlaylistsFragment;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.SongsFragment;
import top.geek_studio.chenlongcould.musicplayer.util.PhonographColorUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;
import top.geek_studio.chenlongcould.musicplayer.util.Util;

/**
 * 媒体库 Fragment
 */
public class LibraryFragment extends AbsMainActivityFragment
        implements CabHolder, MainActivity.MainActivityFragmentCallbacks, ViewPager.OnPageChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = LibraryFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.appbar)
    AppBarLayout appbar;
    @BindView(R.id.pager)
    ViewPager pager;

    private MusicLibraryPagerAdapter pagerAdapter;

    private MaterialCab cab;

    /**
     * 工厂
     */
    public static LibraryFragment newInstance() {
        return new LibraryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_library, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        PreferenceUtil.getInstance(getActivity()).unregisterOnSharedPreferenceChangedListener(this);
        super.onDestroyView();
        pager.removeOnPageChangeListener(this);
        unbinder.unbind();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PreferenceUtil.getInstance(getActivity()).registerOnSharedPreferenceChangedListener(this);
        getMainActivity().setStatusbarColorAuto();
        getMainActivity().setNavigationbarColorAuto();
        getMainActivity().setTaskDescriptionColorAuto();

        setUpToolbar();
        setUpViewPager();
    }

    /**
     * 用于更新 {@link ViewPager}, {@link android.widget.TableLayout} 页面变化
     *
     * @param preferences prefs
     * @param key         prefs TAG
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        // 检测是否匹配
        if (PreferenceUtil.LIBRARY_CATEGORIES.equals(key)) {
            final Fragment current = getCurrentFragment();
            pagerAdapter.setCategoryInfos(PreferenceUtil.getInstance(getActivity()).getLibraryCategoryInfos());
            // 缓存设置
            pager.setOffscreenPageLimit(pagerAdapter.getCount() - 1);
            int position = pagerAdapter.getItemPosition(current);
            if (position < 0) position = 0;
            pager.setCurrentItem(position);
            PreferenceUtil.getInstance(getContext()).setLastPage(position);

            updateTabVisibility();
        }
    }

    private void setUpToolbar() {
        int primaryColor = ThemeStore.primaryColor(getActivity());
        appbar.setBackgroundColor(primaryColor);
        toolbar.setBackgroundColor(primaryColor);
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        getActivity().setTitle(R.string.app_name);
        getMainActivity().setSupportActionBar(toolbar);
    }

    /**
     * 设置 ViewPager
     */
    private void setUpViewPager() {
        final Activity activity = getActivity();

        if (activity == null) return;

        pagerAdapter = new MusicLibraryPagerAdapter(getActivity(), getChildFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(pagerAdapter.getCount() - 1);

        tabs.setupWithViewPager(pager);

        @ColorInt int primaryColor = ThemeStore.primaryColor(activity);
        @ColorInt int normalColor = ToolbarContentTintHelper.toolbarSubtitleColor(activity, primaryColor);
        @ColorInt int selectedColor = ToolbarContentTintHelper.toolbarTitleColor(activity, primaryColor);

        // 颜色
        TabLayoutUtil.setTabIconColors(tabs, normalColor, selectedColor);
        tabs.setTabTextColors(normalColor, selectedColor);
        tabs.setSelectedTabIndicatorColor(ThemeStore.accentColor(activity));

        // 设置 TAB 是否可见
        updateTabVisibility();

        // 恢复最后打开 app 所在页
        if (PreferenceUtil.getInstance(activity).rememberLastTab()) {
            final int lastIndex = PreferenceUtil.getInstance(activity).getLastPage();
            pager.setCurrentItem(lastIndex);
            new Handler().postDelayed(() -> updateSubTitle(getFragmentSubTitle(lastIndex)), 3000);
        }

        pager.addOnPageChangeListener(this);

        /*
         * 添加长按 Tab , 弹出 tab 排序、位置设置 dialog
         * */
        final FragmentManager fragmentManager = getFragmentManager();
        final LibraryPreferenceDialog libraryPreferenceDialog = LibraryPreferenceDialog.newInstance();

        if (fragmentManager != null) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                final TabLayout.Tab tab = tabs.getTabAt(i);
                if (tab != null) {
                    ((View) tab.view).setOnLongClickListener(v -> {
                        // DialogFragment 已经自行实现消失后的移除事务
                        libraryPreferenceDialog.show(fragmentManager, "LibraryPreferenceDialog");
                        return false;
                    });
                }
            }
        }
    }

    /**
     * Hide the tab bar when only a single tab is visible
     */
    private void updateTabVisibility() {
        tabs.setVisibility(pagerAdapter.getCount() == 1 ? View.GONE : View.VISIBLE);
    }

    /**
     * 获取当前 Fragment
     */
    public Fragment getCurrentFragment() {
        return pagerAdapter.getFragment(pager.getCurrentItem());
    }

    /**
     * 检测是否为播放列表 Fragment
     */
    private boolean isPlaylistPage() {
        return getCurrentFragment() instanceof PlaylistsFragment;
    }

    @NonNull
    @Override
    public MaterialCab openCab(final int menuRes, final MaterialCab.Callback callback) {
        if (cab != null && cab.isActive()) cab.finish();
        cab = new MaterialCab(getMainActivity(), R.id.cab_stub)
                .setMenu(menuRes)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(PhonographColorUtil.shiftBackgroundColorForLightText(ThemeStore.primaryColor(getActivity())))
                .start(callback);
        return cab;
    }

    public void addOnAppBarOffsetChangedListener(AppBarLayout.OnOffsetChangedListener onOffsetChangedListener) {
        appbar.addOnOffsetChangedListener(onOffsetChangedListener);
    }

    public void removeOnAppBarOffsetChangedListener(AppBarLayout.OnOffsetChangedListener onOffsetChangedListener) {
        appbar.removeOnOffsetChangedListener(onOffsetChangedListener);
    }

    public int getTotalAppBarScrollingRange() {
        return appbar.getTotalScrollRange();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (pager == null) return;

        // 如果为播放列表页面，添加按钮(添加播放列表)
        inflater.inflate(R.menu.menu_main, menu);
        if (isPlaylistPage()) {
            menu.add(0, R.id.action_new_playlist, 0, R.string.new_playlist_title);
        }

        /*
         * menu 动态设置
         * */
        final Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof AbsLibraryPagerRecyclerViewCustomGridSizeFragment && currentFragment.isAdded()) {
            final AbsLibraryPagerRecyclerViewCustomGridSizeFragment absLibraryRecyclerViewCustomGridSizeFragment
                    = (AbsLibraryPagerRecyclerViewCustomGridSizeFragment) currentFragment;

            final MenuItem gridSizeItem = menu.findItem(R.id.action_grid_size);
            if (Util.isLandscape(getResources())) {
                gridSizeItem.setTitle(R.string.action_grid_size_land);
            }
            setUpGridSizeMenu(absLibraryRecyclerViewCustomGridSizeFragment, gridSizeItem.getSubMenu());

            menu.findItem(R.id.action_colored_footers).setChecked(absLibraryRecyclerViewCustomGridSizeFragment.usePalette());
            menu.findItem(R.id.action_colored_footers).setEnabled(absLibraryRecyclerViewCustomGridSizeFragment.canUsePalette());

            setUpSortOrderMenu(absLibraryRecyclerViewCustomGridSizeFragment, menu.findItem(R.id.action_sort_order).getSubMenu());
        } else {
            menu.removeItem(R.id.action_grid_size);
            menu.removeItem(R.id.action_colored_footers);
            menu.removeItem(R.id.action_sort_order);
        }

        final Activity activity = getActivity();
        if (activity == null) return;
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(getActivity(), toolbar, menu, ATHToolbarActivity.getToolbarBackgroundColor(toolbar));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final Activity activity = getActivity();
        if (activity == null) return;
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(activity, toolbar);
    }

    /**
     * Menu 点击
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (pager == null) return false;
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof AbsLibraryPagerRecyclerViewCustomGridSizeFragment) {
            AbsLibraryPagerRecyclerViewCustomGridSizeFragment absLibraryRecyclerViewCustomGridSizeFragment = (AbsLibraryPagerRecyclerViewCustomGridSizeFragment) currentFragment;
            if (item.getItemId() == R.id.action_colored_footers) {
                item.setChecked(!item.isChecked());
                absLibraryRecyclerViewCustomGridSizeFragment.setAndSaveUsePalette(item.isChecked());
                return true;
            }
            if (handleGridSizeMenuItem(absLibraryRecyclerViewCustomGridSizeFragment, item)) {
                return true;
            }
            if (handleSortOrderMenuItem(absLibraryRecyclerViewCustomGridSizeFragment, item)) {
                return true;
            }
        }

        int id = item.getItemId();
        switch (id) {
            case R.id.action_shuffle_all:
                MusicPlayerRemote.openAndShuffleQueue(SongLoader.getAllSongs(getActivity()), true);
                return true;
            case R.id.action_new_playlist:
                CreatePlaylistDialog.create().show(getChildFragmentManager(), "CREATE_PLAYLIST");
                return true;
            case R.id.action_search:
                startActivity(new Intent(getActivity(), SearchActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 设置 RecyclerView Grid
     *
     * @param fragment     frag
     * @param gridSizeMenu menu
     */
    private void setUpGridSizeMenu(@NonNull AbsLibraryPagerRecyclerViewCustomGridSizeFragment fragment, @NonNull SubMenu gridSizeMenu) {
        switch (fragment.getGridSize()) {
            case 1:
                gridSizeMenu.findItem(R.id.action_grid_size_1).setChecked(true);
                break;
            case 2:
                gridSizeMenu.findItem(R.id.action_grid_size_2).setChecked(true);
                break;
            case 3:
                gridSizeMenu.findItem(R.id.action_grid_size_3).setChecked(true);
                break;
            case 4:
                gridSizeMenu.findItem(R.id.action_grid_size_4).setChecked(true);
                break;
            case 5:
                gridSizeMenu.findItem(R.id.action_grid_size_5).setChecked(true);
                break;
            case 6:
                gridSizeMenu.findItem(R.id.action_grid_size_6).setChecked(true);
                break;
            case 7:
                gridSizeMenu.findItem(R.id.action_grid_size_7).setChecked(true);
                break;
            case 8:
                gridSizeMenu.findItem(R.id.action_grid_size_8).setChecked(true);
                break;
        }
        int maxGridSize = fragment.getMaxGridSize();
        if (maxGridSize < 8) {
            gridSizeMenu.findItem(R.id.action_grid_size_8).setVisible(false);
        }
        if (maxGridSize < 7) {
            gridSizeMenu.findItem(R.id.action_grid_size_7).setVisible(false);
        }
        if (maxGridSize < 6) {
            gridSizeMenu.findItem(R.id.action_grid_size_6).setVisible(false);
        }
        if (maxGridSize < 5) {
            gridSizeMenu.findItem(R.id.action_grid_size_5).setVisible(false);
        }
        if (maxGridSize < 4) {
            gridSizeMenu.findItem(R.id.action_grid_size_4).setVisible(false);
        }
        if (maxGridSize < 3) {
            gridSizeMenu.findItem(R.id.action_grid_size_3).setVisible(false);
        }
    }

    /**
     * 处理网格大小
     *
     * @param fragment frag
     * @param item     menu
     */
    private boolean handleGridSizeMenuItem(@NonNull AbsLibraryPagerRecyclerViewCustomGridSizeFragment fragment, @NonNull MenuItem item) {
        int gridSize = 0;
        switch (item.getItemId()) {
            case R.id.action_grid_size_1:
                gridSize = 1;
                break;
            case R.id.action_grid_size_2:
                gridSize = 2;
                break;
            case R.id.action_grid_size_3:
                gridSize = 3;
                break;
            case R.id.action_grid_size_4:
                gridSize = 4;
                break;
            case R.id.action_grid_size_5:
                gridSize = 5;
                break;
            case R.id.action_grid_size_6:
                gridSize = 6;
                break;
            case R.id.action_grid_size_7:
                gridSize = 7;
                break;
            case R.id.action_grid_size_8:
                gridSize = 8;
                break;
        }
        if (gridSize > 0) {
            item.setChecked(true);
            fragment.setAndSaveGridSize(gridSize);
            toolbar.getMenu().findItem(R.id.action_colored_footers).setEnabled(fragment.canUsePalette());
            return true;
        }
        return false;
    }

    /**
     * 排序处理
     *
     * @param fragment      frag
     * @param sortOrderMenu 子menu, (排序)
     */
    private void setUpSortOrderMenu(@NonNull AbsLibraryPagerRecyclerViewCustomGridSizeFragment fragment, @NonNull SubMenu sortOrderMenu) {
        // 获取排序
        final String currentSortOrder = fragment.getSortOrder();
        sortOrderMenu.clear();

        // album
        if (fragment instanceof AlbumsFragment) {
            sortOrderMenu.add(0, R.id.action_album_sort_order_asc, 0, R.string.sort_order_a_z)
                    .setChecked(currentSortOrder.equals(SortOrder.AlbumSortOrder.ALBUM_A_Z));
            sortOrderMenu.add(0, R.id.action_album_sort_order_desc, 1, R.string.sort_order_z_a)
                    .setChecked(currentSortOrder.equals(SortOrder.AlbumSortOrder.ALBUM_Z_A));
            sortOrderMenu.add(0, R.id.action_album_sort_order_artist, 2, R.string.sort_order_artist)
                    .setChecked(currentSortOrder.equals(SortOrder.AlbumSortOrder.ALBUM_ARTIST));
            sortOrderMenu.add(0, R.id.action_album_sort_order_year, 3, R.string.sort_order_year)
                    .setChecked(currentSortOrder.equals(SortOrder.AlbumSortOrder.ALBUM_YEAR));
            // artist
        } else if (fragment instanceof ArtistsFragment) {
            sortOrderMenu.add(0, R.id.action_artist_sort_order_asc, 0, R.string.sort_order_a_z)
                    .setChecked(currentSortOrder.equals(SortOrder.ArtistSortOrder.ARTIST_A_Z));
            sortOrderMenu.add(0, R.id.action_artist_sort_order_desc, 1, R.string.sort_order_z_a)
                    .setChecked(currentSortOrder.equals(SortOrder.ArtistSortOrder.ARTIST_Z_A));
            // song
        } else if (fragment instanceof SongsFragment) {
            sortOrderMenu.add(0, R.id.action_song_sort_order_asc, 0, R.string.sort_order_a_z)
                    .setChecked(currentSortOrder.equals(SortOrder.SongSortOrder.SONG_A_Z));
            sortOrderMenu.add(0, R.id.action_song_sort_order_desc, 1, R.string.sort_order_z_a)
                    .setChecked(currentSortOrder.equals(SortOrder.SongSortOrder.SONG_Z_A));
            sortOrderMenu.add(0, R.id.action_song_sort_order_artist, 2, R.string.sort_order_artist)
                    .setChecked(currentSortOrder.equals(SortOrder.SongSortOrder.SONG_ARTIST));
            sortOrderMenu.add(0, R.id.action_song_sort_order_album, 3, R.string.sort_order_album)
                    .setChecked(currentSortOrder.equals(SortOrder.SongSortOrder.SONG_ALBUM));
            sortOrderMenu.add(0, R.id.action_song_sort_order_year, 4, R.string.sort_order_year)
                    .setChecked(currentSortOrder.equals(SortOrder.SongSortOrder.SONG_YEAR));
        }

        sortOrderMenu.setGroupCheckable(0, true, true);
    }

    /**
     * 处理排序
     *
     * @param fragment frag
     * @param item     item
     */
    private boolean handleSortOrderMenuItem(@NonNull AbsLibraryPagerRecyclerViewCustomGridSizeFragment fragment, @NonNull MenuItem item) {
        String sortOrder = null;
        if (fragment instanceof AlbumsFragment) {
            switch (item.getItemId()) {
                case R.id.action_album_sort_order_asc:
                    sortOrder = SortOrder.AlbumSortOrder.ALBUM_A_Z;
                    break;
                case R.id.action_album_sort_order_desc:
                    sortOrder = SortOrder.AlbumSortOrder.ALBUM_Z_A;
                    break;
                case R.id.action_album_sort_order_artist:
                    sortOrder = SortOrder.AlbumSortOrder.ALBUM_ARTIST;
                    break;
                case R.id.action_album_sort_order_year:
                    sortOrder = SortOrder.AlbumSortOrder.ALBUM_YEAR;
                    break;
            }
        } else if (fragment instanceof ArtistsFragment) {
            switch (item.getItemId()) {
                case R.id.action_artist_sort_order_asc:
                    sortOrder = SortOrder.ArtistSortOrder.ARTIST_A_Z;
                    break;
                case R.id.action_artist_sort_order_desc:
                    sortOrder = SortOrder.ArtistSortOrder.ARTIST_Z_A;
                    break;
            }
        } else if (fragment instanceof SongsFragment) {
            switch (item.getItemId()) {
                case R.id.action_song_sort_order_asc:
                    sortOrder = SortOrder.SongSortOrder.SONG_A_Z;
                    break;
                case R.id.action_song_sort_order_desc:
                    sortOrder = SortOrder.SongSortOrder.SONG_Z_A;
                    break;
                case R.id.action_song_sort_order_artist:
                    sortOrder = SortOrder.SongSortOrder.SONG_ARTIST;
                    break;
                case R.id.action_song_sort_order_album:
                    sortOrder = SortOrder.SongSortOrder.SONG_ALBUM;
                    break;
                case R.id.action_song_sort_order_year:
                    sortOrder = SortOrder.SongSortOrder.SONG_YEAR;
                    break;
            }
        }

        // 更新排序
        if (sortOrder != null) {
            item.setChecked(true);
            fragment.setAndSaveSortOrder(sortOrder);
            return true;
        }

        return false;
    }

    /**
     * 处理返回
     */
    @Override
    public boolean handleBackPress() {
        if (cab != null && cab.isActive()) {
            cab.finish();
            return true;
        }
        return false;
    }

    // --------------------------------- ViewPager -------------------------------

    /**
     * {@link ViewPager}
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        /////////
    }

    /**
     * {@link ViewPager}
     * <p>
     * 更新最后（打开的）一页
     */
    @Override
    public void onPageSelected(int position) {
        final Activity activity = getActivity();
        if (activity != null) PreferenceUtil.getInstance(getActivity()).setLastPage(position);

        updateSubTitle(getFragmentSubTitle(position));
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //...
    }

    // --------------------------------- ViewPager -------------------------------

    /**
     * 更新 subtitle
     *
     * @param subTitle subtitle Text
     */
    private void updateSubTitle(@Nullable String subTitle) {
        if (subTitle == null) subTitle = "-";
        toolbar.setSubtitle(subTitle);
    }

    /**
     * 获取 Fragment 对应 subtitle
     *
     * @param position pager index
     *
     * @return subtitle
     */
    @NonNull
    private String getFragmentSubTitle(final int position) {
        final AbsLibraryPagerFragment fragment = (AbsLibraryPagerFragment) pagerAdapter.getFragment(position);
        if (fragment == null) return "-";
        return fragment.getSubTitle();
    }
}
