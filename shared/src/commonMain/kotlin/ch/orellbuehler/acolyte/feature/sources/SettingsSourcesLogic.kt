package ch.orellbuehler.acolyte.feature.sources

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.SettingsSourceCall
import ch.orellbuehler.acolyte.api.model.SettingsSourceCheckRow
import ch.orellbuehler.acolyte.api.model.SettingsSourceDay
import ch.orellbuehler.acolyte.api.model.SettingsSourceHealth
import ch.orellbuehler.acolyte.api.model.SettingsSourceTest
import ch.orellbuehler.acolyte.api.model.SourceRef
import ch.orellbuehler.acolyte.feature.Tone
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class SettingsSourceWord {
    SLOWED,
    DOWN,
    BLOCKED,
    KEY_REFUSED,
    WORKING,
    UNREACHABLE,
    KEY_TURNED_DOWN,
    PAUSED,
    OTHER,
}

enum class SettingsSourceOp {
    CAPS,
    SEARCH,
    FEED,
    TEST,
    OTHER,
}

enum class SettingsSourceErrorKind {
    DOWN,
    RATE_LIMITED,
    LOGIN_REFUSED,
    BROKEN_DEFINITION,
    BLOCKED,
    OTHER,
}

enum class SettingsSourceCause {
    NAME_NOT_FOUND,
    REFUSED,
    TIMEOUT,
    CERTIFICATE,
    USE_HTTP,
    USE_HTTPS,
    AUTH,
    WRONG_PATH,
    WRONG_APP,
    CATEGORY_MISSING,
    LOCAL_NETWORK,
}

enum class SettingsSourceCheckWord {
    CONNECTED,
    NOT_CONNECTED,
    PAUSED,
}

data class SettingsSourceOutcome(
    val ok: Boolean,
    val text: String,
    val ms: Int,
    val items: Int,
    val cause: SettingsSourceCause?,
    val botCheck: Boolean,
    val error: ApiError?,
)

data class SettingsSourceBar(
    val day: String,
    val calls: Int,
    val failures: Int,
    val answered: Int,
    val grabs: Int,
    val avgMs: Int,
    val callsPercent: Int,
    val failedPercent: Int,
    val grabsPercent: Int,
    val timePercent: Int,
)

data class SettingsSourceCallRow(
    val at: String,
    val op: SettingsSourceOp,
    val rawOp: String,
    val ok: Boolean,
    val ms: Int,
    val errorKind: SettingsSourceErrorKind,
    val rawError: String,
)

data class SettingsSourceCheckLine(
    val id: Long,
    val name: String,
    val word: SettingsSourceCheckWord,
    val tone: Tone,
    val count: Int,
    val ms: Int,
    val detail: String,
    val link: Boolean,
)

object SettingsSourcesLogic {
    private val ADDRESS = Regex("^https?://([^/?#\\s@]+)([/?#].*)?$", RegexOption.IGNORE_CASE)
    private val PROXY = Regex("^(http|https|socks5)://(?:[^@/\\s]+@)?(\\[[^\\]]+]|[^:/@\\s]+):(\\d{1,5})/?$", RegexOption.IGNORE_CASE)
    private val ORIGIN = Regex("^(https?)://(\\[[^\\]]+]|[^:/\\s]+)(?::(\\d+))?(/.*)?$", RegexOption.IGNORE_CASE)
    private val TAILSCALE = Regex("^100\\.(6[4-9]|[7-9]\\d|1[01]\\d|12[0-7])\\..*")
    private const val BOT_CHECK = "site's bot check"
    private const val MAX_PORT = 65535
    private const val MIN_BAR = 4
    private const val HUNDRED = 100.0
    private const val SECOND = 1000
    private const val TENTH = 100.0
    private const val FRACTION = 10000.0
    private const val SAMPLE_LIMIT = 8

    fun isWebAddress(text: String): Boolean = ADDRESS.matches(text)

    fun isProxy(text: String): Boolean {
        if (text.isEmpty()) return true
        val port =
            PROXY
                .matchEntire(text)
                ?.groupValues
                ?.get(3)
                ?.toIntOrNull() ?: return false
        return port in 1..MAX_PORT
    }

