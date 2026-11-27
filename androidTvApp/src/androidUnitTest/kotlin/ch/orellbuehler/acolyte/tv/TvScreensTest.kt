package ch.orellbuehler.acolyte.tv

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.navigation.compose.rememberNavController
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.api.model.TitleDetails
import ch.orellbuehler.acolyte.api.model.TitlePerson
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.discover.CardMark
import ch.orellbuehler.acolyte.feature.discover.CardRow
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.MarkLabel
import ch.orellbuehler.acolyte.feature.discover.MarkState
import ch.orellbuehler.acolyte.feature.home.ArrivedTitle
import ch.orellbuehler.acolyte.feature.home.ComingUp
import ch.orellbuehler.acolyte.feature.home.HomePicture
import ch.orellbuehler.acolyte.feature.home.HomeState
import ch.orellbuehler.acolyte.feature.home.PictureLabel
import ch.orellbuehler.acolyte.feature.home.UpWhen
import ch.orellbuehler.acolyte.feature.requests.AskSeasonRow
import ch.orellbuehler.acolyte.feature.requests.AskSheetState
import ch.orellbuehler.acolyte.feature.requests.EpisodesLoad
import ch.orellbuehler.acolyte.feature.requests.FOUR_K_LINE
import ch.orellbuehler.acolyte.feature.requests.PillNote
import ch.orellbuehler.acolyte.feature.requests.QualityPill
import ch.orellbuehler.acolyte.feature.requests.ShowMode
import ch.orellbuehler.acolyte.feature.title.TitleAccess
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.tv.ask.TvAskActions
import ch.orellbuehler.acolyte.tv.ask.TvAskContent
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvErrorView
import ch.orellbuehler.acolyte.tv.components.TvOpenOnPhoneView
import ch.orellbuehler.acolyte.tv.home.TvHomeContent
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.nav.LocalTvSignOut
import ch.orellbuehler.acolyte.tv.nav.TvApp
import ch.orellbuehler.acolyte.tv.nav.TvMainTabs
import ch.orellbuehler.acolyte.tv.nav.TvNavigator
import ch.orellbuehler.acolyte.tv.nav.TvSectionPlaceholder
import ch.orellbuehler.acolyte.tv.nav.TvSections
import ch.orellbuehler.acolyte.tv.nav.TvTabSelection
import ch.orellbuehler.acolyte.tv.settings.TvSettingsScreen
import ch.orellbuehler.acolyte.tv.signin.TvSignInScreen
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvTheme
import ch.orellbuehler.acolyte.tv.title.TvTitleContent
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.SessionViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class TvScreensTest {
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

    private fun card(
        key: String,
        name: String,
        kind: CardKind = CardKind.MOVIE,
        mark: CardMark? = null,
    ) = DiscoverCard(
        key = key,
        kind = kind,
        tmdb = key,
        titleId = 0,
        name = name,
        year = 2024,
        overview = "A retired lighthouse keeper finds a message in a bottle and follows it across three coasts.",
        genres = listOf("Drama", "Thriller", "Mystery"),
        mark = mark,
    )

    @Test
    fun tabsListSectionsAndSettingsLast() {
        val selection = TvTabSelection(TvSections.tabs(account))
        show { TvMainTabs(account, selection) { TvSectionPlaceholder(it) } }
        compose.onAllNodesWithText("Home").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("Settings").onFirst().assertIsDisplayed()
        shoot("tabs")
    }

    @Test
    fun homeShowsBillboardFeaturedAndRows() {
        val featured = card("f1", "The Quiet Harbour", mark = CardMark(MarkState.AVAILABLE, MarkLabel.IN_LIBRARY))
        val state =
            HomeState(
                status = LoadStatus(Phase.READY),
                pictures =
                    listOf(
                        HomePicture(featured, PictureLabel.NEW_SEASON, ArrivedTitle.JUST_ARRIVED),
                        HomePicture(card("f2", "Salt and Iron", CardKind.SHOW), PictureLabel.ARRIVED, ArrivedTitle.NEW_TODAY),
                        HomePicture(card("f3", "Northern Lights"), PictureLabel.FOLLOWING, ArrivedTitle.JUST_ARRIVED),
                    ),
                arrived =
                    CardRow(
                        id = "arrived",
                        type = "arrived",
                        title = "Arrived",
                        cards = (1..8).map { card("a$it", "Arrival $it") },
                        wide = false,
                    ),
                comingUp = listOf(ComingUp("u1", 0, "u1", "show", "Tidewater", null, "Season 2", UpWhen.TEXT, "", "Friday")),
            )
        show { TvHomeContent(state) {} }
        compose.onNodeWithText("Featured").assertIsDisplayed()
        compose.onNodeWithText("The Quiet Harbour").assertIsDisplayed()
        shoot("home")
        compose.onNodeWithText("New today").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithText("Salt and Iron").assertIsDisplayed()
        shoot("home-focused")
    }

    @Test
    fun signInStartsOnTheServerStep() {
        show { TvSignInScreen(expired = false) }
        compose.onNodeWithText("Connect to HolyArrs").assertIsDisplayed()
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        shoot("signin")
    }

    @Test
    fun appRestoresIntoTheSignInScreenWithoutASession() {
        val model = SessionViewModel(sdk)
        compose.setContent { TvApp(sdk, model) }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Connect to HolyArrs").fetchSemanticsNodes().isNotEmpty() }
        shoot("app-signed-out")
    }

    @Test
    fun titlePageShowsHeroActionsAndShelves() {
        val similar = (1..6).map { TitleCard(tmdbId = "s$it", kind = "movie", name = "Similar $it", year = 2020 + it) }
        val details =
            TitleDetails(
                tmdbId = "1",
                kind = "movie",
                name = "The Quiet Harbour",
                year = 2024,
                overview = "A retired lighthouse keeper finds a message in a bottle and follows it across three coasts.",
                tagline = "Some messages are meant to be answered.",
                genres = listOf("Drama", "Thriller"),
                runtimeMinutes = 118,
                people =
                    listOf(
                        TitlePerson("Mara Venn", "actor", "Elias", "", "p1"),
                        TitlePerson("Joel Haas", "actor", "Tobias", "", "p2"),
                        TitlePerson("Ines Calder", "director", "", "", "p3"),
                    ),
                similar = similar,
            )
        val state =
            TitleState(
                kind = "movie",
                tmdb = "1",
                libraryId = 0,
                access = TitleAccess(mayAsk = true, followOn = true),
                status = LoadStatus(Phase.READY),
                details = details,
            )
        show { TvTitleContent(state, trailerLoading = false, onRefresh = {}, onTrailer = {}, onFollow = {}, episodes = null) }
        compose.onNodeWithText("The Quiet Harbour").assertIsDisplayed()
        compose.onNodeWithText("Request").assertIsDisplayed()
        shoot("title")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(1)
        compose.waitForIdle()
        compose.onNodeWithText("Cast and crew").assertIsDisplayed()
        shoot("title-shelves")
    }

    @Test
    fun askFormShowsModesQualityAndSubmit() {
        val seasons =
            (1..3).map {
                AskSeasonRow(
                    it,
                    "",
                    kept = false,
                    complete = false,
                    ticked = true,
                    expanded = false,
                    load = EpisodesLoad.NONE,
                    episodes = emptyList(),
                    picked = 0,
                )
            }
        val pills =
            listOf(
                QualityPill("standard", "standard", "Standard", 1, extra = false, disabled = false, note = PillNote.NONE),
                QualityPill("4k", FOUR_K_LINE, "", 2, extra = false, disabled = false, note = PillNote.NONE),
            )
        val state =
            AskSheetState(
                status = LoadStatus(Phase.READY),
                kind = "show",
                tmdb = "2",
                name = "Salt and Iron",
                year = 2023,
                pills = pills,
                selected = "standard",
                mode = ShowMode.ALL,
                seasons = seasons,
            )
        val actions =
            object : TvAskActions {
                override fun select(key: String) {}

                override fun toggleAlso(key: String) {}

                override fun setFollow(on: Boolean) {}

                override fun setMode(mode: ShowMode) {}

                override fun toggleSeason(number: Int) {}

                override fun submit() {}
            }
        show { TvAskContent(state, actions, onRetry = {}, onClose = {}) }
        compose.onNodeWithText("Ask for all seasons").assertIsDisplayed()
        compose.onNodeWithText("Cancel").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithText("Cancel").assertIsFocused()
        shoot("ask")
    }

    @Test
    fun settingsAsksBeforeSigningOut() {
        show { TvSettingsScreen(account) }
        compose.onNodeWithText("holyarrs.local").assertIsDisplayed()
        compose.onNodeWithText("Privacy policy: orellbuehler.github.io/acolyte/privacy").assertIsDisplayed()
        compose.onNodeWithText("Sign out").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithText("Sign out").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        compose.onNodeWithText("Sign out?").assertIsDisplayed()
        shoot("settings")
    }

    @Test
    fun statusViewsRender() {
        show {
            TvErrorView(ApiError(ApiErrorCode.OFFLINE, 0), onRetry = {})
        }
        compose.onNodeWithText("Try again").assertIsDisplayed()
        shoot("error")
    }

    @Test
    fun emptyViewRenders() {
        show { TvEmptyView() }
        compose.onNodeWithText("Nothing here yet").assertIsDisplayed()
        shoot("empty")
    }

    @Test
    fun openOnPhoneViewNamesWhatIsMissing() {
        show { TvOpenOnPhoneView("Person pages") }
        compose.onNodeWithText("Open this on your phone").assertIsDisplayed()
        shoot("phone")
    }
}
