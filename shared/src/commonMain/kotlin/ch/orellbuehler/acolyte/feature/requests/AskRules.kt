package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskLimit
import ch.orellbuehler.acolyte.api.model.AskPickRules
import ch.orellbuehler.acolyte.api.model.KindLimit
import ch.orellbuehler.acolyte.api.model.QualityChoice
import ch.orellbuehler.acolyte.feature.Tone
import kotlin.time.Clock
import kotlin.time.Instant

enum class MemberTab {
    WAITING,
    APPROVED,
    DECLINED,
    READY,
    FAILED,
}

enum class ApproverTab {
    WAITING,
    PROCESSING,
    DONE,
}

enum class KindFilter(
    internal val wire: String,
) {
    ALL(""),
    MOVIE("movie"),
    SHOW("show"),
    ARTIST("artist"),
}

enum class Stage {
    FAILED,
    WAITING,
    UPCOMING,
    SEARCHING,
    DOWNLOADING,
    IMPORTING,
    NEEDS_DECISION,
    LOOKING,
    PROCESSING,
    READY,
    DECLINED,
}

enum class StepKind {
    SEARCHING,
    DOWNLOADING,
    IMPORTING,
    WAITING_ON_ADMIN,
    WAITING,
    NOT_OUT,
    NONE,
}

data class Life(
    val stage: Stage,
    val tone: Tone,
    val progress: Float,
)

const val UNDO_MILLIS: Long = 6_000
const val NOTE_MAX: Int = 500
const val STANDARD_LINE: String = "standard"
const val FOUR_K_LINE: String = "4k"

val Ask.isWaiting: Boolean get() = state == ASKED

val Ask.isFailed: Boolean get() = state == APPROVED && (problem.isNotBlank() || (titleId == 0L && !adding))

val Ask.isArtist: Boolean get() = kind == "artist"

val Ask.memberTab: MemberTab
    get() =
        when {
            state == ASKED -> MemberTab.WAITING
            state == TURNED_DOWN -> MemberTab.DECLINED
            state == AVAILABLE -> MemberTab.READY
            isFailed -> MemberTab.FAILED
            else -> MemberTab.APPROVED
        }

val Ask.approverTab: ApproverTab
    get() =
        when (state) {
            ASKED -> ApproverTab.WAITING
            APPROVED -> ApproverTab.PROCESSING
            else -> ApproverTab.DONE
        }

val Ask.stepKind: StepKind
    get() =
        when (step?.kind) {
            "searching" -> StepKind.SEARCHING
            "downloading" -> StepKind.DOWNLOADING
            "importing" -> StepKind.IMPORTING
            "waiting_on_admin" -> StepKind.WAITING_ON_ADMIN
            "waiting" -> StepKind.WAITING
            "not_out" -> StepKind.NOT_OUT
            else -> StepKind.NONE
        }

val Ask.inFlight: Boolean
    get() =
        state == APPROVED &&
            stepKind in setOf(StepKind.SEARCHING, StepKind.DOWNLOADING, StepKind.IMPORTING, StepKind.WAITING_ON_ADMIN)

val Ask.life: Life
    get() {
        val low = GETTING
        val high = GETTING_END
        return when {
            state == ASKED -> Life(Stage.WAITING, Tone.WAIT, ASKED_WEIGHT)
            state == TURNED_DOWN -> Life(Stage.DECLINED, Tone.IDLE, 1f)
            state == AVAILABLE -> Life(Stage.READY, Tone.OK, 1f)
            isFailed -> Life(Stage.FAILED, Tone.FAIL, APPROVED_WEIGHT)
            else ->
                when (stepKind) {
                    StepKind.SEARCHING -> Life(Stage.SEARCHING, Tone.INFO, low + (high - low) * 0.1f)
                    StepKind.WAITING_ON_ADMIN -> Life(Stage.NEEDS_DECISION, Tone.WAIT, high - 0.03f)
                    StepKind.IMPORTING -> Life(Stage.IMPORTING, Tone.INFO, high - 0.03f)
                    StepKind.DOWNLOADING -> Life(Stage.DOWNLOADING, Tone.INFO, low + (high - low) * 0.15f)
                    StepKind.WAITING -> Life(Stage.LOOKING, Tone.WAIT, low)
                    StepKind.NOT_OUT ->
                        if (futureOnly) {
                            Life(
                                Stage.PROCESSING,
                                Tone.INFO,
                                low,
                            )
                        } else {
                            Life(Stage.UPCOMING, Tone.IDLE, APPROVED_WEIGHT)
                        }
                    StepKind.NONE -> Life(Stage.PROCESSING, Tone.INFO, low)
                }
        }
    }

