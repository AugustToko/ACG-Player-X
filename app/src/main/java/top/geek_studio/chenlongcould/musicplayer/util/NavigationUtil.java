package top.geek_studio.chenlongcould.musicplayer.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;

import io.flutter.embedding.android.FlutterActivity;
import top.geek_studio.chenlongcould.musicplayer.Common.R;

import top.geek_studio.chenlongcould.musicplayer.helper.MusicPlayerRemote;
import top.geek_studio.chenlongcould.musicplayer.model.Genre;
import top.geek_studio.chenlongcould.musicplayer.model.Playlist;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.AlbumDetailActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.ArtistDetailActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.GenreDetailActivity;
import top.geek_studio.chenlongcould.musicplayer.ui.activities.PlaylistDetailActivity;

/**
 * 导航工具
 * <p>
 * 用于启动各种界面 (Activity)
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class NavigationUtil {

    private static final String TAG = "NavigationUtil";

    /**
     * 显示艺术家详情页面
     *
     * @param activity       activity
     * @param artistId       艺术家 ID
     * @param sharedElements 共享元素
     */
    public static void goToArtist(@NonNull final Activity activity, final int artistId, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, ArtistDetailActivity.class);
        intent.putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artistId);

        if (sharedElements != null && sharedElements.length > 0) {
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElements).toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    /**
     * 千万专辑详情页面
     *
     * @param activity       activity
     * @param albumId        专辑 ID
     * @param sharedElements 共享元素
     */
    public static void goToAlbum(@NonNull final Activity activity, final int albumId, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, AlbumDetailActivity.class);
        intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, albumId);

        if (sharedElements != null && sharedElements.length > 0) {
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElements).toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    public static void goToGenre(@NonNull final Activity activity, final Genre genre, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, GenreDetailActivity.class);
        intent.putExtra(GenreDetailActivity.EXTRA_GENRE, genre);
        activity.startActivity(intent);
    }

    /**
     * 前往播放列表页面
     *
     * @param activity       activity
     * @param playlist       播放列表
     * @param sharedElements 共享元素
     */
    public static void goToPlaylist(@NonNull final Activity activity, final Playlist playlist, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, PlaylistDetailActivity.class);
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST, playlist);
        activity.startActivity(intent);
    }

    /**
     * 打开均衡器
     *
     * @param activity activity
     */
    public static void openEqualizer(@NonNull final Activity activity) {
        final int sessionId = MusicPlayerRemote.getAudioSessionId();
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(activity, activity.getResources().getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show();
        } else {
            try {
                final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                activity.startActivityForResult(effects, 0);
            } catch (@NonNull final ActivityNotFoundException notFound) {
                Toast.makeText(activity, activity.getResources().getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void openLive2dPage(@NonNull final Activity activity) {
        final Intent intent = FlutterActivity.withNewEngine().initialRoute("/live2d_settings").build(activity);
        activity.startActivity(intent);
    }
}
