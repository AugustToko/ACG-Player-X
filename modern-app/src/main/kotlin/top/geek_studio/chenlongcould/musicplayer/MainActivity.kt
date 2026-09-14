package top.geek_studio.chenlongcould.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.geek_studio.chenlongcould.musicplayer.ui.AcgPlayerApp
import top.geek_studio.chenlongcould.musicplayer.ui.MainViewModel
import top.geek_studio.chenlongcould.musicplayer.ui.theme.AcgPlayerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            AcgPlayerTheme(themeMode = state.themeMode) {
                AcgPlayerApp(
                    state = state,
                    viewModel = viewModel,
                )
            }
        }
    }
}
