package top.geek_studio.chenlongcould.musicplayer.adapter.song;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialcab.MaterialCab;
import com.bumptech.glide.Glide;
import com.kabouzeid.chenlongcould.musicplayer.R;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

import top.geek_studio.chenlongcould.musicplayer.adapter.base.AbsMultiSelectAdapter;
import top.geek_studio.chenlongcould.musicplayer.adapter.base.MediaEntryViewHolder;
import top.geek_studio.chenlongcould.musicplayer.glide.NetSongGlideRequest;
import top.geek_studio.chenlongcould.musicplayer.glide.PhonographColoredTarget;
import top.geek_studio.chenlongcould.musicplayer.helper.SortOrder;
import top.geek_studio.chenlongcould.musicplayer.helper.menu.SongMenuHelper;
import top.geek_studio.chenlongcould.musicplayer.interfaces.CabHolder;
import top.geek_studio.chenlongcould.musicplayer.model.NetSearchSong.ResultBean.SongsBean;
import top.geek_studio.chenlongcould.musicplayer.model.Song;
import top.geek_studio.chenlongcould.musicplayer.ui.fragments.mainactivity.netsearch.NetSearchFragment;
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil;
import top.geek_studio.chenlongcould.musicplayer.util.NetPlayerUtil;
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil;

/**
 * 歌曲适配器
 *
 * @author chenlongcould (Modify)
 * @author Karim Abou Zeid (kabouzeid)
 */
public class NetSearchSongAdapter extends AbsMultiSelectAdapter<NetSearchSongAdapter.ViewHolder, SongsBean> implements MaterialCab.Callback, FastScrollRecyclerView.SectionedAdapter {

    private static final String TAG = "SongAdapter";

    protected final AppCompatActivity activity;

    private NetSearchFragment fragment;

    protected List<SongsBean> dataSet;

    protected int itemLayoutRes;

    protected boolean usePalette = false;

    protected boolean showSectionName = true;


    public NetSearchSongAdapter(AppCompatActivity activity, NetSearchFragment netSearchFragment, List<SongsBean> dataSet, @LayoutRes int itemLayoutRes, boolean usePalette, @Nullable CabHolder cabHolder) {
        this(activity, netSearchFragment, dataSet, itemLayoutRes, usePalette, cabHolder, true);
    }

    public NetSearchSongAdapter(AppCompatActivity activity, NetSearchFragment netSearchFragment, List<SongsBean> dataSet, @LayoutRes int itemLayoutRes, boolean usePalette, @Nullable CabHolder cabHolder, boolean showSectionName) {
        super(activity, cabHolder, R.menu.menu_media_selection);
        this.activity = activity;
        this.dataSet = dataSet;
        this.itemLayoutRes = itemLayoutRes;
        this.usePalette = usePalette;
        this.showSectionName = showSectionName;
        setHasStableIds(true);

        fragment = netSearchFragment;
    }


    public void swapDataSet(List<SongsBean> dataSet) {
        this.dataSet = dataSet;
        notifyDataSetChanged();
    }

    public void usePalette(boolean usePalette) {
        this.usePalette = usePalette;
        notifyDataSetChanged();
    }

    public List<SongsBean> getDataSet() {
        return dataSet;
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).getId();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final SongsBean song = dataSet.get(position);

        boolean isChecked = isChecked(song);
        holder.itemView.setActivated(isChecked);

        if (holder.getAdapterPosition() == getItemCount() - 1) {
            if (holder.shortSeparator != null) {
                holder.shortSeparator.setVisibility(View.GONE);
            }
        } else {
            if (holder.shortSeparator != null) {
                holder.shortSeparator.setVisibility(View.VISIBLE);
            }
        }

        if (holder.title != null) {
            holder.title.setText(getSongTitle(song));
        }
        if (holder.text != null) {
            holder.text.setText(getSongText(song));
        }

        if (holder.durationText != null)
            holder.durationText.setText(MusicUtil.getReadableDurationString(song.getDuration()));
//        if (holder.formatText != null)
//            holder.formatText.setText(song.data.substring(song.data.lastIndexOf(".") + 1));

        loadAlbumCover(song, holder);
    }

