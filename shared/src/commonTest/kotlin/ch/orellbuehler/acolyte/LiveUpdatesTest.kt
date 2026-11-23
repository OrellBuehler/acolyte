package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.live.LiveUpdates
import ch.orellbuehler.acolyte.live.SseParser
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveUpdatesTest {
    @Test
    fun parsesEventStreamLines() {
        val parser = SseParser()
        assertNull(parser.feed("retry: 3000"))
        assertNull(parser.feed(""))
        assertNull(parser.feed(": ping"))
        assertNull(parser.feed("id: b1-7"))
        assertNull(parser.feed("data: {\"topic\":\"health\","))
        assertNull(parser.feed("data: \"data\":{\"checks\":2}}"))
        val message = parser.feed("")!!
        assertEquals("b1-7", message.id)
        assertEquals("{\"topic\":\"health\",\n\"data\":{\"checks\":2}}", message.data)
    }

    @Test
    fun streamsEventsAndResumes() =
        runTest {
            val server = FakeServer()
            val stream =
                "retry: 3000\n\n: ping\ndata: {}\n\nid: boot-1\ndata: {\"topic\":\"health\",\"data\":{\"checks\":2}}\n\n"
            server.on("GET", "/live") {
                respond(stream, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/event-stream"))
            }
            val client = server.client(listOf(FakeServer.SESSION))
            val live = LiveUpdates(client, backgroundScope)
            val first = live.events.first()
            assertEquals("health", first.topic)
            assertEquals(
                "2",
                first.data.jsonObject["checks"]
                    ?.jsonPrimitive
                    ?.content,
            )
            assertEquals("boot-1", first.id)
            live.events.take(1).toList()
            assertEquals("boot-1", server.requests.last { it.path == "/live" }.headers["Last-Event-ID"])
            assertEquals("holyarrs_session=token-1", server.request("GET", "/live").headers["Cookie"])
        }
}
