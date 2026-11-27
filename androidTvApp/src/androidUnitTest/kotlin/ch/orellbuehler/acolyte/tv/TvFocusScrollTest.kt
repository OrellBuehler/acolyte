package ch.orellbuehler.acolyte.tv

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.navigation.compose.rememberNavController
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.TitleDetails
import ch.orellbuehler.acolyte.api.model.TitlePerson
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.home.HomeState
import ch.orellbuehler.acolyte.feature.title.TitleAccess
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.tv.components.TvBackdropCard
import ch.orellbuehler.acolyte.tv.components.TvBrowsePage
import ch.orellbuehler.acolyte.tv.components.TvCardRow
import ch.orellbuehler.acolyte.tv.components.TvRowFocus
import ch.orellbuehler.acolyte.tv.components.TvShelf
import ch.orellbuehler.acolyte.tv.home.TvHomeContent
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.LocalTvSignOut
import ch.orellbuehler.acolyte.tv.nav.TvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.tv.nav.TvTabSelection
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvTheme
import ch.orellbuehler.acolyte.tv.title.TvTitleContent
import ch.orellbuehler.acolyte.ui.LocalSdk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class TvFocusScrollTest {
    @get:Rule
    val compose = createComposeRule()

    private val sdk = AcolyteSdk(MemoryStore(), "test")

    private val account =
        Account(
            serverUrl = "http://holyarrs.local:6262",
            serverName = "holyarrs.local",
            method = SignInMethod.PASSWORD,
            userId = 1,
            userName = "anna",
            displayName = "Anna",
            role = Role.OWNER,
            permissions = emptyList(),
            parts = emptyList<PartView>(),
            serverVersion = null,
            avatarPath = null,
            mustSetUpTwoFactor = false,
            isKid = false,
            safeMode = false,
        )

    private val density = 2f

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            val nav = rememberNavController()
            val selection = remember { TvTabSelection(TvSections.tabs(account)) }
            val navigator = remember { TvNavigator(account, nav, selection) }
            TvTheme {
                CompositionLocalProvider(LocalSdk provides sdk, LocalTvNavigator provides navigator, LocalTvSignOut provides {}) {
                    Box(Modifier.fillMaxSize().background(TvColors.background)) { content() }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun shoot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File("build/tv-screens/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    private fun down() {
        compose.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
    }

    private fun up() {
        compose.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
    }

    private fun focusFirst() {
        compose.onAllNodes(hasClickAction()).onFirst().requestFocus()
        compose.waitForIdle()
    }

    private fun card(name: String) = DiscoverCard(key = name, kind = CardKind.MOVIE, tmdb = name, titleId = 0, name = name, year = 2024)

    private fun posterRow(title: String) = CardRow(title, title, title, cards = (1..8).map { card("$title card $it") }, wide = false)

    private fun boundsOf(text: String) =
        compose
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .maxByOrNull { it.top } ?: error("missing $text")

    private fun assertRowInSafeArea(
        header: String,
        cardText: String,
    ) {
        val margin = TvMetrics.marginY.value * density
        val head = boundsOf(header)
        val text = boundsOf(cardText)
        val captionLine = 18f * density
        val height =
            compose
                .onRoot()
                .fetchSemanticsNode()
                .size.height
        assertTrue(head.top >= margin - 2f, "$header starts at ${head.top}, above the safe area $margin")
        assertTrue(text.bottom + captionLine <= height - margin + 2f, "$cardText ends at ${text.bottom}, below the safe area")
    }

    @Test
    fun homeKeepsTheFocusedRowInsideTheSafeArea() {
        val titles = listOf("Row A", "Row B", "Row C", "Row D")
        val state =
            HomeState(
                status = LoadStatus(Phase.READY),
                arrived = posterRow("Arrived"),
                borrowed = titles.map { posterRow(it) },
            )
        show { TvHomeContent(state) {} }
        focusFirst()
        assertRowInSafeArea("Just arrived", "Arrived card 1")
        shoot("focus-home-arrived")
        titles.forEach { title ->
            down()
            assertRowInSafeArea(title, "$title card 1")
        }
        shoot("focus-home-last-row")
        up()
        assertRowInSafeArea("Row C", "Row C card 1")
        shoot("focus-home-row-above")
    }

    @Test
    fun browsePageKeepsTheFocusedRowInsideTheSafeArea() {
        val titles = listOf("Row A", "Row B", "Row C", "Row D")
        show {
            TvBrowsePage(hero = card("Hero")) { onFocusCard ->
                items(titles, key = { it }) { title -> TvCardRow(posterRow(title), onFocusCard = onFocusCard) }
            }
        }
        focusFirst()
        titles.forEach { title ->
            assertRowInSafeArea(title, "$title card 1")
            shoot("focus-browse-${title.last()}")
            down()
        }
    }

    @Test
    fun titlePageKeepsEpisodesAndCastInsideTheSafeArea() {
        val state =
            TitleState(
                kind = "show",
                tmdb = "1",
                libraryId = 0,
                access = TitleAccess(mayAsk = true, followOn = true),
                status = LoadStatus(Phase.READY),
                details =
                    TitleDetails(
                        tmdbId = "1",
                        kind = "show",
                        name = "The Quiet Harbour",
                        overview = LONG_OVERVIEW,
                        tagline = "Some messages are meant to be answered.",
                        people = (1..3).map { TitlePerson("Actor $it", "actor", "Role $it", "", "p$it") },
                    ),
            )
        show {
            TvTitleContent(
                state,
                trailerLoading = false,
                onRefresh = {},
                onTrailer = {},
                onFollow = {},
                episodes = {
                    TvShelf("Seasons") {
                        items((1..8).toList()) { number ->
                            TvBackdropCard(
                                title = "$number. Episode $number",
                                subtitle = "44 min · Friday",
                                detail = LONG_OVERVIEW,
                                image = null,
                                onClick = {},
                            )
                        }
                    }
                },
            )
        }
        down()
        assertRowInSafeArea("Seasons", "1. Episode 1")
        shoot("focus-title-episodes")
        down()
        assertRowInSafeArea("Cast and crew", "Actor 1")
        shoot("focus-title-cast")
        up()
        up()
        compose.onNodeWithText("Request").assertIsDisplayed()
        shoot("focus-title-back-to-actions")
    }

    @Test
    fun longHeroKeepsEveryActionLabelInsideItsButton() {
        val state =
            TitleState(
                kind = "show",
                tmdb = "1",
                libraryId = 0,
                access = TitleAccess(mayAsk = true, followOn = true),
                status = LoadStatus(Phase.READY),
                details =
                    TitleDetails(
                        tmdbId = "1",
                        kind = "show",
                        name = "The Quiet Harbour",
                        logo = "http://holyarrs.local/logo.png",
                        overview = LONG_OVERVIEW,
                        tagline = "Some messages are meant to be answered.",
                        genres = listOf("Comedy", "Drama", "Thriller"),
                        rating = "TV-14",
                        year = 2011,
                        people = (1..3).map { TitlePerson("Actor $it", "actor", "Role $it", "", "p$it") },
                    ),
            )
        show { TvTitleContent(state, trailerLoading = false, onRefresh = {}, onTrailer = {}, onFollow = {}, episodes = null) }
        shoot("focus-title-long-hero")
        listOf("Request", "Follow").forEach { label ->
            val height =
                compose
                    .onNodeWithText(label)
                    .fetchSemanticsNode()
                    .size.height
            assertTrue(height >= 36 * density, "$label button is squeezed to $height px")
        }
    }

    @Test
    fun rowFocusDimsEveryRowButTheFocusedOne() {
        val rows = TvRowFocus()
        val a = Any()
        val b = Any()
        assertEquals(1f, rows.alpha(a))
        rows.update(a, true)
        assertEquals(1f, rows.alpha(a))
        assertEquals(TvMetrics.DIM_ALPHA, rows.alpha(b))
        rows.update(b, true)
        rows.update(a, false)
        assertEquals(1f, rows.alpha(b))
        assertEquals(TvMetrics.DIM_ALPHA, rows.alpha(a))
        rows.update(b, false)
        assertEquals(1f, rows.alpha(a))
    }

    private companion object {
        const val LONG_OVERVIEW =
            "Comedy about the unlikely friendship that develops between two very different young women who meet " +
                "waitressing at a diner in trendy Williamsburg, Brooklyn, and form a bond over one day owning their own " +
                "successful cupcake business. Only one thing stands in their way: starting capital and a lot of luck."
    }
}
