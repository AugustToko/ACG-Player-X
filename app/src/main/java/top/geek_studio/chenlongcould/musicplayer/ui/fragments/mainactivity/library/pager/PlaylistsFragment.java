package top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;

import top.geek_studio.chenlongcould.musicplayer.Common.R;

import java.util.ArrayList;
import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.adapter.PlaylistAdapter;
import top.geek_studio.chenlongcould.musicplayer.interfaces.LoaderIds;
import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistLoader;
import top.geek_studio.chenlongcould.musicplayer.misc.WrappedAsyncTaskLoader;
import top.geek_studio.chenlongcould.musicplayer.model.DataViewModel;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.HistoryPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.LastAddedPlaylist;
import top.geek_studio.chenlongcould.musicplayer.model.smartplaylist.MyTopTracksPlaylist;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.MainActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.library.pager.base.AbsLibraryPagerRecyclerViewFragment;

/**
 * 播放列表 Fragment
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistsFragment extends AbsLibraryPagerRecyclerViewFragment<PlaylistAdapter, LinearLayoutManager> implements LoaderManager.LoaderCallbacks<List<Playlist>> {

    private static final int LOADER_ID = LoaderIds.PLAYLISTS_FRAGMENT;

    private int playlistsCount = 0;

    private DataViewModel mViewModel;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
        final MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            mViewModel = activity.mViewModel;
        }
    }

    @Override
    public String getSubTitle() {
        // TODO: use context.getString(int id);
        if (isAdded()) return playlistsCount + " Playlist(s)";
        else  return playlistsCount + " Playlist(s)";
    }

    @NonNull
    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @NonNull
    @Override
    protected PlaylistAdapter createAdapter() {
        // 获取数据
        final List<Playlist> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();
        return new PlaylistAdapter(getLibraryFragment().getMainActivity(), dataSet,
                R.layout.item_list_single_row, getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_playlists;
    }

    @Override
    public void onMediaStoreChanged() {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    public Loader<List<Playlist>> onCreateLoader(int id, Bundle args) {
        return new AsyncPlaylistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Playlist>> loader, List<Playlist> data) {
        // 加载数据
        getAdapter().swapDataSet(data);
        playlistsCount = data.size();
        mViewModel.putPlaylists(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Playlist>> loader) {
        List<Playlist> data = new ArrayList<>();
        // 置空
        getAdapter().swapDataSet(data);
        mViewModel.putPlaylists(data);
    }

    /**
     * 异步加载播放列表
     */
    private static class AsyncPlaylistLoader extends WrappedAsyncTaskLoader<List<Playlist>> {

        public AsyncPlaylistLoader(Context context) {
            super(context);
        }

        /**
         * 获取所有播放列表
         */
        @Override
        public List<Playlist> loadInBackground() {
            final Context context = getContext();

            final List<Playlist> playlists = new ArrayList<>();

            playlists.add(new LastAddedPlaylist(context));
            playlists.add(new HistoryPlaylist(context));
            playlists.add(new MyTopTracksPlaylist(context));

            playlists.addAll(PlaylistLoader.getAllPlaylists(context));

            return playlists;
        }
    }
}
