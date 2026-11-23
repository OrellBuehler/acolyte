package ch.orellbuehler.acolyte

import kotlin.test.Test
import kotlin.test.assertEquals

class AppNameTest {
    @Test
    fun appNameIsAcolyte() {
        assertEquals("Acolyte", appName())
    }
}
