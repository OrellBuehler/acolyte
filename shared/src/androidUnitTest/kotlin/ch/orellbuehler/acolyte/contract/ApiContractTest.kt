package ch.orellbuehler.acolyte.contract

import ch.orellbuehler.acolyte.nav.Section
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class ApiContractTest {
    private val specFile = File(System.getProperty("acolyte.spec") ?: "../api/openapi.json")
    private val checker = SpecChecker(specFile)
    private val repo: File = specFile.absoluteFile.parentFile?.parentFile ?: File("..")

    private fun assertNone(
        title: String,
        problems: List<String>,
    ) {
        if (problems.isNotEmpty()) fail("$title (${problems.size}):\n" + problems.joinToString("\n") { "  - $it" })
    }

    @Test
    fun everyEndpointMatchesTheSpec() {
        assertTrue(checker.endpoints.isNotEmpty(), "no endpoints found")
        val problems = checker.endpoints.flatMap { checker.check(it) }
        assertNone(
            "Client endpoints that do not match api/openapi.json (fix the endpoint or model; when the spec itself is wrong, " +
                "add the exact line with the reason to KnownSpecGaps.problems)",
            problems.filter { it !in KnownSpecGaps.problems },
        )
        assertNone("Stale KnownSpecGaps entries (remove them)", (KnownSpecGaps.problems.keys - problems.toSet()).toList())
    }

    @Test
    fun endpointGroupsAreObjects() {
        assertNone("EndpointGroup subclasses must be objects (data object X : EndpointGroup())", checker.groupsWithoutObject)
    }

    @Test
    fun endpointsAreDeclaredOnce() {
        val duplicates =
            checker.endpoints
                .groupBy { it.key }
                .filterValues { it.size > 1 }
                .keys
                .toList()
        assertNone("Endpoints declared more than once", duplicates)
    }

    @Test
    fun sectionsMatchTheSpec() {
        val problems =
            Section.entries.mapNotNull { section ->
                val (method, path) = section.operation.split(" ", limit = 2)
                val op = checker.operation(method, path) ?: return@mapNotNull "${section.name}: ${section.operation} is not in the spec"
                val need = checker.expectedNeed(op)
                val part = (op["x-holyarrs-part"] as? JsonPrimitive)?.contentOrNull
                when {
                    need != section.need.wire -> "${section.name}: need is ${section.need.wire}, the spec says $need"
                    part != section.part.wire -> "${section.name}: part is ${section.part.wire}, the spec says $part"
                    else -> null
                }
            }
        assertNone("Sections that do not match the spec", problems)
    }

    @Test
    fun onlyEndpointGroupsNameApiPaths() {
        val allowed = Regex(".*/shared/src/commonMain/kotlin/.*/(api/[A-Za-z]+Endpoints\\.kt|api/ApiClient\\.kt|nav/Section\\.kt)$")
        val roots = listOf("shared/src/commonMain", "shared/src/androidMain", "shared/src/iosMain", "androidApp/src", "iosApp/Acolyte")
        val problems =
            roots
                .map { File(repo, it) }
                .filter { it.exists() }
                .flatMap { root -> root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "swift") } }
                .filter { !allowed.matches(it.invariantSeparatorsPath) && it.readText().contains("/api/v1/") }
                .map { it.relativeTo(repo).path }
        assertNone("API paths outside an EndpointGroup (declare them in shared/.../api/<Feature>Endpoints.kt)", problems)
    }

    @Test
    fun onlyTheApiClientMakesRequests() {
        val root = File(repo, "shared/src")
        val problems =
            root
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" && !it.path.contains("Test/") }
                .filter { file ->
                    val text = file.readText()
                    text.contains("import io.ktor.client.request.") || text.contains("import io.ktor.client.HttpClient\n")
                }.map { it.relativeTo(repo).invariantSeparatorsPath }
                .filter { it != "shared/src/commonMain/kotlin/ch/orellbuehler/acolyte/api/ApiClient.kt" }
                .toList()
        assertNone("HTTP requests outside ApiClient (go through an Endpoint instead)", problems)
    }

    @Test
    fun writesTheEndpointReport() {
        checker.endpoints.forEach { checker.check(it) }
        val reportDir = File(System.getProperty("acolyte.reportDir") ?: "build/reports/api").apply { mkdirs() }
        val report =
            buildJsonArray {
                for (endpoint in checker.endpoints.sortedBy { it.key }) {
                    val op = checker.operation(endpoint.method.value, endpoint.path)
                    add(
                        buildJsonObject {
                            put("method", endpoint.method.value)
                            put("path", endpoint.path)
                            put("operationId", (op?.get("operationId") as? JsonPrimitive)?.contentOrNull)
                            put("need", endpoint.need.wire)
                            put("part", endpoint.part.wire)
                            put("scope", (op?.get("x-holyarrs-scope") as? JsonPrimitive)?.contentOrNull)
                            put("typedResponse", op?.let { checker.isTyped(checker.responseSchema(it)) } ?: false)
                            put("query", JsonArray(endpoint.query.map { JsonPrimitive(it) }))
                        },
                    )
                }
                for (section in Section.entries) {
                    val (method, path) = section.operation.split(" ", limit = 2)
                    if (checker.endpoints.none { it.method.value == method && it.path == path }) {
                        add(
                            buildJsonObject {
                                put("method", method)
                                put("path", path)
                                put("section", section.name)
                            },
                        )
                    }
                }
            }
        File(reportDir, "client-endpoints.json").writeText(report.toString())
        val typed = report.count { (it as JsonObject)["typedResponse"]?.jsonPrimitive?.contentOrNull == "true" }
        val summary =
            buildString {
                appendLine("## App API surface")
                appendLine()
                appendLine("Spec: HolyArrs ${File(repo, "api/VERSION").takeIf { it.exists() }?.readText()?.trim()}")
                appendLine()
                appendLine(
                    "- ${checker.endpoints.size} endpoints in ${checker.groups.size} groups, $typed with a typed response in the spec",
                )
                appendLine("- ${Section.entries.size} sections")
                if (checker.notes.isNotEmpty()) {
                    appendLine()
                    appendLine("Notes:")
                    checker.notes
                        .distinct()
                        .sorted()
                        .forEach { appendLine("- $it") }
                }
            }
        File(reportDir, "summary.md").writeText(summary)
    }
}
