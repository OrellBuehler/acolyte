package ch.orellbuehler.acolyte.feature.acquisition.downloaders

import ch.orellbuehler.acolyte.api.model.DownloaderInfo
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderBody
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderPath
import ch.orellbuehler.acolyte.api.model.SettingsDownloaderSeeding
import ch.orellbuehler.acolyte.feature.Tone
import kotlin.math.roundToInt

enum class SettingsDownloaderSignIn {
    QBITTORRENT,
    USER_AND_PASSWORD,
    PASSWORD,
    SECRET_TOKEN,
    API_KEY,
}

enum class SettingsDownloaderSeedingFields {
    NONE,
    REMOVE,
    GOALS,
}

enum class SettingsDownloaderKind(
    val wire: String,
    val appName: String,
    val usenet: Boolean,
    val badge: String,
    val placeholder: String,
    val signIn: SettingsDownloaderSignIn,
    val seeding: SettingsDownloaderSeedingFields,
    val canMakeCategory: Boolean = false,
) {
    QBITTORRENT(
        "qbittorrent",
        "qBittorrent",
        false,
        "qB",
        "http://192.168.1.10:8090",
        SettingsDownloaderSignIn.QBITTORRENT,
        SettingsDownloaderSeedingFields.GOALS,
        canMakeCategory = true,
    ),
    TRANSMISSION(
        "transmission",
        "Transmission",
        false,
        "Tr",
        "http://192.168.1.10:9091",
        SettingsDownloaderSignIn.USER_AND_PASSWORD,
        SettingsDownloaderSeedingFields.REMOVE,
    ),
    DELUGE(
        "deluge",
        "Deluge",
        false,
        "De",
        "http://192.168.1.10:8112",
        SettingsDownloaderSignIn.PASSWORD,
        SettingsDownloaderSeedingFields.REMOVE,
    ),
    RTORRENT(
        "rtorrent",
        "rTorrent",
        false,
        "rT",
        "http://192.168.1.10:8000/RPC2",
        SettingsDownloaderSignIn.USER_AND_PASSWORD,
        SettingsDownloaderSeedingFields.GOALS,
    ),
    ARIA2(
        "aria2",
        "aria2",
        false,
        "a2",
        "http://192.168.1.10:6800/jsonrpc",
        SettingsDownloaderSignIn.SECRET_TOKEN,
        SettingsDownloaderSeedingFields.REMOVE,
    ),
    SABNZBD(
        "sabnzbd",
        "SABnzbd",
        true,
        "SB",
        "http://192.168.1.10:8080",
        SettingsDownloaderSignIn.API_KEY,
        SettingsDownloaderSeedingFields.NONE,
        canMakeCategory = true,
    ),
    NZBGET(
        "nzbget",
        "NZBGet",
        true,
        "NG",
        "http://192.168.1.10:6789",
        SettingsDownloaderSignIn.USER_AND_PASSWORD,
        SettingsDownloaderSeedingFields.NONE,
    ),
    ;

    companion object {
        fun from(wire: String): SettingsDownloaderKind? = entries.firstOrNull { it.wire == wire }

        val torrents: List<SettingsDownloaderKind> get() = entries.filter { !it.usenet }

        val usenetKinds: List<SettingsDownloaderKind> get() = entries.filter { it.usenet }
    }
}

enum class SettingsDownloaderHealthState(
    val wire: String,
    val tone: Tone,
) {
    WORKING("working", Tone.OK),
    PAUSED("paused", Tone.IDLE),
    RESTING("resting", Tone.WAIT),
    NOT_ANSWERING("not_answering", Tone.FAIL),
    SIGN_IN_REFUSED("sign_in_refused", Tone.FAIL),
    UNKNOWN("", Tone.FAIL),
    ;

    companion object {
        fun from(wire: String): SettingsDownloaderHealthState = entries.firstOrNull { it.wire == wire && wire.isNotEmpty() } ?: UNKNOWN
    }
}

enum class SettingsDownloaderIssue {
    NAME,
    URL,
    CATEGORY,
    CATEGORY_LABEL,
    CATEGORY_FOLDER,
    KIND_CATEGORY,
    AFTER_IMPORT,
    AFTER_IMPORT_SAME,
    PRIORITY,
    PATH,
    RATIO,
    HOURS,
    SECRET,
    USERNAME,
}