val Ask.askedLine: String get() = askedQuality(quality, fourK)

fun askedQuality(
    quality: String,
    fourK: Boolean,
): String = quality.ifEmpty { if (fourK) FOUR_K_LINE else STANDARD_LINE }

val Ask.qualityTagLine: String?
    get() = if (isArtist || askedLine == STANDARD_LINE) null else askedLine

val Ask.artistMbid: String
    get() = artistId.ifEmpty { ref.removePrefix("mbid:").takeIf { ref.startsWith("mbid:") }.orEmpty() }

val Ask.askerNames: List<String>
    get() = if (people.isEmpty()) listOf(shownName(askedBy.name)) else people.map { shownName(it.name) }

val Ask.fromWatchlistCount: Int get() = people.count { it.fromWatchlist }

val Ask.deciderName: String
    get() = decidedBy?.let { it.displayName.ifBlank { shownName(it.name) } }.orEmpty()

fun shownName(name: String): String = name.substringBefore(DELETED_MARK).trim()

fun isDeletedName(name: String): Boolean = name.contains(DELETED_MARK)

fun stagesIn(asks: List<Ask>): List<Stage> {
    val present = asks.map { it.life.stage }.toSet()
    return Stage.entries.filter { it in present }
}

fun approverSort(
    tab: ApproverTab,
    asks: List<Ask>,
): List<Ask> {
    fun at(iso: String): Long = parseMillis(iso) ?: 0L

    fun decided(a: Ask): Long = at(a.decidedAt).takeIf { it > 0 } ?: at(a.availableAt).takeIf { it > 0 } ?: at(a.askedAt)
    return when (tab) {
        ApproverTab.WAITING -> asks.sortedByDescending { at(it.askedAt) }
        ApproverTab.PROCESSING -> asks.sortedWith(compareByDescending<Ask> { it.isFailed }.thenByDescending { decided(it) })
        ApproverTab.DONE -> asks.sortedByDescending { decided(it) }
    }
}

fun firstMemberTab(asks: List<Ask>): MemberTab {
    val tabs = asks.map { it.memberTab }.toSet()
    return MemberTab.entries.firstOrNull { it in tabs } ?: MemberTab.WAITING
}

enum class LimitKind {
    MOVIES,
    SHOWS,
    MUSIC,
}

data class LimitLine(
    val kind: LimitKind,
    val left: Int,
    val perWeek: Int,
    val wait: WaitTime?,
)

data class WaitTime(
    val days: Int,
    val hours: Int,
    val minutes: Int,
) {
    val underMinute: Boolean get() = days == 0 && hours == 0 && minutes <= 1
}

fun waitTime(millis: Long): WaitTime {
    if (millis <= 60_000) return WaitTime(0, 0, 1)
    val minutes = if (millis > DAY_MILLIS) ceilDiv(millis, HOUR_MILLIS) * 60 else ceilDiv(millis, 60_000)
    return WaitTime((minutes / 1440).toInt(), ((minutes % 1440) / 60).toInt(), (minutes % 60).toInt())
}

fun limitLines(
    limit: AskLimit?,
    kinds: List<LimitKind> = LimitKind.entries,
    now: Long = Clock.System.now().toEpochMilliseconds(),
): List<LimitLine> {
    if (limit == null || limit.noLimit) return emptyList()
    return kinds.mapNotNull { kind ->
        val k: KindLimit =
            when (kind) {
                LimitKind.MOVIES -> limit.movies
                LimitKind.SHOWS -> limit.shows
                LimitKind.MUSIC -> limit.music
            }
        if (k.noLimit || k.perWeek <= 0) return@mapNotNull null
        val wait = if (k.left > 0) null else parseMillis(k.retryAt)?.let { waitTime(it - now) }
        LimitLine(kind, k.left, k.perWeek, wait)
    }
}

data class QualityPill(
    val key: String,
    val line: String,
    val name: String,
    val rulesId: Long,
    val extra: Boolean,
    val disabled: Boolean,
    val note: PillNote,
)

enum class PillNote {
    NONE,
    IN_LIBRARY,
    REQUESTED,
}

data class MemberRow(
    val pills: List<QualityPill>,
    val selected: String,
) {
    val shown: Boolean get() = pills.size > 1
}

