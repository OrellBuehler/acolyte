package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.nav.destination
import kotlin.test.Test
import kotlin.test.assertEquals

class DestinationsTest {
    @Test
    fun everySectionHasItsOwnRoute() {
        val routes = Section.entries.map { it.destination.route }
        assertEquals(routes.size, routes.toSet().size)
    }
}
