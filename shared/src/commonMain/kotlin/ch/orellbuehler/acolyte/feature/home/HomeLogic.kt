package ch.orellbuehler.acolyte.feature.home

import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.HealthCheck
import ch.orellbuehler.acolyte.api.model.HomeAnswer
import ch.orellbuehler.acolyte.api.model.HomeCard
import ch.orellbuehler.acolyte.api.model.HomeDecision
import ch.orellbuehler.acolyte.api.model.HomeDownload
import ch.orellbuehler.acolyte.api.model.HomeHealthPart
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.epochDay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class GreetingTime {
    MORNING,
    AFTERNOON,
    EVENING,
}

enum class NewsKind {
    ASKED_ARRIVED_ANY,
    ASKED_ARRIVED_ONE,
    ASKED_ARRIVED_MANY,
    WAITING,
    WAITING_PAGE,
    NEW_TODAY_ANY,
    NEW_TODAY_ONE,
    NEW_TODAY_MANY,
    MINE_FINISHING,
    FINISHING,
}

data class HomeNews(
    val kind: NewsKind,
    val count: Int = 0,
    val title: String = "",
)

enum class DownloadStage {
    CHECKING,
    QUEUED,
    PAUSED,
    DOWNLOADING,
    STALLED,
    IMPORTING,
    DECIDE,
    FAILED,
    SEEDING,
    DONE,
}

internal data class LocalTime(
    val epochMillis: Long,
    val offsetSeconds: Int,
) {
    val localSeconds: Long get() = epochMillis / MILLIS + offsetSeconds
    val day: Int get() = Floor.floorDiv(localSeconds, DAY_SECONDS).toInt()
    val hour: Int get() = (Floor.floorMod(localSeconds, DAY_SECONDS) / HOUR_SECONDS).toInt()
    val weekday: Int get() = Floor.floorMod(day + THURSDAY, WEEK)

    fun dayOf(epochMillis: Long): Int = Floor.floorDiv(epochMillis / MILLIS + offsetSeconds, DAY_SECONDS).toInt()

    fun isoDay(): String = isoOf(day)

    companion object {
        const val MILLIS = 1000L
        const val DAY_SECONDS = 86_400L
        const val HOUR_SECONDS = 3_600L
        const val DAY_MILLIS = DAY_SECONDS * MILLIS
        private const val THURSDAY = 4
        private const val WEEK = 7
    }
}

@OptIn(ExperimentalTime::class)
internal fun systemMillis(): Long = Clock.System.now().toEpochMilliseconds()

internal fun localToday(offsetSeconds: Int): String = LocalTime(systemMillis(), offsetSeconds).isoDay()

internal object Floor {
    fun floorDiv(
        a: Long,
        b: Long,
    ): Long {
        val q = a / b
        return if ((a % b != 0L) && ((a < 0) != (b < 0))) q - 1 else q
    }

    fun floorMod(
        a: Long,
        b: Long,
    ): Long = a - floorDiv(a, b) * b

    fun floorMod(
        a: Int,
        b: Int,
    ): Int = (a % b + b) % b
}

