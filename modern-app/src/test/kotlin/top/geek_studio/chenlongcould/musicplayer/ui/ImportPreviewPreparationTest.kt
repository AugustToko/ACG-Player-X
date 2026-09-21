package top.geek_studio.chenlongcould.musicplayer.ui

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ImportPreviewPreparationTest(
    @Suppress("unused") private val name: String,
    private val scenario: () -> Unit,
) {
    @Test
    fun preparationContract() = scenario()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> =
            ImportPreviewPreparationCases.cases.map { (name, run) -> arrayOf<Any>(name, run) }
    }
}
