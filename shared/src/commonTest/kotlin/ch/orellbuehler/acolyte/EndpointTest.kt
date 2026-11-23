package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ArtworkEndpoints
import ch.orellbuehler.acolyte.api.HealthEndpoints
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.image.Images
import ch.orellbuehler.acolyte.session.ServerAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EndpointTest {
    @Test
    fun buildsUrlsUnderBasePath() {
        assertEquals("$BASE/api/v1/health/checks", HealthEndpoints.checks.url(BASE, emptyMap()))
        assertEquals("http://10.0.0.2:6262/api/v1/health/checks", HealthEndpoints.checks.url("http://10.0.0.2:6262", emptyMap()))
        assertEquals(
            "$BASE/api/v1/artwork/tmdb/w342/a%20b.jpg",
            ArtworkEndpoints.tmdb.url(BASE, mapOf("size" to "w342", "file" to "a b.jpg")),
        )
    }

    @Test
    fun rejectsUndeclaredParameters() {
        assertFailsWith<IllegalArgumentException> { HealthEndpoints.checks.url(BASE, mapOf("page" to 1)) }
        assertFailsWith<IllegalArgumentException> { ArtworkEndpoints.tmdb.url(BASE, mapOf("size" to "w342")) }
    }

    @Test
    fun normalizesServerAddresses() {
        assertEquals(listOf("https://nas:6262", "http://nas:6262"), ServerAddress.candidates(" nas:6262/ "))
        assertEquals(listOf("https://media.example.com/holyarrs"), ServerAddress.candidates("https://media.example.com/holyarrs/login"))
        assertEquals(listOf("http://10.0.0.2:6262"), ServerAddress.candidates("http://10.0.0.2:6262/api/v1/auth/me"))
        assertEquals(listOf("https://host"), ServerAddress.candidates("https://host/?x=1#y"))
        assertTrue(ServerAddress.candidates("ftp://host").isEmpty())
        assertTrue(ServerAddress.candidates("not a host").isEmpty())
        assertEquals("media.example.com/holyarrs", ServerAddress.displayName(BASE))
        assertEquals("nas:6262", ServerAddress.displayName("http://nas:6262"))
    }

    @Test
    fun rewritesArtworkToTheServer() {
        val server = FakeServer()
        val client = server.client(listOf(FakeServer.SESSION))
        val images = Images { client }
        assertEquals(
            "$BASE/api/v1/artwork/tmdb/w780/abc.jpg",
            images.url("https://image.tmdb.org/t/p/original/abc.jpg", ImageSize.BACKDROP),
        )
        assertEquals("$BASE/api/v1/users/1/avatar?v=3", images.url("/api/v1/users/1/avatar?v=3"))
        assertEquals("https://coverartarchive.org/release/x/front-500", images.url("https://coverartarchive.org/release/x/front-500"))
        assertNull(images.url(""))
        assertEquals(mapOf("Cookie" to "holyarrs_session=token-1"), images.headers("$BASE/api/v1/artwork/x"))
        assertTrue(images.headers("https://coverartarchive.org/release/x").isEmpty())
        val keyed = server.client(apiKey = "hak_1")
        assertEquals(mapOf("X-Api-Key" to "hak_1"), Images { keyed }.headers("$BASE/api/v1/artwork/x"))
    }
}