fun memberRow(
    choices: List<QualityChoice>,
    inLibrary: Boolean = false,
    has4k: Boolean = false,
    mine: Set<String> = emptySet(),
    open: Set<String> = emptySet(),
    prefer: String? = null,
): MemberRow {
    val mineMain = choices.any { !it.extra && it.id in mine }
    val pills =
        choices.map { c ->
            val have = if (c.extra) has4k else inLibrary
            val asked = !have && (if (c.extra) c.id in mine else mineMain)
            QualityPill(
                key = c.id,
                line = c.id,
                name = c.name,
                rulesId = 0,
                extra = c.extra,
                disabled = have || asked,
                note =
                    when {
                        have -> PillNote.IN_LIBRARY
                        asked -> PillNote.REQUESTED
                        else -> PillNote.NONE
                    },
            )
        }

    fun free(id: String?): Boolean = id != null && pills.any { it.key == id && !it.disabled }
    val selected = (if (free(prefer)) prefer else open.firstOrNull { free(it) }) ?: pills.firstOrNull { !it.disabled }?.key.orEmpty()
    return MemberRow(pills, selected)
}

fun alsoPills(
    pills: List<QualityPill>,
    selected: String,
    pickMany: Boolean,
): List<QualityPill> {
    if (!pickMany || selected.isEmpty()) return emptyList()
    return pills.filter { it.extra && !it.disabled && it.key != selected }
}

fun ownerRow(
    lines: List<QualityChoice>,
    rules: List<AskPickRules>,
    ask: Ask,
    builtInName: (String) -> String = { it },
): List<QualityPill> {
    val asked = ask.askedLine
    val all = if (lines.any { it.id == asked }) lines else lines + QualityChoice(asked, ask.qualityName, asked == FOUR_K_LINE, 0)

    fun pill(l: QualityChoice) = QualityPill(l.id, l.id, l.name, 0, l.extra, false, PillNote.NONE)
    val mains = all.filter { !it.extra }
    val used = all.map { it.pickRulesId }.filter { it != 0L }.toSet()
    val names = all.map { it.name.ifEmpty { builtInName(it.id) }.lowercase() }.toSet()
    val main = mains.firstOrNull()?.id ?: STANDARD_LINE
    val kind = if (ask.kind == "show") "show" else "movie"
    val other =
        rules
            .filter { it.id !in used && it.name.lowercase() !in names && (it.kinds.isEmpty() || kind in it.kinds) }
            .map { QualityPill("rules-${it.id}", main, it.name, it.id, false, false, PillNote.NONE) }
    return mains.map(::pill) + other + all.filter { it.extra }.map(::pill)
}

enum class AskFailure {
    WEEKLY_LIMIT,
    NEVER_ADD,
    IN_LIBRARY,
    AGE_LIMIT,
    NO_MUSIC,
    ALREADY_DECIDED,
    ASKED_AGAIN,
    NOT_INCLUDED,
    OTHER,
}

fun askFailure(error: ApiError): AskFailure {
    val text = error.message.lowercase()
    return when {
        error.code == ApiErrorCode.RATE_LIMITED -> AskFailure.WEEKLY_LIMIT
        error.code == ApiErrorCode.FORBIDDEN && text.contains("music") -> AskFailure.NO_MUSIC
        error.code == ApiErrorCode.FORBIDDEN && text.contains("rated") -> AskFailure.AGE_LIMIT
        error.code == ApiErrorCode.CONFLICT && text.contains("never-add") -> AskFailure.NEVER_ADD
        error.code == ApiErrorCode.CONFLICT && text.contains("already decided") -> AskFailure.ALREADY_DECIDED
        error.code == ApiErrorCode.CONFLICT && text.contains("asked for this") && text.contains("again") -> AskFailure.ASKED_AGAIN
        error.code == ApiErrorCode.CONFLICT && text.contains("already in the library") -> AskFailure.IN_LIBRARY
        error.code == ApiErrorCode.CONFLICT && text.contains("album kinds") -> AskFailure.NOT_INCLUDED
        else -> AskFailure.OTHER
    }
}

val ApiError.failureKind: AskFailure get() = askFailure(this)

internal fun parseMillis(iso: String): Long? =
    if (iso.isBlank()) {
        null
    } else {
        try {
            Instant.parse(iso).toEpochMilliseconds()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

private fun ceilDiv(
    a: Long,
    b: Long,
): Long = (a + b - 1) / b

internal const val ASKED = "asked"
internal const val APPROVED = "approved"
internal const val TURNED_DOWN = "turned_down"
internal const val AVAILABLE = "available"
private const val DELETED_MARK = '\u2063'
private const val ASKED_WEIGHT = 0.08f
private const val APPROVED_WEIGHT = 0.30f
private const val GETTING = 0.45f
private const val GETTING_END = 0.92f
private const val HOUR_MILLIS = 3_600_000L
private const val DAY_MILLIS = 86_400_000L
