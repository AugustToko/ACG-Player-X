package top.geek_studio.chenlongcould.musicplayer.glide.artistimage;

import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ArtistImage {
    public final String artistName;

    // filePath to get4LastFM the image of the artist
    public final List<AlbumCover> albumCovers;

    public ArtistImage(String artistName, final List<AlbumCover> albumCovers) {

        this.artistName = artistName;
        this.albumCovers = albumCovers;
    }

    public String toIdString() {
        StringBuilder id = new StringBuilder(artistName);
        for (AlbumCover albumCover: albumCovers) {
            id.append(albumCover.getYear()).append(albumCover.getFilePath());
        }
        return id.toString();
    }
}
