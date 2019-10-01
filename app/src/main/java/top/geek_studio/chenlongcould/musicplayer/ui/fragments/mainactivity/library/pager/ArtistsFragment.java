package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;

import com.kabouzeid.chenlongcould.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.adapter.artist.ArtistAdapter;
import top.geek_studio.chenlongcould.musicplayer.interfaces.LoaderIds;
import top.geek_studio.chenlongcould.musicplayer.loader.ArtistLoader;
import top.geek_studio.chenlongcould.musicplayer.misc.WrappedAsyncTaskLoader;
import top.geek_studio.chenlongcould.musicplayer.model.Artist;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ArtistsFragment extends AbsLibraryPagerRecyclerViewCustomGridSizeFragment<ArtistAdapter, GridLayoutManager> implements LoaderManager.LoaderCallbacks<List<Artist>> {

    private static final int LOADER_ID = LoaderIds.ARTISTS_FRAGMENT;

    private int artistCount = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    @Override
    public String getSubTitle() {
        return artistCount + " Artist(s)";
    }

    @NonNull
    @Override
    protected GridLayoutManager createLayoutManager() {
        return new GridLayoutManager(getActivity(), getGridSize());
    }

    @NonNull
    @Override
    protected ArtistAdapter createAdapter() {
        int itemLayoutRes = getItemLayoutRes();
        notifyLayoutResChanged(itemLayoutRes);
        List<Artist> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();
        return new ArtistAdapter(
                getLibraryFragment().getMainActivity(),
                dataSet,
                itemLayoutRes,
                loadUsePalette(),
                getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_artists;
    }

    @Override
    public void onMediaStoreChanged() {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected String loadSortOrder() {
        return PreferenceUtil.getInstance(getActivity()).getArtistSortOrder();
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        PreferenceUtil.getInstance(getActivity()).setArtistSortOrder(sortOrder);
    }

    @Override
    protected void setSortOrder(String sortOrder) {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected int loadGridSize() {
        return PreferenceUtil.getInstance(getActivity()).getArtistGridSize(getActivity());
    }

    @Override
    protected void saveGridSize(int gridSize) {
        PreferenceUtil.getInstance(getActivity()).setArtistGridSize(gridSize);
    }

    @Override
    protected int loadGridSizeLand() {
        return PreferenceUtil.getInstance(getActivity()).getArtistGridSizeLand(getActivity());
    }

    @Override
    protected void saveGridSizeLand(int gridSize) {
        PreferenceUtil.getInstance(getActivity()).setArtistGridSizeLand(gridSize);
    }

    @Override
    protected void saveUsePalette(boolean usePalette) {
        PreferenceUtil.getInstance(getActivity()).setArtistColoredFooters(usePalette);
    }

    @Override
    public boolean loadUsePalette() {
        return PreferenceUtil.getInstance(getActivity()).artistColoredFooters();
    }

    @Override
    protected void setUsePalette(boolean usePalette) {
        getAdapter().usePalette(usePalette);
    }

    @Override
    protected void setGridSize(int gridSize) {
        getLayoutManager().setSpanCount(gridSize);
        getAdapter().notifyDataSetChanged();
    }


    @Override
    public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
        return new AsyncArtistLoader(getActivity());
    }


    @Override
    public void onLoadFinished(Loader<List<Artist>> loader, List<Artist> data) {
        getAdapter().swapDataSet(data);
        artistCount = data.size();
    }


    @Override
    public void onLoaderReset(Loader<List<Artist>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncArtistLoader extends WrappedAsyncTaskLoader<List<Artist>> {
        public AsyncArtistLoader(Context context) {
            super(context);
        }

        @Override
        public List<Artist> loadInBackground() {
            return ArtistLoader.getAllArtists(getContext());
        }
    }
}
