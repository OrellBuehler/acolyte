package ch.orellbuehler.acolyte

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsTest {
    private fun strings(folder: String): Map<String, String> {
        val files = File("src/androidMain/res/$folder").listFiles { file -> file.name.matches(Regex("strings.*\\.xml")) }.orEmpty()
        return files
            .sortedBy { it.name }
            .flatMap { file ->
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                val nodes = document.getElementsByTagName("string")
                (0 until nodes.length)
                    .map { nodes.item(it) as Element }
                    .filter { it.getAttribute("translatable") != "false" }
                    .map { it.getAttribute("name") to it.textContent }
            }.toMap()
    }

    private val placeholder = Regex("%\\d\\$[sd]")

    @Test
    fun germanHasEveryKey() {
        val english = strings("values")
        val german = strings("values-de")
        assertEquals(english.keys.sorted(), german.keys.sorted())
    }

    @Test
    fun placeholdersMatch() {
        val german = strings("values-de")
        strings("values").forEach { (key, value) ->
            assertEquals(
                placeholder.findAll(value).map { it.value }.toSet(),
                placeholder.findAll(german.getValue(key)).map { it.value }.toSet(),
                key,
            )
        }
    }

    @Test
    fun germanUsesSwissSpelling() {
        val offenders = strings("values-de").filterValues { it.contains('ß') }.keys
        assertTrue(offenders.isEmpty(), "eszett in $offenders")
    }
}
