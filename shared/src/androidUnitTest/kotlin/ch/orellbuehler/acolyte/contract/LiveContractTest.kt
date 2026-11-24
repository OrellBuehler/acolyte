package ch.orellbuehler.acolyte.contract

import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.api.ApiClient
import ch.orellbuehler.acolyte.api.ApiException
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.Endpoint
import ch.orellbuehler.acolyte.api.NoContent
import ch.orellbuehler.acolyte.api.platformEngine
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveStatus
import ch.orellbuehler.acolyte.session.MemoryStore
import ch.orellbuehler.acolyte.session.Role
import ch.orellbuehler.acolyte.session.SessionState
import ch.orellbuehler.acolyte.session.SignInMethod
import ch.orellbuehler.acolyte.session.SignInStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail

class LiveContractTest {
    private val url = System.getenv("ACOLYTE_LIVE_URL").orEmpty()
    private val user = System.getenv("ACOLYTE_LIVE_USER").orEmpty()
    private val password = System.getenv("ACOLYTE_LIVE_PASSWORD").orEmpty()
    private val apiKey = System.getenv("ACOLYTE_LIVE_API_KEY").orEmpty()
    private val specFile = File(System.getProperty("acolyte.spec") ?: "../api/openapi.json")

    private fun sdk(scope: CoroutineScope) = AcolyteSdk(MemoryStore(), "live-test", ::platformEngine, scope)

    private fun readEndpoints(): List<Endpoint<*, *>> {
        val checker = SpecChecker(specFile)
        return checker.endpoints.filter { endpoint ->
            val op = checker.operation(endpoint.method.value, endpoint.path)
            endpoint.method.value == "GET" &&
                endpoint.key !in KnownSpecGaps.liveSkips &&
                endpoint.pathParams.isEmpty() &&
                endpoint.response !== NoContent &&
                endpoint.key !in KnownSpecGaps.liveSkips &&
                op != null &&
                checker.requiredQuery(op).isEmpty()
        }
    }

    private suspend fun decodeAll(client: ApiClient): List<String> {
        val strict = ApiClient(client.baseUrl, client.credentials, platformEngine(), json = ApiJson.strict)
        val problems = mutableListOf<String>()
        for (endpoint in readEndpoints()) {
            @Suppress("UNCHECKED_CAST")
            val typed = endpoint as Endpoint<Unit, Any?>
            try {
                strict.call(typed)
                client.call(typed)
            } catch (e: ApiException) {
                problems += "${endpoint.key}: ${e.error.code} ${e.error.status} ${e.error.message.take(300)}"
            }
        }
        strict.close()
        return problems
    }

    @Test
    fun passwordSignInReadsEverything() {
        assumeTrue("ACOLYTE_LIVE_URL is not set", url.isNotBlank() && user.isNotBlank())
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val sdk = sdk(scope)
                sdk.session.restore()
                val flow = sdk.signIn()
                flow.connect(url)
                assertEquals(SignInStep.CREDENTIALS, flow.state.value.step, "connect: ${flow.state.value.error}")
                flow.signIn(user, password)
                assertEquals(SignInStep.DONE, flow.state.value.step, "sign-in: ${flow.state.value.error}")
                val account = assertIs<SessionState.SignedIn>(sdk.session.state.value).account
                assertEquals(Role.OWNER, account.role)
                assertEquals(SignInMethod.PASSWORD, account.method)
                assertEquals(File(specFile.parentFile, "VERSION").readText().trim(), account.serverVersion)

                val client = sdk.session.client ?: fail("no client after sign-in")
                val problems = decodeAll(client)
                if (problems.isNotEmpty()) fail("Read endpoints that fail against the live server:\n" + problems.joinToString("\n"))

                val health = sdk.health()
                health.load()
                assertEquals(Phase.READY, health.state.value.status.phase, "health: ${health.state.value.status.error}")
                health.runChecks()
                assertEquals(null, health.state.value.actionError, "run checks (CSRF)")

                val collector = scope.launch { sdk.session.liveEvents.collect {} }
                withTimeout(20_000) { sdk.session.liveStatus.first { it == LiveStatus.LIVE } }
                collector.cancel()

                sdk.session.signOut()
                assertIs<SessionState.SignedOut>(sdk.session.state.value)
            } finally {
                scope.cancel()
            }
        }
    }

    @Test
    fun apiKeySignInReadsEverything() {
        assumeTrue("ACOLYTE_LIVE_API_KEY is not set", url.isNotBlank() && apiKey.isNotBlank())
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val sdk = sdk(scope)
                sdk.session.restore()
                val flow = sdk.signIn()
                flow.connect(url)
                flow.useApiKey(apiKey)
                assertEquals(SignInStep.DONE, flow.state.value.step, "api key: ${flow.state.value.error}")
                val account = assertIs<SessionState.SignedIn>(sdk.session.state.value).account
                assertEquals(SignInMethod.API_KEY, account.method)
                val client = sdk.session.client ?: fail("no client after sign-in")
                val problems = decodeAll(client)
                if (problems.isNotEmpty()) fail("Read endpoints that fail with an API key:\n" + problems.joinToString("\n"))
            } finally {
                scope.cancel()
            }
        }
    }
}
