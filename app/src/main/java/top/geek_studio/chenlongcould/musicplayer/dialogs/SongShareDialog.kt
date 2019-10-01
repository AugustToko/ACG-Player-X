package top.geek_studio.chenlongcould.musicplayer.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment

import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.chenlongcould.musicplayer.R

import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil

/**
 * 歌曲分享 dialog
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
class SongShareDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song = arguments!!.getParcelable<Song>("song")
        val currentlyListening = getString(R.string.currently_listening_to_x_by_x, song!!.title, song.artistName)
        return MaterialDialog.Builder(activity!!)
                .title(R.string.what_do_you_want_to_share)
                .items(getString(R.string.the_audio_file), "\u201C" + currentlyListening + "\u201D")
                .itemsCallback { materialDialog, view, i, charSequence ->
                    when (i) {
                        0 -> startActivity(Intent.createChooser(MusicUtil.createShareSongFileIntent(song, context),
                                null))
                        1 -> activity!!.startActivity(
                                Intent.createChooser(
                                        Intent()
                                                .setAction(Intent.ACTION_SEND)
                                                .putExtra(Intent.EXTRA_TEXT, currentlyListening)
                                                .setType("text/plain"), null
                                )
                        )
                    }
                }
                .build()
    }

    companion object {
        fun create(song: Song): SongShareDialog {
            val dialog = SongShareDialog()
            val args = Bundle()
            args.putParcelable("song", song)
            dialog.arguments = args
            return dialog
        }
    }
}