enum class SettingsDownloaderKindField {
    MOVIES,
    SHOWS,
    MUSIC,
}

const val MASKED_SECRET: String = "••••••••"

private const val NAME_MAX = 80
private const val CATEGORY_MAX = 64
private const val PRIORITY_MAX = 50
private const val RATIO_MAX = 1000.0
private const val MINUTES_MAX = 525_600
private const val MINUTES_PER_HOUR = 60
private const val ARIA_BAD = ":*?\"<>|"
private const val DELUGE_OK = "abcdefghijklmnopqrstuvwxyz0123456789_.-"

fun formatNumber(value: Double): String =
    when {
        value.isNaN() || value.isInfinite() -> ""
        value == value.toLong().toDouble() -> value.toLong().toString()
        else -> value.toString()
    }

fun parseNumber(text: String): Double? =
    text
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() }

fun looksLikeAddress(raw: String): Boolean = Regex("^(?i)https?://[^/?#\\s]+").containsMatchIn(raw.trim())

fun validAddress(raw: String): Boolean {
    val text = raw.trim()
    val match = Regex("^(?i)https?://([^/?#]*)([^?#]*)(#.*)?$").matchEntire(text) ?: return false
    if (text.substringBefore('#').contains('?')) return false
    val authority = match.groupValues[1]
    if (authority.contains('@') || authority.any { it.isWhitespace() }) return false
    val host = if (authority.startsWith("[")) authority.substringBefore(']').removePrefix("[") else authority.substringBefore(':')
    return host.isNotEmpty()
}

fun isAbsolutePath(raw: String): Boolean {
    val p = raw.trim()
    return p.startsWith("/") || (p.length >= 3 && p[1] == ':' && (p[2] == '\\' || p[2] == '/'))
}

fun samePathPair(pair: SettingsDownloaderPath): Boolean {
    fun norm(raw: String): String {
        var x = raw.trim()
        if (x.contains('\\') || Regex("^[a-zA-Z]:").containsMatchIn(x)) x = x.replace('\\', '/').lowercase()
        x = x.replace(Regex("/+"), "/")
        if (x.length > 1 && x.endsWith("/")) x = x.dropLast(1)
        return x
    }
    val theirs = norm(pair.theirs)
    return theirs.isNotEmpty() && theirs == norm(pair.ours)
}

fun categoryIssue(
    raw: String,
    kind: String,
): SettingsDownloaderIssue? {
    val c = raw.trim()
    if (c.isEmpty()) return null
    if (c.length > CATEGORY_MAX || c.any { it == '/' || it == '\\' }) return SettingsDownloaderIssue.CATEGORY
    if (kind == "deluge" && c.any { it !in DELUGE_OK }) return SettingsDownloaderIssue.CATEGORY_LABEL
    if (kind == "aria2" && (c == "." || c == ".." || c.any { it in ARIA_BAD || it.code < 0x20 || it.code == 0x7f })) {
        return SettingsDownloaderIssue.CATEGORY_FOLDER
    }
    return null
}

private fun goal(text: String): Double? = parseNumber(text)?.takeIf { it > 0 }

