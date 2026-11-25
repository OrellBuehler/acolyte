package ch.orellbuehler.acolyte.contract

import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ApiException
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.AppEndpoints
import ch.orellbuehler.acolyte.api.AuthEndpoints
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.DownloadsEndpoints
import ch.orellbuehler.acolyte.api.HomeEndpoints
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.platformEngine
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.SessionState
import ch.orellbuehler.acolyte.session.SignInStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class DemoServerTest {
    private val url = System.getenv("ACOLYTE_DEMO_URL").orEmpty()

    @Test
    fun demoServerFeedsTheScreenshotScreens() {
        assumeTrue("ACOLYTE_DEMO_URL is not set", url.isNotBlank())
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val sdk = AcolyteSdk(MemoryStore(), "demo-test", ::platformEngine, scope)
                sdk.session.restore()
                val flow = sdk.signIn()
                flow.connect(url)
                assertEquals(SignInStep.CREDENTIALS, flow.state.value.step, "connect: ${flow.state.value.error}")
                flow.signIn("noor", "demo")
                assertEquals(SignInStep.DONE, flow.state.value.step, "sign-in: ${flow.state.value.error}")
                val account = assertIs<SessionState.SignedIn>(sdk.session.state.value).account
                println("tabs=${account.tabs} more=${account.moreGroups.map { it.group to it.sections }}")

                val client = sdk.session.client ?: fail("no client")
                val strict = ApiClient(client.baseUrl, client.credentials, platformEngine(), json = ApiJson.strict)
                val problems = mutableListOf<String>()

                suspend fun check(
                    name: String,
                    block: suspend () -> Unit,
                ) {
                    try {
                        block()
                    } catch (e: ApiException) {
                        problems += "$name: ${e.error.code} ${e.error.status} ${e.error.message.take(300)}"
                    } catch (e: Exception) {
                        problems += "$name: ${e::class.simpleName} ${e.message?.take(300)}"
                    }
                }
                check("me") { strict.call(AuthEndpoints.me) }
                check("parts") { strict.call(AppEndpoints.parts) }
                check("profile") { strict.call(AppEndpoints.profile) }
                check("about") { strict.call(AppEndpoints.about) }
                check("home") { strict.call(HomeEndpoints.home) }
                check("health") { strict.call(HomeEndpoints.health) }
                check("titles") { strict.call(LibraryEndpoints.titles, "grid" to 1) }
                check("title 1") { strict.call(LibraryEndpoints.titleVersions, "id" to 1) }
                check("title 13") { strict.call(LibraryEndpoints.titleVersions, "id" to 13) }
                check("downloads") { strict.call(DownloadsEndpoints.view) }
                check("asks") { strict.call(RequestsEndpoints.asks) }
                check("owned") { strict.call(DiscoverEndpoints.owned) }
                check("head") { strict.call(DiscoverEndpoints.head, "type" to "all", "tz" to null, "services" to "", "region" to null) }
                check("details movie") { strict.call(TitleEndpoints.details, "kind" to "movie", "tmdb_id" to "910004") }
                check("details show") { strict.call(TitleEndpoints.details, "kind" to "show", "tmdb_id" to "920001") }
                strict.close()

                val home = sdk.home(0)
                home.refresh()
                val homeState = home.state.value
                println("home: ${homeState.status.phase} pictures=${homeState.pictures.size} tiles=${homeState.tiles.size}")
                assertEquals(Phase.READY, homeState.status.phase, "home: ${homeState.status.error}")

                val explore = sdk.explore("US", "UTC")
                explore.refresh()
                val exploreState = explore.state.value
                println("explore: ${exploreState.status.phase} hero=${exploreState.hero.size} rows=${exploreState.rows.size}")
                assertEquals(Phase.READY, exploreState.status.phase, "explore: ${exploreState.status.error}")

                val library = sdk.library(null)
                library.refresh()
                println("library: ${library.state.value.status.phase} items=${library.state.value.items.size}")
                assertTrue(
                    library.state.value.items
                        .isNotEmpty(),
                    "library empty: ${library.state.value.status.error}",
                )

                val title = sdk.title("movie", "910004", 0, false)
                title.refresh()
                println("title: ${title.state.value.status.phase} name=${title.state.value.details?.name}")
                assertEquals(Phase.READY, title.state.value.status.phase, "title: ${title.state.value.status.error}")

                val downloads = sdk.downloads()
                downloads.refresh()
                println("downloads: ${downloads.state.value.status.phase} items=${downloads.state.value.items.size}")
                assertTrue(
                    downloads.state.value.items
                        .isNotEmpty(),
                    "downloads empty: ${downloads.state.value.status.error}",
                )

                val requests = sdk.requests()
                requests.refresh()
                println("requests: ${requests.state.value.status.phase} asks=${requests.state.value.asks.size}")
                assertTrue(
                    requests.state.value.asks
                        .isNotEmpty(),
                    "requests empty: ${requests.state.value.status.error}",
                )

                if (problems.isNotEmpty()) fail("Strict decoding problems:\n" + problems.joinToString("\n"))
            } finally {
                scope.cancel()
            }
        }
    }
}
