package top.geek_studio.chenlongcould.musicplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private val Context.settingsDataStore by preferencesDataStore(name = "acg_player_settings")

class SettingsRepository(
    private val context: Context,
) {
    val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                preferences[THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.SYSTEM
            }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
