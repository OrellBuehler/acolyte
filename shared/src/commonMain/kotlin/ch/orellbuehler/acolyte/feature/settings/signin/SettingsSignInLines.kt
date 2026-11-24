package ch.orellbuehler.acolyte.feature.settings.signin

enum class SettingsSignInFixKind {
    PART,
    MISSING,
    LETTER,
    EXTRA,
    BITS,
    WIDE,
    BRIDGE,
    NOT_ADDRESS,
}

data class SettingsSignInLineProblem(
    val line: Int,
    val text: String,
    val kind: SettingsSignInFixKind,
    val fix: String = "",
    val letter: String = "",
)

internal data class SettingsSignInFix(
    val kind: SettingsSignInFixKind,
    val fix: String = "",
    val letter: String = "",
)

internal object SettingsSignInLines {
    private val digitsAndDots = Regex("^[\\d.]+$")
    private val ipv6Chars = Regex("^[0-9a-f:.]+$", RegexOption.IGNORE_CASE)
    private val digits = Regex("^\\d+$")
    private val prefixDigits = Regex("^\\d{1,2}$")
    private val ipv4Part = Regex("^\\d{1,3}$")
    private val gateway = Regex("^172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.1$")
    private val separators = Regex("[\\s,]+")
    private val lookalikeOne = Regex("[lI|]")
    private val lookalikeZero = Regex("[oO]")
    private val lookalike = Regex("[lI|oO]")
    private const val DOCKER_BRIDGE = "172.17.0.0"
    private const val PODMAN_GATEWAY = "10.88.0.1"
    private const val OCTET_LIMIT = 255
    private const val MIN_V4_BITS = 8
    private const val MIN_V6_BITS = 7
    private const val MAX_V6_BITS = 128
    private const val MAX_V4_BITS = 32
    private const val BRIDGE_BITS = 16
    private const val PARTS = 4

    fun lines(text: String): List<String> = text.split(separators).filter { it.isNotEmpty() }

    fun ip4(text: String): Long? {
        val parts = text.split('.')
        if (parts.size != PARTS || parts.any { !ipv4Part.matches(it) || it.toInt() > OCTET_LIMIT }) return null
        return parts.fold(0L) { n, part -> n * 256 + part.toInt() }
    }

    private fun inNet(
        ip: Long,
        base: Long,
        bits: Int,
    ): Boolean {
        val size = 1L shl (MAX_V4_BITS - bits)
        return ip / size == base / size
    }

    private fun partValue(part: String): Int {
        val trimmed = part.trimStart('0')
        return if (trimmed.length > 3) OCTET_LIMIT + 1 else trimmed.ifEmpty { "0" }.toInt()
    }

    private fun bridge(
        base: Long,
        bits: Int,
        text: String,
        gateways: List<String>,
    ): Boolean {
        if (text in gateways) return true
        val docker = ip4(DOCKER_BRIDGE) ?: return false
        return bits >= BRIDGE_BITS && inNet(base, docker, BRIDGE_BITS)
    }

    fun check(
        raw: String,
        proxy: Boolean,
        gateways: List<String> = emptyList(),
    ): SettingsSignInFix? {
        val text = raw.trim()
        val pieces = text.split('/')
        val addr = pieces[0]
        val bitsText = pieces.getOrNull(1)
        if (pieces.size > 2) return SettingsSignInFix(SettingsSignInFixKind.NOT_ADDRESS)
        if (addr.contains(':')) return checkV6(addr, bitsText)
        val tail = if (bitsText == null) "" else "/$bitsText"
        val swapped = addr.replace(lookalikeOne, "1").replace(lookalikeZero, "0")
        if (swapped != addr && digitsAndDots.matches(swapped)) {
            val letter = lookalike.find(addr)?.value.orEmpty()
            return SettingsSignInFix(SettingsSignInFixKind.LETTER, swapped + tail, letter)
        }
        if (!digitsAndDots.matches(addr)) return SettingsSignInFix(SettingsSignInFixKind.NOT_ADDRESS)
        val parts = addr.split('.')
        if (parts.any { it.isEmpty() } && parts.size > PARTS) return SettingsSignInFix(SettingsSignInFixKind.NOT_ADDRESS)
        if (parts.size > PARTS) return SettingsSignInFix(SettingsSignInFixKind.EXTRA)
        if (parts.any { it.isNotEmpty() && partValue(it) > OCTET_LIMIT }) return SettingsSignInFix(SettingsSignInFixKind.PART)
        if (parts.size < PARTS || parts.any { it.isEmpty() }) {
            val full = parts.filter { it.isNotEmpty() }.toMutableList()
            while (full.size < PARTS) full += "0"
            return SettingsSignInFix(SettingsSignInFixKind.MISSING, full.joinToString(".") + tail)
        }
        if (bitsText != null && (!prefixDigits.matches(bitsText) || bitsText.toInt() > MAX_V4_BITS)) {
            return SettingsSignInFix(SettingsSignInFixKind.BITS)
        }
        val bits = bitsText?.toInt() ?: MAX_V4_BITS
        if (bits < MIN_V4_BITS) return SettingsSignInFix(SettingsSignInFixKind.WIDE)
        val base = ip4(addr)
        if (!proxy && base != null && bridge(base, bits, text, gateways)) return SettingsSignInFix(SettingsSignInFixKind.BRIDGE)
        return null
    }

    private fun checkV6(
        addr: String,
        bitsText: String?,
    ): SettingsSignInFix? {
        val notAddress = SettingsSignInFix(SettingsSignInFixKind.NOT_ADDRESS)
        if (!ipv6Chars.matches(addr) || !digits.matches(bitsText ?: MAX_V6_BITS.toString())) return notAddress
        val bits = if (bitsText == null) MAX_V6_BITS else bitsText.toIntOrNull() ?: return notAddress
        if (bits > MAX_V6_BITS) return notAddress
        return if (bits < MIN_V6_BITS) SettingsSignInFix(SettingsSignInFixKind.WIDE) else null
    }

    fun problems(
        text: String,
        proxy: Boolean,
        gateways: List<String> = emptyList(),
    ): List<SettingsSignInLineProblem> =
        text.split('\n').mapIndexedNotNull { index, line ->
            if (line.isBlank()) return@mapIndexedNotNull null
            val fix = check(line, proxy, gateways) ?: return@mapIndexedNotNull null
            SettingsSignInLineProblem(index + 1, line.trim(), fix.kind, fix.fix, fix.letter)
        }

    fun covers(
        ip: String,
        list: List<String>,
    ): Boolean? {
        val n = ip4(ip) ?: return null
        return list.any { entry ->
            val pieces = entry.split('/')
            val base = ip4(pieces[0])
            val bits = if (pieces.size < 2) MAX_V4_BITS else pieces[1].toIntOrNull()
            base != null && bits != null && bits in 0..MAX_V4_BITS && inNet(n, base, bits)
        }
    }

    fun gatewayLike(ip: String): Boolean = gateway.matches(ip) || ip == PODMAN_GATEWAY
}
