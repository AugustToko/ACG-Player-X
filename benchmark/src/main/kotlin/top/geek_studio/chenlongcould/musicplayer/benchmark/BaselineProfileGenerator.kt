package top.geek_studio.chenlongcould.musicplayer.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupProfile() =
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true,
            filterPredicate = ::isApplicationProfileRule,
        ) {
            launchFromHome()
        }

    @Test
    fun commonUserJourneys() =
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = false,
            filterPredicate = ::isApplicationProfileRule,
        ) {
            launchFromHome()
            openDestination("设置")
            swipeSettingsUp()
            swipeSettingsDown()
            openDestination("播放")
            openDestination("歌单")
            openDestination("音乐库")
        }

    private fun isApplicationProfileRule(rule: String): Boolean =
        rule.contains(APPLICATION_RULE_PREFIX)

    private companion object {
        val APPLICATION_RULE_PREFIX = "L${TARGET_PACKAGE.replace('.', '/')}/"
    }
}
