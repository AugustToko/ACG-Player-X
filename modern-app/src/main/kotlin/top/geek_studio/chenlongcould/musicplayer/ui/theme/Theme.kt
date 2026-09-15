package top.geek_studio.chenlongcould.musicplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import top.geek_studio.chenlongcould.musicplayer.data.ThemeMode

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE9DDFF),
        onPrimaryContainer = Color(0xFF22005D),
        secondary = Color(0xFF625B71),
        secondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFF7D5260),
        tertiaryContainer = Color(0xFFFFD8E4),
        surface = Color(0xFFFFF8FF),
        surfaceVariant = Color(0xFFE7E0EC),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFCFBCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFE9DDFF),
        secondary = Color(0xFFCCC2DC),
        secondaryContainer = Color(0xFF4A4458),
        tertiary = Color(0xFFEFB8C8),
        tertiaryContainer = Color(0xFF633B48),
        surface = Color(0xFF141218),
        surfaceVariant = Color(0xFF49454F),
    )

@Composable
fun AcgPlayerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else if (darkTheme) {
            DarkColors
        } else {
            LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