data class SettingsDownloaderForm(
    val id: Long = 0,
    val kind: String = "",
    val picked: Boolean = false,
    val name: String = "",
    val url: String = "http://",
    val username: String = "",
    val secret: String = "",
    val login: Boolean = false,
    val category: String = "holyarrs",
    val priority: String = "1",
    val categoryMovies: String = "",
    val categoryShows: String = "",
    val categoryMusic: String = "",
    val tags: List<String> = emptyList(),
    val pathMap: List<SettingsDownloaderPath> = emptyList(),
    val allowLocalNetwork: Boolean = true,
    val writable: Boolean = true,
    val writableLocked: Boolean = false,
    val seedRemove: Boolean = false,
    val seedRatio: String = "",
    val seedHours: String = "",
    val seedDeleteFiles: Boolean = false,
    val categoryAfterImport: String = "",
    val ownOnly: Boolean = false,
    val clearFinished: Boolean = true,
    val original: DownloaderInfo? = null,
) {
    val isNew: Boolean get() = id == 0L

    val kindInfo: SettingsDownloaderKind? get() = SettingsDownloaderKind.from(kind)

    val usenet: Boolean get() = kindInfo?.usenet == true

    val appName: String get() = kindInfo?.appName ?: kind

    val signIn: SettingsDownloaderSignIn? get() = kindInfo?.signIn

    val hasUsername: Boolean
        get() =
            when (signIn) {
                SettingsDownloaderSignIn.USER_AND_PASSWORD -> true
                SettingsDownloaderSignIn.QBITTORRENT -> login
                else -> false
            }

    val seedingFields: SettingsDownloaderSeedingFields get() = kindInfo?.seeding ?: SettingsDownloaderSeedingFields.NONE

    val showFields: Boolean get() = !isNew || picked

    val usesKindCategories: Boolean get() = kindInfo != null

    val keyPageUrl: String get() = url.trim().trimEnd('/') + "/config/general/"

    val canFetchKey: Boolean get() = kind == SettingsDownloaderKind.SABNZBD.wire && looksLikeAddress(url)

    val secretKept: Boolean get() = secret == MASKED_SECRET

    val issues: List<SettingsDownloaderIssue> get() = computeIssues()

    val canSave: Boolean get() = showFields && name.isNotBlank() && url.isNotBlank() && issues.isEmpty()

    val changed: Boolean
        get() {
            val saved = original ?: return true
            return body() != fromInfo(saved).body()
        }

    fun kindCategory(field: SettingsDownloaderKindField): String =
        when (field) {
            SettingsDownloaderKindField.MOVIES -> categoryMovies
            SettingsDownloaderKindField.SHOWS -> categoryShows
            SettingsDownloaderKindField.MUSIC -> categoryMusic
        }

    fun hasPicks(): Boolean = categoryMovies.isEmpty() && categoryShows.isEmpty() && categoryMusic.isEmpty()

    fun testBody(): SettingsDownloaderBody? = if (changed) body() else null

    fun body(): SettingsDownloaderBody {
        val info = kindInfo
        val sendsUsername =
            info != null && info.signIn != SettingsDownloaderSignIn.PASSWORD && info.signIn != SettingsDownloaderSignIn.SECRET_TOKEN
        val username = if (sendsUsername && hasUsername) this.username.trim() else ""
        return SettingsDownloaderBody(
            name = name.trim(),
            kind = kind,
            url = url.trim(),
            username = username,
            secret = secret,
            category = category.trim(),
            categoryMovies = categoryMovies.trim(),
            categoryShows = categoryShows.trim(),
            categoryMusic = categoryMusic.trim(),
            priority = priority.trim().toIntOrNull()?.takeIf { it != 0 } ?: 1,
            pathMap =
                pathMap
                    .map { SettingsDownloaderPath(it.theirs.trim(), it.ours.trim(), it.link) }
                    .filter { it.theirs.isNotEmpty() || it.ours.isNotEmpty() },
            allowLocalNetwork = allowLocalNetwork,
            writable = writable,
            tags = tags,
            categoryAfterImport = if (kind == SettingsDownloaderKind.QBITTORRENT.wire) categoryAfterImport.trim() else null,
            clearFinished = if (usenet) clearFinished else null,
            ownOnly = if (usenet) null else ownOnly,
            afterSeeding = if (seedingFields == SettingsDownloaderSeedingFields.NONE) null else seeding(),
        )
    }

    fun seeding(): SettingsDownloaderSeeding {
        val goals = seedingFields == SettingsDownloaderSeedingFields.GOALS
        val hours = if (goals) goal(seedHours) else null
        return SettingsDownloaderSeeding(
            remove = seedRemove,
            ratio = if (goals) goal(seedRatio) else null,
            minutes = hours?.let { maxOf(1, (it * MINUTES_PER_HOUR).roundToInt()) },
            deleteFiles = seedRemove && seedDeleteFiles,
        )
    }

    fun problem(issue: SettingsDownloaderIssue): Boolean = issue in issues

    private fun computeIssues(): List<SettingsDownloaderIssue> {
        if (!showFields) return emptyList()
        val found = mutableListOf<SettingsDownloaderIssue>()
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > NAME_MAX) found += SettingsDownloaderIssue.NAME
        if (!validAddress(url)) found += SettingsDownloaderIssue.URL
        val main = category.trim()
        if (main.isEmpty()) {
            found += SettingsDownloaderIssue.CATEGORY
        } else {
            categoryIssue(main, kind)?.let { found += it }
        }
        val perKind = listOf(categoryMovies, categoryShows, categoryMusic).mapNotNull { categoryIssue(it, kind) }
        if (perKind.isNotEmpty()) found += SettingsDownloaderIssue.KIND_CATEGORY
        if (kind == SettingsDownloaderKind.QBITTORRENT.wire) {
            val after = categoryAfterImport.trim()
            if (after.isNotEmpty()) {
                if (categoryIssue(after, kind) != null) {
                    found += SettingsDownloaderIssue.AFTER_IMPORT
                } else if (listOf(main, categoryMovies.trim(), categoryShows.trim(), categoryMusic.trim()).any { it.equals(after, true) }) {
                    found += SettingsDownloaderIssue.AFTER_IMPORT_SAME
                }
            }
        }
        val level = priority.trim().toIntOrNull()
        if (priority.isNotBlank() && (level == null || level < 1 || level > PRIORITY_MAX)) found += SettingsDownloaderIssue.PRIORITY
        val pairs = pathMap.filter { it.theirs.isNotBlank() || it.ours.isNotBlank() }
        if (pairs.any { !isAbsolutePath(it.theirs) || !isAbsolutePath(it.ours) }) found += SettingsDownloaderIssue.PATH
        if (seedingFields == SettingsDownloaderSeedingFields.GOALS) {
            if (seedRatio.isNotBlank() &&
                (parseNumber(seedRatio)?.let { it <= 0 || it > RATIO_MAX } != false)
            ) {
                found += SettingsDownloaderIssue.RATIO
            }
            val hours = parseNumber(seedHours)
            if (seedHours.isNotBlank() && (hours == null || hours <= 0 || hours * MINUTES_PER_HOUR > MINUTES_MAX)) {
                found += SettingsDownloaderIssue.HOURS
            }
        }
        if (kind == SettingsDownloaderKind.QBITTORRENT.wire) {
            if (login && username.isBlank()) found += SettingsDownloaderIssue.USERNAME
            val waySwitched = original?.let { login != it.username.isNotEmpty() } ?: false
            if ((isNew || waySwitched) && (secret.isBlank() || secretKept)) found += SettingsDownloaderIssue.SECRET
        }
        if (kind == SettingsDownloaderKind.SABNZBD.wire && isNew && secret.isBlank()) found += SettingsDownloaderIssue.SECRET
        return found
    }

    companion object {
        fun blank(): SettingsDownloaderForm = SettingsDownloaderForm()

        fun starting(kind: SettingsDownloaderKind): SettingsDownloaderForm =
            SettingsDownloaderForm(
                kind = kind.wire,
                picked = true,
                name = kind.appName,
                category = if (kind == SettingsDownloaderKind.SABNZBD) "movies" else "holyarrs",
            )

        fun fromInfo(info: DownloaderInfo): SettingsDownloaderForm {
            val minutes = info.afterSeeding.minutes
            return SettingsDownloaderForm(
                id = info.id,
                kind = info.kind,
                picked = true,
                name = info.name,
                url = info.url,
                username = info.username,
                secret = info.secret,
                login = info.username.isNotEmpty(),
                category = info.category,
                priority = info.priority.toString(),
                categoryMovies = info.categoryMovies,
                categoryShows = info.categoryShows,
                categoryMusic = info.categoryMusic,
                tags = info.tags,
                pathMap = info.pathMap,
                allowLocalNetwork = info.allowLocalNetwork,
                writable = info.writable,
                writableLocked = info.writableLocked,
                seedRemove = info.afterSeeding.remove,
                seedRatio =
                    info.afterSeeding.ratio
                        ?.let { formatNumber(it) }
                        .orEmpty(),
                seedHours =
                    if (minutes != null && minutes > 0) {
                        formatNumber((minutes.toDouble() / MINUTES_PER_HOUR * 100).roundToInt() / 100.0)
                    } else {
                        ""
                    },
                seedDeleteFiles = info.afterSeeding.deleteFiles,
                categoryAfterImport = info.categoryAfterImport,
                ownOnly = info.ownOnly,
                clearFinished = info.clearFinished,
                original = info,
            )
        }

        fun picking(
            current: SettingsDownloaderForm,
            kind: SettingsDownloaderKind,
        ): SettingsDownloaderForm {
            val fresh = starting(kind)
            return fresh.copy(
                url = if (current.url != "http://") current.url else fresh.url,
                allowLocalNetwork = current.allowLocalNetwork,
                writable = current.writable,
            )
        }
    }
}