internal fun isoOf(day: Int): String {
    val z = day + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + if (mp < 10) 3 else -9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

private val STAMP = Regex("^(\\d{4}-\\d{2}-\\d{2})(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?)?$")

internal fun epochMillisOf(text: String): Long? {
    val match = STAMP.matchEntire(text.trim()) ?: return null
    val day = epochDay(match.groupValues[1]) ?: return null
    val hour = match.groupValues[2].toIntOrNull() ?: 0
    val minute = match.groupValues[3].toIntOrNull() ?: 0
    val second = match.groupValues[4].toIntOrNull() ?: 0
    val zone = match.groupValues[5]
    val offset =
        if (zone.isEmpty() || zone == "Z") {
            0
        } else {
            val digits = zone.drop(1).replace(":", "")
            val sign = if (zone.startsWith("-")) -1 else 1
            sign * ((digits.take(2).toIntOrNull() ?: 0) * 3600 + (digits.drop(2).toIntOrNull() ?: 0) * 60)
        }
    val seconds = day * LocalTime.DAY_SECONDS + hour * 3600L + minute * 60L + second - offset
    return seconds * LocalTime.MILLIS
}

private val ID_PREFIX = Regex("^(preview|e2e|test|user|guest)[-_]", RegexOption.IGNORE_CASE)
private val HEX_ID = Regex("^[0-9a-f-]{12,}$", RegexOption.IGNORE_CASE)

internal fun looksLikeId(name: String): Boolean {
    val n = name.trim()
    if (n.isEmpty()) return true
    if (ID_PREFIX.containsMatchIn(n)) return true
    if (n.none { it.isLetter() }) return true
    return HEX_ID.matches(n)
}

internal fun capitalise(name: String): String {
    var n = name.trim()
    if (n.contains('@')) n = n.substringBefore('@')
    if (n.any { it.isUpperCase() }) return n
    return n
        .split(Regex("[._\\s-]+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
}

fun greetingName(
    displayName: String,
    accountName: String,
): String {
    val display = displayName.trim()
    if (display.isNotEmpty() && !looksLikeId(display)) return display.split(Regex("\\s+")).first()
    return if (looksLikeId(accountName)) "" else capitalise(accountName)
}

internal fun greetingTime(hour: Int): GreetingTime =
    when (hour) {
        in 5..11 -> GreetingTime.MORNING
        in 12..16 -> GreetingTime.AFTERNOON
        else -> GreetingTime.EVENING
    }

internal fun nearlyDone(
    state: String,
    progress: Double,
    eta: Long,
): Boolean = state == "downloading" && (progress >= 0.8 || (eta in 1..300))

internal fun hash(text: String): Long {
    var h = 0x811c9dc5L
    for (char in text) {
        h = h xor char.code.toLong()
        h = (h * 0x01000193L) and 0xffffffffL
    }
    return h
}

internal fun homeNews(
    home: HomeAnswer?,
    asks: List<Ask>,
    downloads: List<HomeDownload>,
    approver: Boolean,
    manager: Boolean,
    kid: Boolean,
    now: LocalTime,
    seed: String,
): HomeNews? {
    val nowMillis = now.epochMillis
    val arrived =
        asks
            .filter { it.mine && it.state == "available" }
            .filter { ask ->
                val at = epochMillisOf(ask.availableAt) ?: return@filter false
                nowMillis - at <= 3 * LocalTime.DAY_MILLIS && at <= nowMillis + 3_600_000L
            }.map { it.name }
            .distinct()
    val waiting = if (approver && !kid) asks.count { it.state == "asked" } else 0
    val fresh = linkedMapOf<Long, String>()
    for (card in home?.arrived.orEmpty()) {
        val at = epochMillisOf(card.at) ?: continue
        if (now.dayOf(at) == now.day) fresh[card.titleId] = card.name
    }
    val today = now.isoDay()
    for (card in home?.newSeasons.orEmpty() + home?.soon.orEmpty()) if (card.at.take(10) == today) fresh[card.titleId] = card.name
    val mine =
        home
            ?.askProgress
            ?.values
            .orEmpty()
            .count { nearlyDone(it.state, it.progress, it.eta) }
    val all = if (manager && !kid) downloads.count { nearlyDone(stateOf(it), it.progress, it.eta) } else 0

    fun pick(
        salt: String,
        options: Int,
    ): Int = (hash("$today|$seed|$salt") % options).toInt()
    return when {
        arrived.isNotEmpty() -> {
            val one = arrived.size == 1
            val any = pick("asked", 2) == 0
            HomeNews(
                if (any) {
                    NewsKind.ASKED_ARRIVED_ANY
                } else if (one) {
                    NewsKind.ASKED_ARRIVED_ONE
                } else {
                    NewsKind.ASKED_ARRIVED_MANY
                },
                arrived.size,
                arrived.first(),
            )
        }
        waiting > 0 && !kid -> HomeNews(if (pick("waiting", 2) == 0) NewsKind.WAITING else NewsKind.WAITING_PAGE, waiting)
        fresh.isNotEmpty() -> {
            val one = fresh.size == 1
            val any = pick("new", 2) == 0
            HomeNews(
                if (any) {
                    NewsKind.NEW_TODAY_ANY
                } else if (one) {
                    NewsKind.NEW_TODAY_ONE
                } else {
                    NewsKind.NEW_TODAY_MANY
                },
                fresh.size,
                fresh.values.first(),
            )
        }
        mine > 0 -> HomeNews(NewsKind.MINE_FINISHING, mine)
        all > 0 && !kid -> HomeNews(NewsKind.FINISHING, all)
        else -> null
    }
}

private fun stateOf(download: HomeDownload): String = if (stageOf(download) == DownloadStage.DOWNLOADING) "downloading" else download.state

internal fun stageOf(download: HomeDownload): DownloadStage {
    if (download.clientHint == "checking" || download.clientHint == "moving") return DownloadStage.CHECKING
    return when (download.phase) {
        "waiting" -> DownloadStage.QUEUED
        "paused" -> DownloadStage.PAUSED
        "downloading" -> if (download.progress >= 1) DownloadStage.IMPORTING else DownloadStage.DOWNLOADING
        "stalled" -> DownloadStage.STALLED
        "waiting_to_import", "importing" -> DownloadStage.IMPORTING
        "needs_a_decision" -> DownloadStage.DECIDE
        "failed", "vanished" -> if (download.replaced) DownloadStage.DONE else DownloadStage.FAILED
        "import_refused" -> DownloadStage.FAILED
        "done" -> if (download.state == "seeding") DownloadStage.SEEDING else DownloadStage.DONE
        else -> DownloadStage.DONE
    }
}

private val FETCHING =
    setOf(DownloadStage.CHECKING, DownloadStage.QUEUED, DownloadStage.DOWNLOADING, DownloadStage.STALLED, DownloadStage.IMPORTING)

internal fun homeCounts(download: HomeDownload): Boolean = !download.clientItem || !download.outside || download.titleId > 0

private fun isActive(download: HomeDownload): Boolean =
    download.active || download.state == "downloading" || download.state == "queued" || download.state == "stalled"

internal fun fetchingNow(download: HomeDownload): Boolean {
    val stage = stageOf(download)
    return stage in FETCHING && (isActive(download) || stage == DownloadStage.IMPORTING)
}

internal fun downloadingNow(downloads: List<HomeDownload>): List<HomeDownload> =
    downloads
        .filter { homeCounts(it) && fetchingNow(it) }
        .sortedWith(compareByDescending<HomeDownload> { it.progress }.thenByDescending { it.id })

internal fun notSentNow(downloads: List<HomeDownload>): List<HomeDownload> = downloads.filter { !homeCounts(it) && fetchingNow(it) }

enum class AttentionKind {
    HEALTH,
    DOWNLOAD_FAILED,
    IMPORT_REFUSED,
    DECISION,
    SETUP,
}

enum class AttentionTarget {
    SETTINGS,
    HEALTH,
    DOWNLOADS,
}

data class AttentionRow(
    val key: String,
    val tone: Tone,
    val kind: AttentionKind,
    val count: Int,
    val title: String,
    val checkKind: String = "",
    val error: Boolean = false,
    val names: List<String> = emptyList(),
    val target: AttentionTarget,
)

internal fun shownState(check: HealthCheck): String {
    val s = check.confirmed.ifEmpty { check.state }
    return if (s == "error" || s == "warning") s else "ok"
}

internal fun checkKey(check: HealthCheck): String = if (check.key == "backups:key") "key_kit" else check.kind

internal fun healthTarget(
    kind: String,
    admin: Boolean,
): AttentionTarget =
    when {
        !admin -> AttentionTarget.HEALTH
        kind == "waiting_downloads" || kind == "clear_waiting" -> AttentionTarget.DOWNLOADS
        kind == "config_folder" -> AttentionTarget.HEALTH
        else -> AttentionTarget.SETTINGS
    }

private val NAMED = setOf("source", "downloader", "library_folder", "disk_space")

internal fun attention(
    failed: List<HomeDownload>,
    decisions: List<HomeDecision>,
    checks: List<HealthCheck>,
    admin: Boolean,
): List<AttentionRow> {
    data class Item(
        val cause: String,
        val tone: Tone,
        val kind: AttentionKind,
        val title: String,
        val checkKind: String,
        val error: Boolean,
        val subject: String,
        val target: AttentionTarget,
    )
    val items = mutableListOf<Item>()

    fun health(
        check: HealthCheck,
        error: Boolean,
    ) {
        val kind = checkKey(check)
        items +=
            Item(
                cause = "health:$kind:${if (error) "error" else "warning"}",
                tone = if (error) Tone.FAIL else Tone.WAIT,
                kind = AttentionKind.HEALTH,
                title = check.subject,
                checkKind = kind,
                error = error,
                subject = if (check.kind in NAMED) check.subject else "",
                target = healthTarget(check.kind, admin),
            )
    }
    checks.filter { shownState(it) == "error" }.forEach { health(it, true) }
    for (download in failed) {
        val refused = download.phase == "import_refused"
        items +=
            Item(
                cause = "download_failed",
                tone = Tone.FAIL,
                kind = if (refused) AttentionKind.IMPORT_REFUSED else AttentionKind.DOWNLOAD_FAILED,
                title = download.title.ifEmpty { download.name },
                checkKind = "",
                error = true,
                subject = "",
                target = AttentionTarget.DOWNLOADS,
            )
    }
    for (decision in decisions) {
        val title = decision.title + (decision.episode?.label?.let { " $it" } ?: "")
        items += Item("decision", Tone.WAIT, AttentionKind.DECISION, title, "", false, "", AttentionTarget.DOWNLOADS)
    }
    checks.filter { shownState(it) == "warning" }.forEach { health(it, false) }
    return items.groupBy { it.cause }.map { (cause, list) ->
        val first = list.first()
        AttentionRow(
            key = cause + (if (list.size == 1) ":" + first.title else ""),
            tone = first.tone,
            kind = if (list.any { it.kind == AttentionKind.DOWNLOAD_FAILED }) AttentionKind.DOWNLOAD_FAILED else first.kind,
            count = list.size,
            title = first.title,
            checkKind = first.checkKind,
            error = first.error,
            names = list.map { if (first.kind == AttentionKind.HEALTH) it.subject else it.title }.filter { it.isNotEmpty() },
            target = first.target,
        )
    }
}

data class PartCount(
    val total: Int = 0,
    val on: Int = 0,
    val working: Int = 0,
    val off: Int = 0,
    val failing: Int = 0,
    val troubled: List<String> = emptyList(),
)

private val REMOVED = Regex(" \\(removed \\d+\\)$")
private val GOOD = setOf("ok", "working", "enabled")

internal fun partCount(
    parts: List<HomeHealthPart>,
    failingNames: List<String>,
): PartCount {
    val list = parts.filter { !REMOVED.containsMatchIn(it.name) }
    val off = list.count { it.state == "disabled" }
    val on = list.size - off
    val good = list.filter { it.state in GOOD }
    val known = list.map { it.name }.toSet()
    val bad = good.count { it.name in failingNames } + failingNames.count { it !in known }
    val working = (good.size - bad).coerceAtLeast(0)
    val troubled =
        (list.filter { it.state !in GOOD && it.state != "disabled" }.map { it.name } + failingNames).filter { it.isNotEmpty() }.distinct()
    return PartCount(list.size, on, working, off, on - working, troubled)
}

data class FreeSpace(
    val free: Long,
    val many: Boolean,
)

internal fun freeSpace(
    folders: List<Long>,
    zeroIsFull: Boolean,
): FreeSpace? {
    val sizes = folders.filter { it > 0 || (zeroIsFull && it == 0L) }.sorted()
    if (sizes.isEmpty()) return null
    var disks = 1
    for (i in 1 until sizes.size) {
        if (sizes[i] - sizes[i - 1] > maxOf((sizes[i] * 0.05).toLong(), FOUR_GB)) disks++
    }
    return FreeSpace(sizes.first(), disks > 1)
}

private const val FOUR_GB = 4L * 1024 * 1024 * 1024

enum class ArrivedTitle {
    NEW_TODAY,
    NEW_THIS_WEEK,
    JUST_ARRIVED,
    RECENTLY_ADDED,
}

internal fun arrivedTitle(
    newest: Long?,
    now: Long,
): ArrivedTitle {
    newest ?: return ArrivedTitle.JUST_ARRIVED
    val age = now - newest
    return when {
        age < LocalTime.DAY_MILLIS -> ArrivedTitle.NEW_TODAY
        age < 7 * LocalTime.DAY_MILLIS -> ArrivedTitle.NEW_THIS_WEEK
        age < 14 * LocalTime.DAY_MILLIS -> ArrivedTitle.JUST_ARRIVED
        else -> ArrivedTitle.RECENTLY_ADDED
    }
}

enum class UpWhen {
    OUT_NOW,
    TODAY,
    DATE,
    TEXT,
}

data class ComingUp(
    val key: String,
    val titleId: Long,
    val tmdb: String,
    val kind: String,
    val name: String,
    val image: String?,
    val sub: String,
    val whenKind: UpWhen,
    val date: String,
    val text: String,
)

internal fun upLine(
    card: HomeCard,
    today: String,
): Triple<String, UpWhen, String> {
    val parts = card.line.split(" · ")
    var sub = parts.getOrElse(0) { "" }
    var tail: String? = parts.getOrNull(1)
    val outNow = tail?.contains("out now", ignoreCase = true) == true
    val isToday = outNow || card.at.take(10) == today
    val day =
        card.at
            .take(10)
            .takeIf { epochDay(it) != null }
            .orEmpty()
    if (tail == null && sub.startsWith("Out ") && day.isNotEmpty()) {
        sub = ""
        tail = day
    }
    return when {
        outNow -> Triple(sub, UpWhen.OUT_NOW, day)
        isToday -> Triple(sub, UpWhen.TODAY, day)
        day.isNotEmpty() -> Triple(sub, UpWhen.DATE, day)
        else -> Triple(sub, UpWhen.TEXT, tail.orEmpty())
    }
}
