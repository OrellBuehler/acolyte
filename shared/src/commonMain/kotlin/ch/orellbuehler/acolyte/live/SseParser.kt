package ch.orellbuehler.acolyte.live

internal data class SseMessage(
    val id: String?,
    val event: String?,
    val data: String,
)

internal class SseParser {
    private val data = StringBuilder()
    private var hasData = false
    private var event: String? = null
    private var id: String? = null

    fun feed(line: String): SseMessage? {
        if (line.isEmpty()) return dispatch()
        if (line.startsWith(":")) return null
        val colon = line.indexOf(':')
        val field = if (colon == -1) line else line.substring(0, colon)
        val value =
            when {
                colon == -1 -> ""
                line.length > colon + 1 && line[colon + 1] == ' ' -> line.substring(colon + 2)
                else -> line.substring(colon + 1)
            }
        when (field) {
            "data" -> {
                if (hasData) data.append('\n')
                data.append(value)
                hasData = true
            }
            "event" -> event = value
            "id" -> if ('\u0000' !in value) id = value
        }
        return null
    }

    private fun dispatch(): SseMessage? {
        val message = if (hasData) SseMessage(id, event?.ifEmpty { null }, data.toString()) else null
        data.clear()
        hasData = false
        event = null
        id = null
        return message
    }
}
