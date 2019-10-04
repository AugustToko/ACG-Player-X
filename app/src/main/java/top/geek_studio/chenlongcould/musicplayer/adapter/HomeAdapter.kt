package top.geek_studio.chenlongcould.musicplayer.adapter

import android.content.res.ColorStateList
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.kabouzeid.appthemehelper.ThemeStore
import com.kabouzeid.chenlongcould.musicplayer.R
import top.geek_studio.chenlongcould.musicplayer.adapter.album.AlbumFullWidthAdapter
import top.geek_studio.chenlongcould.musicplayer.adapter.artist.ArtistAdapter
import top.geek_studio.chenlongcould.musicplayer.adapter.song.SongAdapter
import top.geek_studio.chenlongcould.musicplayer.loader.PlaylistSongLoader
import top.geek_studio.chenlongcould.musicplayer.model.*
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil

class HomeAdapter(
        private val activity: AppCompatActivity,
        private var homes: List<Home>,
        private val displayMetrics: DisplayMetrics
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return homes[position].homeSection
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = LayoutInflater.from(activity).inflate(R.layout.section_recycler_view, parent, false)
        return when (viewType) {
            RECENT_ARTISTS, TOP_ARTISTS -> ArtistViewHolder(layout)
            PLAYLISTS -> PlaylistViewHolder(layout)
            else -> {
                AlbumViewHolder(LayoutInflater.from(activity).inflate(R.layout.metal_section_recycler_view, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val home = homes[position]
        when (getItemViewType(position)) {

            RECENT_ALBUMS, TOP_ALBUMS -> {
                val viewHolder = holder as AlbumViewHolder
                viewHolder.bindView(home)
            }
            RECENT_ARTISTS, TOP_ARTISTS -> {
                val viewHolder = holder as ArtistViewHolder
                viewHolder.bindView(home)
            }
            PLAYLISTS -> {
                val viewHolder = holder as PlaylistViewHolder
                viewHolder.bindView(home)
            }
        }
    }

    override fun getItemCount(): Int {
        return homes.size
    }

    fun swapData(finalList: List<Home>) {
        homes = finalList
        notifyDataSetChanged()
    }

    companion object {

        @IntDef(RECENT_ALBUMS, TOP_ALBUMS, RECENT_ARTISTS, TOP_ARTISTS, GENRES, PLAYLISTS)
        @Retention(AnnotationRetention.SOURCE)
        annotation class HomeSection

        const val RECENT_ALBUMS = 0
        const val TOP_ALBUMS = 1
        const val RECENT_ARTISTS = 2
        const val TOP_ARTISTS = 3
        const val GENRES = 4
        const val PLAYLISTS = 5

    }

    private inner class AlbumViewHolder(view: View) : AbsHomeViewItem(view) {
        fun bindView(home: Home) {
            if (home.arrayList.isEmpty()) return

            recyclerView.apply {
                adapter = AlbumFullWidthAdapter(activity, home.arrayList as ArrayList<Album>, displayMetrics)
            }
            chip.text = activity.getString(home.title)
            chip.setChipIconResource(home.icon)
        }
    }

    private inner class ArtistViewHolder(view: View) : AbsHomeViewItem(view) {
        fun bindView(home: Home) {
            if (home.arrayList.isEmpty()) return

            recyclerView.apply {
                layoutManager = GridLayoutManager(activity, 1, GridLayoutManager.HORIZONTAL, false)
                val artistAdapter = ArtistAdapter(activity, home.arrayList as ArrayList<Artist>, PreferenceUtil.getInstance(activity).getHomeGridStyle(context!!), false, null)
                adapter = artistAdapter
            }
            chip.text = activity.getString(home.title)
            chip.setChipIconResource(home.icon)
        }
    }

    private inner class PlaylistViewHolder(view: View) : AbsHomeViewItem(view) {
        fun bindView(home: Home) {
            if (home.arrayList.isEmpty()) return

            val songs = PlaylistSongLoader.getPlaylistSongList(activity, (home.arrayList[0] as Playlist).id)
            recyclerView.apply {
                val songAdapter = SongAdapter(activity, songs as List<Song>?, R.layout.item_album_card, false, null)
                layoutManager = GridLayoutManager(activity, 1, GridLayoutManager.HORIZONTAL, false)
                adapter = songAdapter

            }
            chip.text = activity.getString(home.title)
            chip.setChipIconResource(home.icon)
        }
    }

    private open inner class AbsHomeViewItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerView)
        val chip: Chip = itemView.findViewById(R.id.chipHead)

        init {
            chip.apply { chipBackgroundColor = ColorStateList.valueOf(ThemeStore.primaryColor(context)) }
        }
    }
}