package top.geek_studio.chenlongcould.musicplayer.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import top.geek_studio.chenlongcould.musicplayer.Common.R;

/**
 * 铃声管理
 */
public class RingtoneManager {

    /**
     * 检测是否能写入设置
     *
     * @param context ctx
     */
    public static boolean requiresDialog(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return !Settings.System.canWrite(context);
        }
        return false;
    }

    /**
     * 获取权限
     *
     * @param context ctx
     */
    public static MaterialDialog showDialog(Context context) {
        return new MaterialDialog.Builder(context)
                .title(R.string.dialog_ringtone_title)
                .content(R.string.dialog_ringtone_message)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    context.startActivity(intent);
                })
                .show();
    }

    /**
     * 设置铃声，通过 {@link android.content.ContentProvider} 设置
     */
    public void setRingtone(@NonNull final Context context, final int id) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = MusicUtil.getSongFileUri(id);
        try {
            final ContentValues values = new ContentValues(2);
            values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, "1");
            values.put(MediaStore.Audio.AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (@NonNull final UnsupportedOperationException ignored) {
            return;
        }

        try {
            try (
                    final Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.MediaColumns.TITLE},
                            BaseColumns._ID + "=?",
                            new String[]{String.valueOf(id)},
                            null)
            ) {
                if (cursor != null && cursor.getCount() == 1) {
                    cursor.moveToFirst();
                    Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
                    final String message = context.getString(R.string.x_has_been_set_as_ringtone, cursor.getString(0));
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException ignored) {
        }
    }
}