//    private void setColors(int color, ViewHolder holder) {
//        if (holder.paletteColorContainer != null) {
//            holder.paletteColorContainer.setBackgroundColor(color);
//            if (holder.title != null) {
//                holder.title.setTextColor(MaterialValueHelper.getPrimaryTextColor(activity, ColorUtil.isColorLight(color)));
//            }
//            if (holder.text != null) {
//                holder.text.setTextColor(MaterialValueHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(color)));
//            }
//        }
//    }

    private void loadAlbumCover(SongsBean song, final ViewHolder holder) {
        if (holder.image == null) return;

        NetPlayerUtil.getNetSong(activity, song, data -> activity.runOnUiThread(() ->
                NetSongGlideRequest.Builder.from(Glide.with(activity), data.getSongs().get(0))
                        .generatePalette(activity).build()
                        .into(new PhonographColoredTarget(holder.image) {
                            @Override
                            public void onColorReady(int color) {
                            }
                        })));

    }

    protected String getSongTitle(SongsBean song) {
        return song.getName();
    }

    protected String getSongText(SongsBean song) {
        return MusicUtil.getSongInfoString(song);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    protected SongsBean getIdentifier(int position) {
        return dataSet.get(position);
    }

    @Override
    protected String getName(SongsBean song) {
        return song.getName();
    }

    @Override
    protected void onMultipleItemAction(@NonNull MenuItem menuItem, @NonNull List<SongsBean> selection) {
//        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.getItemId());
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (!showSectionName) {
            return "";
        }

        @Nullable String sectionName = null;
        switch (PreferenceUtil.getInstance(activity).getSongSortOrder()) {
            case SortOrder.SongSortOrder.SONG_A_Z:
            case SortOrder.SongSortOrder.SONG_Z_A:
                sectionName = dataSet.get(position).getName();
                break;
            case SortOrder.SongSortOrder.SONG_ALBUM:
                sectionName = dataSet.get(position).getAlbum().getName();
                break;
            case SortOrder.SongSortOrder.SONG_ARTIST:
                sectionName = dataSet.get(position).getArtists().get(0).getName();
                break;
            case SortOrder.SongSortOrder.SONG_YEAR:
                return MusicUtil.getYearString((int) dataSet.get(position).getAlbum().getPublishTime());
        }

        return MusicUtil.getSectionName(sectionName);
    }

    public class ViewHolder extends MediaEntryViewHolder {
        protected int DEFAULT_MENU_RES = SongMenuHelper.NET_MENU_RES;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            setImageTransitionName(activity.getString(R.string.transition_album_art));

            if (menu == null) {
                return;
            }
            menu.setOnClickListener(new SongMenuHelper.OnClickSongMenu(activity) {
                @Override
                public Song getSong() {
                    return new Song(-1, ViewHolder.this.getSong().getName(), -1,
                            -1, ViewHolder.this.getSong().getDuration(), "", -1,
                            -1, ViewHolder.this.getSong().getAlbum().getName(), -1,
                            ViewHolder.this.getSong().getArtists().get(0).getName()
                    );
                }

                @Override
                public int getMenuRes() {
                    return getSongMenuRes();
                }

                /**
                 * 监听菜单点击
                 *
                 * @param item item
                 * @return 是否处理了
                 * */
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onSongMenuItemClick(item) || super.onMenuItemClick(item);
                }
            });
        }

        protected SongsBean getSong() {
            return dataSet.get(getAdapterPosition());
        }

        int getSongMenuRes() {
            return DEFAULT_MENU_RES;
        }

        boolean onSongMenuItemClick(MenuItem item) {
            if (image != null && image.getVisibility() == View.VISIBLE) {
                switch (item.getItemId()) {
                    case R.id.action_go_to_album:
//                        Pair[] albumPairs = new Pair[]{
//                                Pair.create(image, activity.getResources().getString(R.string.transition_album_art))
//                        };
//                        NavigationUtil.goToAlbum(activity, getSong().albumId, albumPairs);
                        return true;
                }
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            if (isInQuickSelectMode()) {
                toggleChecked(getAdapterPosition());
            } else {

//                if (currentSong != null) {
//                    if (mediaPlayer.isPlaying()) {
//                        if (getSong().getId() == currentSong.getId()) {
//                            mediaPlayer.pause();
//                        } else {
//                            tryPlay(getSong());
//                        }
//                    } else {
//                        if (getSong().getId() == currentSong.getId()) {
//                            mediaPlayer.start();
//                        } else {
//                            tryPlay(getSong());
//                        }
//                    }
//                } else {
                fragment.tryPlay(getSong());
//                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            return toggleChecked(getAdapterPosition());
        }
    }


}
