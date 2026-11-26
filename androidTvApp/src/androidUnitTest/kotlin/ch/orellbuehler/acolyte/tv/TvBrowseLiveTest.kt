package ch.orellbuehler.acolyte.tv

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.requestFocus
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.tv.nav.TvApp
import ch.orellbuehler.acolyte.ui.SessionViewModel
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class TvBrowseLiveTest {
    @get:Rule
    val compose = createComposeRule()

    private val url = System.getenv("ACOLYTE_LIVE_URL").orEmpty()
    private val user = System.getenv("ACOLYTE_LIVE_USER").orEmpty()
    private val password = System.getenv("ACOLYTE_LIVE_PASSWORD").orEmpty()

    private fun shoot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File("build/tv-screens/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun press(
        text: String,
        last: Boolean = false,
    ) {
        val nodes = compose.onAllNodesWithText(text)
        val node = if (last) nodes.onLast() else nodes.onFirst()
        node.requestFocus()
        compose.waitForIdle()
        node.performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private fun waitForText(text: String) {
        val deadline = System.currentTimeMillis() + 30_000
        while (compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            if (System.currentTimeMillis() > deadline) {
                val tree = runCatching { compose.onRoot().printToString() }.getOrElse { "no tree: $it" }
                throw AssertionError("no '$text' on screen:\n$tree")
            }
            ShadowLooper.idleMainLooper()
            compose.waitForIdle()
            Thread.sleep(50)
        }
    }

    @Test
    fun browseScreensLoadFromARealServer() {
        assumeTrue("ACOLYTE_LIVE_URL is not set", url.isNotBlank() && user.isNotBlank())
        val sdk = AcolyteSdk(MemoryStore(), "test")
        val model = SessionViewModel(sdk)
        compose.setContent { TvApp(sdk, model) }
        waitForText("Connect to HolyArrs")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(url)
        press("Continue")
        waitForText("Sign in")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields.onFirst().performTextInput(user)
        fields.onLast().performTextInput(password)
        press("Sign in", last = true)
        waitForText("Explore")
        press("Explore")
        waitForText("Movies")
        compose.waitForIdle()
        shoot("live-explore")
        press("Search")
        waitForText("Shows, movies and people")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("dune")
        compose.waitForIdle()
        shoot("live-search")
    }
}
