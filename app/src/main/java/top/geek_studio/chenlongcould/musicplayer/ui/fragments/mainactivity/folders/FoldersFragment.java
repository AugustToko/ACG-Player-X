package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.folders;


import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;

import com.afollestad.materialcab.MaterialCab;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.common.ATHToolbarActivity;
import com.kabouzeid.appthemehelper.util.ToolbarContentTintHelper;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import top.geek_studio.chenlongcould.musicplayer.adapter.SongFileAdapter;
import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.helper.SortOrder;
import top.geek_studio.chenlongcould.musicplayer.helper.menu.SongMenuHelper;
import top.geek_studio.chenlongcould.musicplayer.helper.menu.SongsMenuHelper;
import top.geek_studio.chenlongcould.musicplayer.interfaces.CabHolder;
import top.geek_studio.chenlongcould.musicplayer.interfaces.LoaderIds;
import top.geek_studio.chenlongcould.musicplayer.misc.DialogAsyncTask;
import top.geek_studio.chenlongcould.musicplayer.misc.UpdateToastMediaScannerCompletionListener;
import top.geek_studio.chenlongcould.musicplayer.misc.WrappedAsyncTaskLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.AbsMainActivityFragment;
import top.geek_studio.chenlongcould.musicplayer.util.FileUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PhonographColorUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;
import top.geek_studio.chenlongcould.musicplayer.util.ViewUtil;
import top.geek_studio.chenlongcould.musicplayer.views.BreadCrumbLayout;

import static top.geek_studio.chenlongcould.musicplayer.views.BreadCrumbLayout.Crumb;
import static top.geek_studio.chenlongcould.musicplayer.views.BreadCrumbLayout.SelectionCallback;

public class FoldersFragment extends AbsMainActivityFragment implements CabHolder, SelectionCallback, SongFileAdapter.Callbacks, AppBarLayout.OnOffsetChangedListener, LoaderManager.LoaderCallbacks<List<File>> {

    private static final int LOADER_ID = LoaderIds.FOLDERS_FRAGMENT;

    protected static final String PATH = "path";
    protected static final String CRUMBS = "crumbs";
    private static final String TAG = FoldersFragment.class.getSimpleName();

