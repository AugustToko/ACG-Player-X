package top.geek_studio.chenlongcould.musicplayer.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.chenlongcould.musicplayer.R
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import top.geek_studio.chenlongcould.musicplayer.model.Song
import top.geek_studio.chenlongcould.musicplayer.util.MusicUtil
import java.io.File
import java.io.IOException

/**
 * 歌曲详情 Dialog
 *
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
class SongDetailDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity
        val song = arguments!!.getParcelable<Song>("song")

        val dialog = MaterialDialog.Builder(context!!)
                .customView(R.layout.dialog_file_details, true)
                .title(context.resources.getString(R.string.label_details))
                .positiveText(android.R.string.ok)
                .build()

        val dialogView = dialog.customView
        val fileName = dialogView!!.findViewById<TextView>(R.id.file_name)
        val filePath = dialogView.findViewById<TextView>(R.id.file_path)
        val fileSize = dialogView.findViewById<TextView>(R.id.file_size)
        val fileFormat = dialogView.findViewById<TextView>(R.id.file_format)
        val trackLength = dialogView.findViewById<TextView>(R.id.track_length)
        val bitRate = dialogView.findViewById<TextView>(R.id.bitrate)
        val samplingRate = dialogView.findViewById<TextView>(R.id.sampling_rate)

        fileName.text = makeTextWithTitle(context, R.string.label_file_name, "-")
        filePath.text = makeTextWithTitle(context, R.string.label_file_path, "-")
        fileSize.text = makeTextWithTitle(context, R.string.label_file_size, "-")
        fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, "-")
        trackLength.text = makeTextWithTitle(context, R.string.label_track_length, "-")
        bitRate.text = makeTextWithTitle(context, R.string.label_bit_rate, "-")
        samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, "-")

        if (song != null) {
            val songFile = File(song.data)
            if (songFile.exists()) {
                fileName.text = makeTextWithTitle(context, R.string.label_file_name, songFile.name)
                filePath.text = makeTextWithTitle(context, R.string.label_file_path, songFile.absolutePath)
                fileSize.text = makeTextWithTitle(context, R.string.label_file_size, getFileSizeString(songFile.length()))
                try {
                    val audioFile = AudioFileIO.read(songFile)
                    val audioHeader = audioFile.audioHeader

                    fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, audioHeader.format)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString((audioHeader.trackLength * 1000).toLong()))
                    bitRate.text = makeTextWithTitle(context, R.string.label_bit_rate, audioHeader.bitRate + " kb/s")
                    samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, audioHeader.sampleRate + " Hz")
                } catch (e: CannotReadException) {
                    Log.e(TAG, "error while reading the song file", e)
                    // fallback
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: IOException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: TagException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: ReadOnlyFileException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                } catch (e: InvalidAudioFrameException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
                }

            } else {
                // fallback
                fileName.text = makeTextWithTitle(context, R.string.label_file_name, song.title)
                trackLength.text = makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration))
            }
        }

        return dialog
    }

    companion object {

        val TAG = SongDetailDialog::class.java.simpleName

        fun create(song: Song): SongDetailDialog {
            val dialog = SongDetailDialog()
            val args = Bundle()
            args.putParcelable("song", song)
            dialog.arguments = args
            return dialog
        }

        private fun makeTextWithTitle(context: Context, titleResId: Int, text: String): Spanned {
            return Html.fromHtml("<b>" + context.resources.getString(titleResId) + ": " + "</b>" + text)
        }

        private fun getFileSizeString(sizeInBytes: Long): String {
            val fileSizeInKB = sizeInBytes / 1024
            val fileSizeInMB = fileSizeInKB / 1024
            return "$fileSizeInMB MB"
        }
    }
}
