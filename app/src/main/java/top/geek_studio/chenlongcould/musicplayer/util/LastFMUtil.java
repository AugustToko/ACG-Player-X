package top.geek_studio.chenlongcould.musicplayer.util;

import top.geek_studio.chenlongcould.musicplayer.lastfm.rest.model.LastFmAlbum;
import top.geek_studio.chenlongcould.musicplayer.lastfm.rest.model.LastFmArtist;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class LastFMUtil {
    public enum ImageSize {
        SMALL, MEDIUM, LARGE, EXTRALARGE, MEGA, UNKNOWN
    }

    public static String getLargestArtistImageUrl(List<LastFmArtist.Artist.Image> images) {
        Map<ImageSize, String> imageUrls = new HashMap<>();
        for (LastFmArtist.Artist.Image image : images) {
            ImageSize size = null;
            final String attribute = image.getSize();
            if (attribute == null) {
                size = ImageSize.UNKNOWN;
            } else {
                try {
                    size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH));
                } catch (final IllegalArgumentException e) {
                    // if they suddenly again introduce a new image size
                }
            }
            if (size != null) {
                imageUrls.put(size, image.getText());
            }
        }
        return getLargestImageUrl(imageUrls);
    }

    public static String getLargestAlbumImageUrl(List<LastFmAlbum.Album.Image> images) {
        Map<ImageSize, String> imageUrls = new HashMap<>();
        for (LastFmAlbum.Album.Image image : images) {
            ImageSize size = null;
            final String attribute = image.getSize();
            if (attribute == null) {
                size = ImageSize.UNKNOWN;
            } else {
                try {
                    size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH));
                } catch (final IllegalArgumentException e) {
                    // if they suddenly again introduce a new image size
                }
            }
            if (size != null) {
                imageUrls.put(size, image.getText());
            }
        }
        return getLargestImageUrl(imageUrls);
    }

    private static String getLargestImageUrl(Map<ImageSize, String> imageUrls) {
        if (imageUrls.containsKey(ImageSize.MEGA)) {
            return imageUrls.get(ImageSize.MEGA);
        }
        if (imageUrls.containsKey(ImageSize.EXTRALARGE)) {
            return imageUrls.get(ImageSize.EXTRALARGE);
        }
        if (imageUrls.containsKey(ImageSize.LARGE)) {
            return imageUrls.get(ImageSize.LARGE);
        }
        if (imageUrls.containsKey(ImageSize.MEDIUM)) {
            return imageUrls.get(ImageSize.MEDIUM);
        }
        if (imageUrls.containsKey(ImageSize.SMALL)) {
            return imageUrls.get(ImageSize.SMALL);
        }
        if (imageUrls.containsKey(ImageSize.UNKNOWN)) {
            return imageUrls.get(ImageSize.UNKNOWN);
        }
        return null;
    }
}
