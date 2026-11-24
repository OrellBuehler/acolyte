package ch.orellbuehler.acolyte.feature.settings.mediaservers

import ch.orellbuehler.acolyte.api.ApiError

internal object SettingsMediaServersLogic {
    const val MAX_TRACK_LANGUAGES = 5
    const val OFF = "off"
    const val CONNECT_HELP = "https://docs.holyarrs.com/start/connect/#if-it-does-not-work"

    private val apps =
        mapOf(
            "qbittorrent" to "qBittorrent",
            "sabnzbd" to "SABnzbd",
            "transmission" to "Transmission",
            "nzbget" to "NZBGet",
            "deluge" to "Deluge",
            "rtorrent" to "rTorrent",
            "aria" to "aria2",
            "prowlarr" to "Prowlarr",
            "jackett" to "Jackett",
            "sonarr" to "Sonarr",
            "radarr" to "Radarr",
            "lidarr" to "Lidarr",
            "readarr" to "Readarr",
            "bazarr" to "Bazarr",
            "whisparr" to "Whisparr",
            "overseerr" to "Overseerr",
            "jellyseerr" to "Jellyseerr",
            "jellyfin" to "Jellyfin",
            "emby" to "Emby",
            "plex" to "Plex",
        )

    private val version = Regex("v?(\\d+(?:\\.\\d+)+)")
    private val words = Regex("[a-z]+")
    private val connectLine = Regex("^(\\S+) → (not found\\.|\\S+ → (?:refused|no answer) on port )")
    private val detailStatuses = setOf(400, 409, 422, 502, 503)
    private val preferredLanguages = listOf("en", "es", "fr", "de", "it", "pt", "ja", "ko", "zh")

    private fun blocksVersion(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "_./:@-"

    fun testVersion(text: String): String {
        var from = 0
        while (from <= text.length) {
            val match = version.find(text, from) ?: return ""
            val start = match.range.first
            if (start == 0 || !blocksVersion(text[start - 1])) {
                return "v" +
                    match.groupValues[1]
                        .split('.')
                        .take(3)
                        .joinToString(".")
            }
            from = start + 1
        }
        return ""
    }

    fun testApp(text: String): String {
        for (word in words.findAll(text.lowercase())) {
            apps[word.value]?.let { return it }
        }
        return ""
    }

    fun testLabel(
        text: String,
        app: String = "",
        extra: String = "",
        connected: String = "Connected",
    ): String {
        val head =
            listOf(app.ifEmpty { testApp(text) }, testVersion(text))
                .filter { it.isNotEmpty() }
                .joinToString(" ")
                .ifEmpty { connected }
        return listOf(head, extra).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    fun connectHelpUrl(text: String): String {
        val match = connectLine.find(text) ?: return ""
        val name = match.groupValues[1].lowercase()
        val row =
            when {
                match.groupValues[2] != "not found." -> "Not answering"
                name.endsWith(".local") -> "A .local name like"
                name.endsWith(".ts.net") -> "A Tailscale name"
                '.' in name -> "A Pi-hole or router name"
                else -> "A container name like"
            }
        return "$CONNECT_HELP:~:text=${encodeComponent(row)}"
    }

    private fun encodeComponent(text: String): String =
        buildString {
            for (byte in text.encodeToByteArray()) {
                val c = (byte.toInt() and 0xFF).toChar()
                if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "_.!~*'()") {
                    append(c)
                } else {
                    append('%')
                    append("0123456789ABCDEF"[(byte.toInt() and 0xFF) shr 4])
                    append("0123456789ABCDEF"[byte.toInt() and 0x0F])
                }
            }
        }

    fun plainMessage(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        val capital = trimmed.replaceFirstChar { it.uppercaseChar() }
        return if (capital.last() in ".!?)") capital else "$capital."
    }

    fun serverText(error: ApiError?): String = if (error != null && error.status > 0) plainMessage(error.message) else ""

    fun detail(error: ApiError?): String = if (error != null && error.status in detailStatuses) plainMessage(error.message) else ""

    fun plexAddresses(
        local: Boolean,
        url: String,
        secureUrl: String,
    ): List<String> =
        if (!local) {
            listOf(secureUrl.ifEmpty { url }).filter { it.isNotEmpty() }
        } else {
            listOf(secureUrl, url).filter { it.isNotEmpty() }.distinct()
        }

    fun plexRank(
        local: Boolean,
        relay: Boolean,
    ): Int =
        when {
            local -> 0
            relay -> 2
            else -> 1
        }

    fun nextLanguage(
        list: List<String>,
        known: List<String>,
        preferred: String,
    ): String? = (listOf(preferred) + preferredLanguages + known).firstOrNull { it in known && it !in list }

    fun replaceLanguage(
        list: List<String>,
        index: Int,
        code: String,
    ): List<String> =
        when {
            index !in list.indices -> list
            code == OFF -> list.take(index) + OFF
            else -> list.mapIndexed { i, old -> if (i == index) code else old }
        }

    fun moveLanguage(
        list: List<String>,
        index: Int,
        up: Boolean,
    ): List<String> {
        val target = if (up) index - 1 else index + 1
        if (index !in list.indices || target !in list.indices) return list
        val moved = list.toMutableList()
        moved[index] = list[target]
        moved[target] = list[index]
        return if (OFF in moved.dropLast(1)) list else moved
    }
}
