package top.geek_studio.chenlongcould.musicplayer.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.kabouzeid.chenlongcould.musicplayer.R;

import top.geek_studio.chenlongcould.musicplayer.glide.palette.BitmapPaletteTranscoder;
import top.geek_studio.chenlongcould.musicplayer.glide.palette.BitmapPaletteWrapper;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class UrlGlideRequest {

    private static final DiskCacheStrategy DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.NONE;
    private static int DEFAULT_ERROR_IMAGE = R.drawable.default_album_art;
    private static final int DEFAULT_ANIMATION = android.R.anim.fade_in;
    private static final String TAG = UrlAndId.class.getSimpleName();

    public static class Builder {
        final RequestManager requestManager;
        final UrlAndId d;

        public static Builder from(@NonNull RequestManager requestManager, UrlAndId song) {
            return new Builder(requestManager, song);
        }

        private Builder(@NonNull RequestManager requestManager, UrlAndId song) {
            this.requestManager = requestManager;
            this.d = song;
        }

        public BitmapBuilder asBitmap() {
            return new BitmapBuilder(this);
        }

        public Builder setDefaultImage(@DrawableRes int res) {
            DEFAULT_ERROR_IMAGE = res;
            return this;
        }

        public DrawableRequestBuilder<GlideDrawable> build() {
            //noinspection unchecked
            return ((DrawableTypeRequest) requestManager.load(d.url))
                    .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                    .error(DEFAULT_ERROR_IMAGE)
                    .animate(DEFAULT_ANIMATION)
                    .signature(createSignature(d));
        }
    }

    public static class BitmapBuilder {
        private final Builder builder;

        public BitmapBuilder(Builder builder) {
            this.builder = builder;
        }

        public BitmapRequestBuilder<?, Bitmap> build() {
            //noinspection unchecked
            return ((DrawableTypeRequest) builder.requestManager.load(builder.d.url))
                    .asBitmap()
                    .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                    .error(DEFAULT_ERROR_IMAGE)
                    .animate(DEFAULT_ANIMATION)
                    .signature(createSignature(builder.d));
        }
    }

    public static Key createSignature(UrlAndId d) {
        return new MediaStoreSignature("", d.id, 0);
    }

    public static class UrlAndId {
        public int id;

        public String url;

        public UrlAndId(int id, String url) {
            this.id = id;
            this.url = url;
        }
    }

}
