package top.geek_studio.chenlongcould.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicPathTest {
    @Test
    fun relativePathResolvesFolderNameAndCanonicalPath() {
        assertEquals(
            MusicFolder(name = "Anime", path = "Music/Anime"),
            resolveMusicFolder("Music/Anime/", isRelativePath = true),
        )
    }

    @Test
    fun legacyFilePathUsesParentDirectory() {
        assertEquals(
            MusicFolder(name = "Anime", path = "/storage/emulated/0/Music/Anime"),
            resolveMusicFolder(
                "/storage/emulated/0/Music/Anime/song.flac",
                isRelativePath = false,
            ),
        )
    }

    @Test
    fun missingPathFallsBackToRootFolder() {
        assertEquals(
            MusicFolder(name = ROOT_FOLDER, path = ROOT_FOLDER),
            resolveMusicFolder(null, isRelativePath = true),
        )
    }
}