    private Unbinder unbinder;

    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.container)
    View container;
    @BindView(android.R.id.empty)
    View empty;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.bread_crumbs)
    BreadCrumbLayout breadCrumbs;
    @BindView(R.id.appbar)
    AppBarLayout appbar;
    @BindView(R.id.recycler_view)
    FastScrollRecyclerView recyclerView;

    private ViewGroup root;

    private SongFileAdapter adapter;
    private MaterialCab cab;

    public static final String A_Z_ORDER = "FOLDERS_ORDER_A_Z";
    public static final String Z_A_ORDER = "FOLDERS_ORDER_Z_A";
    public static final String DATE_ORDER = "FOLDERS_ORDER_DATE";
    public static final String DATE_ORDER_REV = "FOLDERS_ORDER_DATE_DESC";

    /**
     * 当前排序模式
     */
    private String currentSortOrder;

    public static FoldersFragment newInstance(Context context) {
        return newInstance(PreferenceUtil.getInstance(context).getStartDirectory());
    }

    public static FoldersFragment newInstance(File directory) {
        FoldersFragment frag = new FoldersFragment();
        Bundle b = new Bundle();
        b.putSerializable(PATH, directory);
        frag.setArguments(b);
        return frag;
    }

    public void setCrumb(Crumb crumb, boolean addToHistory) {
        if (crumb == null) return;
        saveScrollPosition();
        breadCrumbs.setActiveOrAdd(crumb, false);
        if (addToHistory) {
            breadCrumbs.addHistory(crumb);
        }
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    private void saveScrollPosition() {
        Crumb crumb = getActiveCrumb();
        if (crumb != null) {
            crumb.setScrollPosition(((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        }
    }

    @Nullable
    private Crumb getActiveCrumb() {
        return breadCrumbs != null && breadCrumbs.size() > 0 ? breadCrumbs.getCrumb(breadCrumbs.getActiveIndex()) : null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CRUMBS, breadCrumbs.getStateWrapper());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            setCrumb(new Crumb(FileUtil.safeGetCanonicalFile((File) getArguments().getSerializable(PATH))), true);
        } else {
            breadCrumbs.restoreFromStateWrapper(savedInstanceState.getParcelable(CRUMBS));
            LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
        }
        currentSortOrder = PreferenceUtil.getInstance(requireActivity()).getFolderSortOrder();
    }

    @Override
    protected ViewGroup createRootView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Fade fade = new Fade();
        fade.setDuration(200);
        setEnterTransition(fade);

        View view = inflater.inflate(R.layout.fragment_folder, container, false);
        root = (ViewGroup) view;
        unbinder = ButterKnife.bind(this, view);
        return (ViewGroup) view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getMainActivity().setStatusbarColorAuto();
        getMainActivity().setNavigationbarColorAuto();
        getMainActivity().setTaskDescriptionColorAuto();

        setUpAppbarColor();
        setUpToolbar();
        setUpBreadCrumbs();
        setUpRecyclerView();
        setUpAdapter();
    }

    private void setUpAppbarColor() {
        int primaryColor = ThemeStore.primaryColor(requireActivity());
        appbar.setBackgroundColor(primaryColor);
        toolbar.setBackgroundColor(primaryColor);
        breadCrumbs.setBackgroundColor(primaryColor);
        breadCrumbs.setActivatedContentColor(ToolbarContentTintHelper.toolbarTitleColor(requireActivity(), primaryColor));
        breadCrumbs.setDeactivatedContentColor(ToolbarContentTintHelper.toolbarSubtitleColor(requireActivity(), primaryColor));
    }

    private void setUpToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        requireActivity().setTitle(R.string.app_name);
        getMainActivity().setSupportActionBar(toolbar);
    }

    private void setUpBreadCrumbs() {
        breadCrumbs.setCallback(this);
    }

    private void setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(getActivity(), recyclerView, ThemeStore.accentColor(requireActivity()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        appbar.addOnOffsetChangedListener(this);
    }

    private void setUpAdapter() {
        adapter = new SongFileAdapter(getMainActivity(), new LinkedList<>(), R.layout.item_list, this, this);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkIsEmpty();
            }
        });
        recyclerView.setAdapter(adapter);
        checkIsEmpty();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScrollPosition();
    }

    @Override
    public void onDestroyView() {
        appbar.removeOnOffsetChangedListener(this);
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public boolean handleBackPress() {
        if (cab != null && cab.isActive()) {
            cab.finish();
            return true;
        }
        if (breadCrumbs.popHistory()) {
            setCrumb(breadCrumbs.lastHistory(), false);
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    public MaterialCab openCab(int menuRes, MaterialCab.Callback callback) {
        if (cab != null && cab.isActive()) cab.finish();
        cab = new MaterialCab(getMainActivity(), R.id.cab_stub)
                .setMenu(menuRes)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(PhonographColorUtil.shiftBackgroundColorForLightText(ThemeStore.primaryColor(getActivity())))
                .start(callback);
        return cab;
    }

    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_folders, menu);
        final Menu sortOrderMenu = menu.findItem(R.id.action_sort_folders_order).getSubMenu();
        sortOrderMenu.clear();
        sortOrderMenu.add(0, R.id.action_folders_sort_order_name, 0, R.string.sort_order_a_z)
                .setChecked(currentSortOrder.equals(A_Z_ORDER));
        sortOrderMenu.add(0, R.id.action_folders_sort_order_name_desc, 1, R.string.sort_order_z_a)
                .setChecked(currentSortOrder.equals(Z_A_ORDER));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            sortOrderMenu.add(0, R.id.action_folders_sort_order_date, 2, R.string.sort_order_date)
                    .setChecked(currentSortOrder.equals(DATE_ORDER));
            sortOrderMenu.add(0, R.id.action_folders_sort_order_date_desc, 3, R.string.sort_order_date_desc)
                    .setChecked(currentSortOrder.equals(DATE_ORDER_REV));
        }

        sortOrderMenu.setGroupCheckable(0, true, true);
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(requireActivity(), toolbar, menu, ATHToolbarActivity.getToolbarBackgroundColor(toolbar));
    }

    @Override
    public void onPrepareOptionsMenu(@NotNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), toolbar);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_go_to_start_directory:
                setCrumb(new Crumb(FileUtil.safeGetCanonicalFile(PreferenceUtil.getInstance(requireActivity()).getStartDirectory())), true);
                return true;
            case R.id.action_scan:
                Crumb crumb = getActiveCrumb();
                if (crumb != null) {
                    new ArrayListPathsAsyncTask(getActivity(), this::scanPaths).execute(new ArrayListPathsAsyncTask.LoadingInfo(crumb.getFile(), AUDIO_FILE_FILTER));
                }
                return true;
            case R.id.action_folders_sort_order_date:
                setAndSaveSortOrder(DATE_ORDER);
                item.setChecked(true);
                return true;
            case R.id.action_folders_sort_order_date_desc:
                setAndSaveSortOrder(DATE_ORDER_REV);
                item.setChecked(true);
                return true;
            case R.id.action_folders_sort_order_name:
                setAndSaveSortOrder(A_Z_ORDER);
                item.setChecked(true);
                return true;
            case R.id.action_folders_sort_order_name_desc:
                setAndSaveSortOrder(Z_A_ORDER);
                item.setChecked(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 设置和保存排序
     */
    private void setAndSaveSortOrder(final String order) {
        currentSortOrder = order;
        PreferenceUtil.getInstance(requireContext()).setFoldersSortOrder(order);
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    public static final FileFilter AUDIO_FILE_FILTER = file -> !file.isHidden() && (file.isDirectory() ||
            FileUtil.fileIsMimeType(file, "audio/*", MimeTypeMap.getSingleton()) ||
            FileUtil.fileIsMimeType(file, "application/ogg", MimeTypeMap.getSingleton()));

    @Override
    public void onCrumbSelection(Crumb crumb, int index) {
        setCrumb(crumb, true);
    }

    public static File getDefaultStartDirectory() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File startFolder;
        if (musicDir.exists() && musicDir.isDirectory()) {
            startFolder = musicDir;
        } else {
            File externalStorage = Environment.getExternalStorageDirectory();
            if (externalStorage.exists() && externalStorage.isDirectory()) {
                startFolder = externalStorage;
            } else {
                startFolder = new File("/"); // root
            }
        }
        return startFolder;
    }

    @Override
    public void onFileSelected(File file) {
        final File canonicalFile = FileUtil.safeGetCanonicalFile(file); // important as we compare the path value later
        if (canonicalFile.isDirectory()) {
            setCrumb(new Crumb(canonicalFile), true);
        } else {
            FileFilter fileFilter = pathname -> !pathname.isDirectory() && AUDIO_FILE_FILTER.accept(pathname);
            new ListSongsAsyncTask(getActivity(), null, (songs, extra) -> {
                int startIndex = -1;
                for (int i = 0; i < songs.size(); i++) {
                    if (canonicalFile.getPath().equals(songs.get(i).data)) {
                        startIndex = i;
                        break;
                    }
                }
                if (startIndex > -1) {
                    MusicPlayerRemote.openQueue(songs, startIndex, true);
                } else {
                    Snackbar.make(coordinatorLayout, Html.fromHtml(String.format(getString(R.string.not_listed_in_media_store), canonicalFile.getName())), BaseTransientBottomBar.LENGTH_LONG)
                            .setAction(R.string.action_scan, v -> scanPaths(new String[]{canonicalFile.getPath()}))
                            .setActionTextColor(ThemeStore.accentColor(requireActivity()))
                            .show();
                }
            }).execute(new ListSongsAsyncTask.LoadingInfo(toList(canonicalFile.getParentFile()), fileFilter, getFileComparator()));
        }
    }

    @Override
    public void onMultipleItemAction(MenuItem item, List<File> files) {
        final int itemId = item.getItemId();
        new ListSongsAsyncTask(getActivity(), null, (songs, extra) -> {
            if (!songs.isEmpty()) {
                SongsMenuHelper.handleMenuClick(requireActivity(), songs, itemId);
            }
            if (songs.size() != files.size()) {
                Snackbar.make(coordinatorLayout, R.string.some_files_are_not_listed_in_the_media_store, BaseTransientBottomBar.LENGTH_LONG)
                        .setAction(R.string.action_scan, v -> {
                            String[] paths = new String[files.size()];
                            for (int i = 0; i < files.size(); i++) {
                                paths[i] = FileUtil.safeGetCanonicalPath(files.get(i));
                            }
                            scanPaths(paths);
                        })
                        .setActionTextColor(ThemeStore.accentColor(requireActivity()))
                        .show();
            }
        }).execute(new ListSongsAsyncTask.LoadingInfo(files, AUDIO_FILE_FILTER, getFileComparator()));
    }

    private List<File> toList(File file) {
        List<File> files = new ArrayList<>(1);
        files.add(file);
        return files;
    }

    Comparator<File> fileComparator = (lhs, rhs) -> {
        if (lhs.isDirectory() && !rhs.isDirectory()) {
            return -1;
        } else if (!lhs.isDirectory() && rhs.isDirectory()) {
            return 1;
        } else {
            switch (currentSortOrder) {
                case A_Z_ORDER:
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                case Z_A_ORDER:
                    return -lhs.getName().compareToIgnoreCase(rhs.getName());
                case DATE_ORDER:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            final BasicFileAttributes att_lhs = Files.readAttributes(lhs.toPath(), BasicFileAttributes.class);
                            final BasicFileAttributes att_rhs = Files.readAttributes(rhs.toPath(), BasicFileAttributes.class);

                            return att_lhs.creationTime().toMillis() > att_rhs.creationTime().toMillis() ? -1 : 1;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                case DATE_ORDER_REV:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            final BasicFileAttributes att_lhs = Files.readAttributes(lhs.toPath(), BasicFileAttributes.class);
                            final BasicFileAttributes att_rhs = Files.readAttributes(rhs.toPath(), BasicFileAttributes.class);

                            return att_lhs.creationTime().toMillis() < att_rhs.creationTime().toMillis() ? -1 : 1;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                default:
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        }
    };

    private Comparator<File> getFileComparator() {
        return fileComparator;
    }

    @Override
    public void onFileMenuClicked(final File file, View view) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        if (file.isDirectory()) {
            popupMenu.inflate(R.menu.menu_item_directory);
            popupMenu.setOnMenuItemClickListener(item -> {
                final int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.action_play_next:
                    case R.id.action_add_to_current_playing:
                    case R.id.action_add_to_playlist:
                    case R.id.action_delete_from_device:
                        new ListSongsAsyncTask(getActivity(), null, (songs, extra) -> {
                            if (!songs.isEmpty()) {
                                SongsMenuHelper.handleMenuClick(requireActivity(), songs, itemId);
                            }
                        }).execute(new ListSongsAsyncTask.LoadingInfo(toList(file), AUDIO_FILE_FILTER, getFileComparator()));
                        return true;
                    case R.id.action_set_as_start_directory:
                        PreferenceUtil.getInstance(requireActivity()).setStartDirectory(file);
                        Toast.makeText(getActivity(), String.format(getString(R.string.new_start_directory), file.getPath()), Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.action_scan:
                        new ArrayListPathsAsyncTask(getActivity(), this::scanPaths).execute(new ArrayListPathsAsyncTask.LoadingInfo(file, AUDIO_FILE_FILTER));
                        return true;
                }
                return false;
            });
        } else {
            popupMenu.inflate(R.menu.menu_item_file);
            popupMenu.setOnMenuItemClickListener(item -> {
                final int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.action_play_next:
                    case R.id.action_add_to_current_playing:
                    case R.id.action_add_to_playlist:
                    case R.id.action_go_to_album:
                    case R.id.action_go_to_artist:
                    case R.id.action_share:
                    case R.id.action_tag_editor:
                    case R.id.action_details:
                    case R.id.action_set_as_ringtone:
                    case R.id.action_delete_from_device:
                        new ListSongsAsyncTask(getActivity(), null, (songs, extra) -> {
                            if (!songs.isEmpty()) {
                                SongMenuHelper.handleMenuClick(requireActivity(), songs.get(0), itemId);
                            } else {
                                Snackbar.make(coordinatorLayout, Html.fromHtml(String.format(getString(R.string.not_listed_in_media_store), file.getName())), Snackbar.LENGTH_LONG)
                                        .setAction(R.string.action_scan, v -> scanPaths(new String[]{FileUtil.safeGetCanonicalPath(file)}))
                                        .setActionTextColor(ThemeStore.accentColor(requireActivity()))
                                        .show();
                            }
                        }).execute(new ListSongsAsyncTask.LoadingInfo(toList(file), AUDIO_FILE_FILTER, getFileComparator()));
                        return true;
                    case R.id.action_scan:
                        scanPaths(new String[]{FileUtil.safeGetCanonicalPath(file)});
                        return true;
                }
                return false;
            });
        }
        popupMenu.show();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        container.setPadding(container.getPaddingLeft(), container.getPaddingTop(), container.getPaddingRight(), appbar.getTotalScrollRange() + verticalOffset);
    }

    private void checkIsEmpty() {
        if (empty != null) {
            empty.setVisibility(adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void scanPaths(@Nullable String[] toBeScanned) {
        if (getActivity() == null) return;
        if (toBeScanned == null || toBeScanned.length < 1) {
            Toast.makeText(getActivity(), R.string.nothing_to_scan, Toast.LENGTH_SHORT).show();
        } else {
            MediaScannerConnection.scanFile(getActivity().getApplicationContext(), toBeScanned, null, new UpdateToastMediaScannerCompletionListener(getActivity(), toBeScanned));
        }
    }

    private void updateAdapter(@NonNull List<File> files) {
        adapter.swapDataSet(files);
        Crumb crumb = getActiveCrumb();
        if (crumb != null && recyclerView != null) {
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(crumb.getScrollPosition(), 0);
        }
    }

    @NotNull
    @Override
    public Loader<List<File>> onCreateLoader(int id, Bundle args) {
        return new AsyncFileLoader(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<File>> loader, List<File> data) {
        updateAdapter(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<File>> loader) {
        updateAdapter(new LinkedList<>());
    }

    /**
     * Loader
     */
    private static class AsyncFileLoader extends WrappedAsyncTaskLoader<List<File>> {
        private WeakReference<FoldersFragment> fragmentWeakReference;

        public AsyncFileLoader(FoldersFragment foldersFragment) {
            super(foldersFragment.getActivity());
            fragmentWeakReference = new WeakReference<>(foldersFragment);
        }

        @Override
        public List<File> loadInBackground() {
            final FoldersFragment foldersFragment = fragmentWeakReference.get();
            File directory = null;
            if (foldersFragment != null) {
                final Crumb crumb = foldersFragment.getActiveCrumb();
                if (crumb != null) {
                    directory = crumb.getFile();
                }
            }
            if (directory != null) {
                final List<File> files = FileUtil.listFiles(directory, AUDIO_FILE_FILTER);
                Collections.sort(files, foldersFragment.getFileComparator());
                return files;
            } else {
                return new LinkedList<>();
            }
        }
    }

    private static class ListSongsAsyncTask extends ListingFilesDialogAsyncTask<ListSongsAsyncTask.LoadingInfo, Void, List<Song>> {
        private WeakReference<Context> contextWeakReference;
        private WeakReference<OnSongsListedCallback> callbackWeakReference;
        private final Object extra;

        public ListSongsAsyncTask(Context context, Object extra, OnSongsListedCallback callback) {
            super(context, 500);
            this.extra = extra;
            contextWeakReference = new WeakReference<>(context);
            callbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            checkCallbackReference();
            checkContextReference();
        }

        @Override
        protected List<Song> doInBackground(LoadingInfo... params) {
            try {
                LoadingInfo info = params[0];
                List<File> files = FileUtil.listFilesDeep(info.files, info.fileFilter);

                if (isCancelled() || checkContextReference() == null || checkCallbackReference() == null)
                    return null;

                Collections.sort(files, info.fileComparator);

                Context context = checkContextReference();
                if (isCancelled() || context == null || checkCallbackReference() == null)
                    return null;

                return FileUtil.matchFilesWithMediaStore(context, files);
            } catch (Exception e) {
                e.printStackTrace();
                cancel(false);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Song> songs) {
            super.onPostExecute(songs);
            OnSongsListedCallback callback = checkCallbackReference();
            if (songs != null && callback != null)
                callback.onSongsListed(songs, extra);
        }

        private Context checkContextReference() {
            Context context = contextWeakReference.get();
            if (context == null) {
                cancel(false);
            }
            return context;
        }

        private OnSongsListedCallback checkCallbackReference() {
            OnSongsListedCallback callback = callbackWeakReference.get();
            if (callback == null) {
                cancel(false);
            }
            return callback;
        }

        public static class LoadingInfo {
            public final Comparator<File> fileComparator;
            public final FileFilter fileFilter;
            public final List<File> files;

            public LoadingInfo(@NonNull List<File> files, @NonNull FileFilter fileFilter, @NonNull Comparator<File> fileComparator) {
                this.fileComparator = fileComparator;
                this.fileFilter = fileFilter;
                this.files = files;
            }
        }

        public interface OnSongsListedCallback {
            void onSongsListed(@NonNull List<Song> songs, Object extra);
        }
    }

    public static class ArrayListPathsAsyncTask extends ListingFilesDialogAsyncTask<ArrayListPathsAsyncTask.LoadingInfo, String, String[]> {
        private WeakReference<OnPathsListedCallback> onPathsListedCallbackWeakReference;

        public ArrayListPathsAsyncTask(Context context, OnPathsListedCallback callback) {
            super(context, 500);
            onPathsListedCallbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            checkCallbackReference();
        }

        @Override
        protected String[] doInBackground(LoadingInfo... params) {
            try {
                if (isCancelled() || checkCallbackReference() == null) return null;

                LoadingInfo info = params[0];

                final String[] paths;

                if (info.file.isDirectory()) {
                    List<File> files = FileUtil.listFilesDeep(info.file, info.fileFilter);

                    if (isCancelled() || checkCallbackReference() == null) return null;

                    paths = new String[files.size()];
                    for (int i = 0; i < files.size(); i++) {
                        File f = files.get(i);
                        paths[i] = FileUtil.safeGetCanonicalPath(f);

                        if (isCancelled() || checkCallbackReference() == null) return null;
                    }
                } else {
                    paths = new String[1];
                    paths[0] = FileUtil.safeGetCanonicalPath(info.file);
                }

                return paths;
            } catch (Exception e) {
                e.printStackTrace();
                cancel(false);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] paths) {
            super.onPostExecute(paths);
            OnPathsListedCallback callback = checkCallbackReference();
            if (callback != null && paths != null) {
                callback.onPathsListed(paths);
            }
        }

        private OnPathsListedCallback checkCallbackReference() {
            OnPathsListedCallback callback = onPathsListedCallbackWeakReference.get();
            if (callback == null) {
                cancel(false);
            }
            return callback;
        }

        public static class LoadingInfo {
            public final File file;
            public final FileFilter fileFilter;

            public LoadingInfo(File file, FileFilter fileFilter) {
                this.file = file;
                this.fileFilter = fileFilter;
            }
        }

        public interface OnPathsListedCallback {
            void onPathsListed(@NonNull String[] paths);
        }
    }

    private static abstract class ListingFilesDialogAsyncTask<Params, Progress, Result> extends DialogAsyncTask<Params, Progress, Result> {
        public ListingFilesDialogAsyncTask(Context context) {
            super(context);
        }

        public ListingFilesDialogAsyncTask(Context context, int showDelay) {
            super(context, showDelay);
        }

        @Override
        protected Dialog createDialog(@NonNull Context context) {
            return new MaterialDialog.Builder(context)
                    .title(R.string.listing_files)
                    .progress(true, 0)
                    .progressIndeterminateStyle(true)
                    .cancelListener(dialog -> cancel(false))
                    .dismissListener(dialog -> cancel(false))
                    .negativeText(android.R.string.cancel)
                    .onNegative((dialog, which) -> cancel(false))
                    .show();
        }
    }
}
