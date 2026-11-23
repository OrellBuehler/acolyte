package ch.orellbuehler.acolyte.image

object ArtPlaceholder {
    fun text(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.length <= 4 && trimmed.all { it.isDigit() }) return trimmed
        val words = trimmed.split(Regex("\\s+")).filter { word -> word.any { it.isLetterOrDigit() } }
        return words
            .take(2)
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() }
            .joinToString("")
    }

    fun hue(name: String): Int {
        var hash = 0x811c9dc5.toInt()
        for (char in name.lowercase()) {
            hash = hash xor char.code
            hash *= 0x01000193
        }
        return ((hash.toLong() and 0xffffffffL) % 360).toInt()
    }
}
