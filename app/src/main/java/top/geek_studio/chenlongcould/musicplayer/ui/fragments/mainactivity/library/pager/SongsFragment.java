package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;

import com.kabouzeid.chenlongcould.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.adapter.song.ShuffleButtonSongAdapter;
import top.geek_studio.chenlongcould.musicplayer.adapter.song.SongAdapter;
import top.geek_studio.chenlongcould.musicplayer.interfaces.LoaderIds;
import top.geek_studio.chenlongcould.musicplayer.loader.SongLoader;
import top.geek_studio.chenlongcould.musicplayer.misc.WrappedAsyncTaskLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * 歌曲 Fragment
 * <p>
 * same as {@link PlaylistsFragment}
 *
 * @author Karim Abou Zeid (kabouzeid)
 * @see top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.LibraryFragment
 */
public class SongsFragment extends AbsLibraryPagerRecyclerViewCustomGridSizeFragment<SongAdapter, GridLayoutManager> implements LoaderManager.LoaderCallbacks<List<Song>> {

    private static final int LOADER_ID = LoaderIds.SONGS_FRAGMENT;

    private static final String TAG = SongsFragment.class.getSimpleName();

    private int songCount = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    @Override
    public String getSubTitle() {
        return songCount + " Song(s)";
    }

    @NonNull
    @Override
    protected GridLayoutManager createLayoutManager() {
        return new GridLayoutManager(getActivity(), getGridSize());
    }

    @NonNull
    @Override
    protected SongAdapter createAdapter() {
        int itemLayoutRes = getItemLayoutRes();
        notifyLayoutResChanged(itemLayoutRes);
        boolean usePalette = loadUsePalette();
        List<Song> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();

        if (getGridSize() <= getMaxGridSizeForList()) {
            return new ShuffleButtonSongAdapter(
                    getLibraryFragment().getMainActivity(),
                    dataSet,
                    itemLayoutRes,
                    usePalette,
                    getLibraryFragment());
        }

        return new SongAdapter(
                getLibraryFragment().getMainActivity(),
                dataSet,
                itemLayoutRes,
                usePalette,
                getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_songs;
    }

    @Override
    public void onMediaStoreChanged() {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected String loadSortOrder() {
        return PreferenceUtil.getInstance(getActivity()).getSongSortOrder();
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        PreferenceUtil.getInstance(getActivity()).setSongSortOrder(sortOrder);
    }

    @Override
    protected void setSortOrder(String sortOrder) {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected int loadGridSize() {
        return PreferenceUtil.getInstance(getActivity()).getSongGridSize(getActivity());
    }

    @Override
    protected void saveGridSize(int gridSize) {
        PreferenceUtil.getInstance(getActivity()).setSongGridSize(gridSize);
    }

    @Override
    protected int loadGridSizeLand() {
        return PreferenceUtil.getInstance(getActivity()).getSongGridSizeLand(getActivity());
    }

    @Override
    protected void saveGridSizeLand(int gridSize) {
        PreferenceUtil.getInstance(getActivity()).setSongGridSizeLand(gridSize);
    }

    @Override
    public void saveUsePalette(boolean usePalette) {
        PreferenceUtil.getInstance(getActivity()).setSongColoredFooters(usePalette);
    }

    @Override
    public boolean loadUsePalette() {
        return PreferenceUtil.getInstance(getActivity()).songColoredFooters();
    }

    @Override
    public void setUsePalette(boolean usePalette) {
        getAdapter().usePalette(usePalette);
    }

    @Override
    protected void setGridSize(int gridSize) {
        getLayoutManager().setSpanCount(gridSize);
        getAdapter().notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        return new AsyncSongLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        getAdapter().swapDataSet(data);
        songCount = data.size();
        Log.d(TAG, "onLoadFinished: ");
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    /**
     * 异步加载歌曲
     */
    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
        AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return SongLoader.getAllSongs(getContext());
        }
    }
}