    fun number(text: String): Double? {
        val trimmed = text.trim().replace(',', '.')
        if (trimmed.isEmpty()) return 0.0
        return trimmed.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    fun formatNumber(value: Double): String {
        val scaled = (value * FRACTION).roundToLong()
        val whole = scaled / FRACTION.toLong()
        val fraction = (scaled % FRACTION.toLong()).toString().padStart(4, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
    }

    fun durationText(
        ms: Int,
        separator: Char,
    ): String {
        if (ms <= 0) return "—"
        if (ms < SECOND) return "$ms ms"
        val tenths = (ms / TENTH).roundToInt()
        return "${tenths / 10}$separator${tenths % 10} s"
    }

    fun barPercent(
        n: Int,
        most: Int,
    ): Int = if (most <= 0 || n <= 0) 0 else max(MIN_BAR, (n * HUNDRED / most).roundToInt())

    fun word(h: SettingsSourceHealth): SettingsSourceWord =
        when (h.reason) {
            "slowed" -> SettingsSourceWord.SLOWED
            "down" -> SettingsSourceWord.DOWN
            "blocked" -> SettingsSourceWord.BLOCKED
            "key" -> SettingsSourceWord.KEY_REFUSED
            else ->
                when (h.state) {
                    "working" -> SettingsSourceWord.WORKING
                    "unreachable" -> SettingsSourceWord.UNREACHABLE
                    "auth_failed" -> SettingsSourceWord.KEY_TURNED_DOWN
                    "disabled" -> SettingsSourceWord.PAUSED
                    else -> SettingsSourceWord.OTHER
                }
        }

    fun tone(h: SettingsSourceHealth): Tone =
        when (h.reason) {
            "slowed" -> Tone.WAIT
            "down", "blocked", "key" -> Tone.FAIL
            else ->
                when (h.state) {
                    "working" -> Tone.OK
                    "disabled" -> Tone.IDLE
                    else -> Tone.FAIL
                }
        }

    fun rawWord(h: SettingsSourceHealth): String = h.state.replace('_', ' ')

    fun cause(wire: String): SettingsSourceCause? =
        when (wire) {
            "name_not_found" -> SettingsSourceCause.NAME_NOT_FOUND
            "refused" -> SettingsSourceCause.REFUSED
            "timeout" -> SettingsSourceCause.TIMEOUT
            "certificate" -> SettingsSourceCause.CERTIFICATE
            "use_http" -> SettingsSourceCause.USE_HTTP
            "use_https" -> SettingsSourceCause.USE_HTTPS
            "auth" -> SettingsSourceCause.AUTH
            "wrong_path" -> SettingsSourceCause.WRONG_PATH
            "wrong_app" -> SettingsSourceCause.WRONG_APP
            "category_missing" -> SettingsSourceCause.CATEGORY_MISSING
            "local_network" -> SettingsSourceCause.LOCAL_NETWORK
            else -> null
        }

    fun op(wire: String): SettingsSourceOp =
        when (wire) {
            "caps" -> SettingsSourceOp.CAPS
            "search" -> SettingsSourceOp.SEARCH
            "feed" -> SettingsSourceOp.FEED
            "test" -> SettingsSourceOp.TEST
            else -> SettingsSourceOp.OTHER
        }

    fun errorKind(wire: String): SettingsSourceErrorKind =
        when (wire) {
            "down" -> SettingsSourceErrorKind.DOWN
            "rate_limited" -> SettingsSourceErrorKind.RATE_LIMITED
            "login_refused" -> SettingsSourceErrorKind.LOGIN_REFUSED
            "broken_definition" -> SettingsSourceErrorKind.BROKEN_DEFINITION
            "blocked" -> SettingsSourceErrorKind.BLOCKED
            else -> SettingsSourceErrorKind.OTHER
        }

    fun formatLabel(s: SourceRef): String =
        when (s.format) {
            "rss" -> "RSS"
            "site" -> SettingsSourceSites.byId(s.site)?.name ?: s.site.ifEmpty { "Site" }
            else -> s.format.replaceFirstChar { it.uppercase() }
        }

    fun outcome(r: SettingsSourceTest): SettingsSourceOutcome =
        SettingsSourceOutcome(
            ok = r.ok,
            text = r.text,
            ms = r.ms,
            items = r.items,
            cause = cause(r.cause.ifEmpty { r.health.cause }),
            botCheck = r.text.contains(BOT_CHECK),
            error = null,
        )

    fun failedOutcome(error: ApiError): SettingsSourceOutcome =
        SettingsSourceOutcome(false, error.message, 0, 0, null, error.message.contains(BOT_CHECK), error)

    fun showsServerText(error: ApiError): Boolean =
        error.message.isNotBlank() && error.code in setOf(ApiErrorCode.BAD_REQUEST, ApiErrorCode.CONFLICT, ApiErrorCode.SERVER)

    fun bars(days: List<SettingsSourceDay>): List<SettingsSourceBar> {
        val most = days.maxOfOrNull { it.calls } ?: 0
        val slowest = days.maxOfOrNull { it.avgMs } ?: 0
        return days.map {
            SettingsSourceBar(
                day = it.day,
                calls = it.calls,
                failures = it.failures,
                answered = max(0, it.calls - it.failures),
                grabs = it.grabs,
                avgMs = it.avgMs,
                callsPercent = barPercent(it.calls, most),
                failedPercent = barPercent(it.failures, most),
                grabsPercent = barPercent(it.grabs, most),
                timePercent = barPercent(it.avgMs, slowest),
            )
        }
    }

    fun callRows(calls: List<SettingsSourceCall>): List<SettingsSourceCallRow> =
        calls.map {
            SettingsSourceCallRow(it.at, op(it.op), it.op, it.ok, it.ms, errorKind(it.errorKind), it.errorKind.replace('_', ' '))
        }

    fun checkLine(r: SettingsSourceCheckRow): SettingsSourceCheckLine {
        val word =
            when {
                r.paused -> SettingsSourceCheckWord.PAUSED
                r.answered -> SettingsSourceCheckWord.CONNECTED
                else -> SettingsSourceCheckWord.NOT_CONNECTED
            }
        val tone =
            when {
                r.paused -> tone(r.health)
                r.answered -> Tone.OK
                else -> Tone.FAIL
            }
        val detail = if (r.paused) r.health.text else r.error
        return SettingsSourceCheckLine(r.id, r.name, word, tone, r.count, r.ms, detail, !r.answered)
    }

    fun checkSample(kind: String): String =
        when (kind) {
            "show" -> "Breaking Bad"
            "music" -> "Daft Punk"
            else -> "The Matrix"
        }

    fun titleAfterKind(
        old: String,
        title: String,
        new: String,
    ): String = if (title == checkSample(old)) checkSample(new) else title

    fun version(text: String): String {
        val token = text.split(' ').firstOrNull { Regex("^v?\\d+(\\.\\d+)+$").matches(it) } ?: return ""
        val numbers = token.removePrefix("v").split('.').take(3)
        return "v" + numbers.joinToString(".")
    }

    fun siteSample(titles: List<String>): List<String> = titles.take(SAMPLE_LIMIT)

    fun callbackAddress(
        serverUrl: String,
        homeIp: String,
        saved: String,
    ): String {
        if (saved.isNotBlank()) return saved
        val m = ORIGIN.matchEntire(serverUrl.trim().trimEnd('/')) ?: return ""
        val scheme = m.groupValues[1].lowercase()
        val host = m.groupValues[2]
        val port = m.groupValues[3]
        val path = m.groupValues[4]
        val local = host == "localhost" || host.startsWith("127.") || host == "[::1]"
        val odd = local || TAILSCALE.matches(host) || host.endsWith(".ts.net")
        return when {
            odd && homeIp.isNotEmpty() -> "$scheme://$homeIp${if (port.isNotEmpty()) ":$port" else ""}$path"
            local -> ""
            else -> "$scheme://$host${if (port.isNotEmpty()) ":$port" else ""}$path"
        }
    }
}
