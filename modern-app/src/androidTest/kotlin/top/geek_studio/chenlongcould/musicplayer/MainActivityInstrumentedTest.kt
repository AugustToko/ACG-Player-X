package top.geek_studio.chenlongcould.musicplayer

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.geek_studio.chenlongcould.musicplayer.shortcuts.AppShortcuts

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shellNavigatesBetweenLibraryAndSettings() {
        composeRule.onNodeWithText("ACG Player X").assertIsDisplayed()

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("音乐来源").assertIsDisplayed()

        composeRule.onNodeWithText("音乐库").performClick()
        composeRule.onNodeWithText("ACG Player X").assertIsDisplayed()
    }

    @Test
    fun warmShortcutRoutesToFavorites() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.dispatchNavigationIntent(
                Intent(activity, MainActivity::class.java)
                    .setAction(AppShortcuts.ACTION_OPEN_FAVORITES),
            )
        }

        composeRule.waitForIdle()
        composeRule
            .onNode(
                hasText("收藏", substring = true) and
                    hasText("总库", substring = true),
            )
            .assertIsDisplayed()
    }

    @Test
    fun activityRecreationKeepsCurrentDestination() {
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("音乐来源").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("音乐来源").assertIsDisplayed()
    }
}
