package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.ReportsEndpoints
import ch.orellbuehler.acolyte.api.model.NewReport
import ch.orellbuehler.acolyte.api.model.Report
import ch.orellbuehler.acolyte.api.model.ReportFix
import ch.orellbuehler.acolyte.api.model.TitleSearchBody
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class ReportFilter {
    OPEN,
    FIXED,
    ALL,
}

enum class ReportKind(
    val wire: String,
) {
    PICTURE("picture"),
    SOUND("sound"),
    SUBTITLES("subtitles"),
    WONT_PLAY("wont_play"),
    OTHER("other"),
    ;

    companion object {
        fun of(wire: String): ReportKind = entries.firstOrNull { it.wire == wire } ?: OTHER
    }
}

enum class ReportsNoteKind {
    FIXED,
    FIX_FAILED,
    REOPENED,
    REOPEN_FAILED,
    SEARCH_STARTED,
    SEARCH_FAILED,
}

data class ReportsNote(
    val id: Long,
    val kind: ReportsNoteKind,
    val name: String = "",
    val reporter: String = "",
    val error: ApiError? = null,
)

data class ReportsState(
    val status: LoadStatus = LoadStatus(),
    val manager: Boolean = false,
    val reports: List<Report> = emptyList(),
    val chosen: ReportFilter? = null,
    val busy: Set<Long> = emptySet(),
    val searching: Set<Long> = emptySet(),
    val note: ReportsNote? = null,
) {
    val filter: ReportFilter get() = chosen ?: if (manager) ReportFilter.OPEN else ReportFilter.ALL

    val counts: Map<ReportFilter, Int>
        get() =
            mapOf(
                ReportFilter.OPEN to reports.count { it.isOpen },
                ReportFilter.FIXED to reports.count { !it.isOpen },
                ReportFilter.ALL to reports.size,
            )

    val shown: List<Report>
        get() =
            reports
                .filter {
                    when (filter) {
                        ReportFilter.OPEN -> it.isOpen
                        ReportFilter.FIXED -> !it.isOpen
                        ReportFilter.ALL -> true
                    }
                }.sortedWith(
                    compareByDescending<Report> { it.isOpen }
                        .thenByDescending {
                            parseMillis(
                                it.createdAt,
                            ) ?: 0L
                        }.thenByDescending { it.id },
                )

    val isEmpty: Boolean get() = status.phase == Phase.READY && shown.isEmpty()

    fun report(id: Long): Report? = reports.firstOrNull { it.id == id }
}

val Report.isOpen: Boolean get() = state != "fixed"

val Report.reportKind: ReportKind get() = ReportKind.of(kind)

val Report.partWords: String
    get() {
        val place = where.trim()
        val name = itemName.trim()
        if (place.isEmpty() && name.isEmpty()) return ""
        return if (name.isEmpty() || place.contains(name)) {
            place
        } else if (place.isEmpty()) {
            name
        } else {
            "$place · $name"
        }
    }

val Report.versionWord: String
    get() =
        Regex("^\\((.+)\\)$")
            .find(partWords)
            ?.groupValues
            ?.get(1)
            .orEmpty()

val Report.reporterName: String get() = madeBy?.let { it.displayName.ifBlank { shownName(it.name) } }.orEmpty()

val Report.fixerName: String get() = closedBy?.let { it.displayName.ifBlank { shownName(it.name) } }.orEmpty()

class ReportsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val inbox: RequestsInbox,
) {
    private val mutableState = MutableStateFlow(ReportsState())
    private val lock = Mutex()
    private var noteId = 0L

    val state: StateFlow<ReportsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val manager = account()?.can(Need.MANAGE_LIBRARY) == true
            mutableState.update { it.copy(status = it.status.started(), manager = manager) }
            api
                .result(ReportsEndpoints.list, "limit" to LIMIT)
                .onSuccess { answer -> mutableState.update { it.copy(status = LoadStatus.ready, reports = answer.reports) } }
                .onFailure { error -> mutableState.update { it.copy(status = it.status.failed(error)) } }
        }
    }

    fun setFilter(filter: ReportFilter) = mutableState.update { it.copy(chosen = filter) }

    fun dismissNote() = mutableState.update { it.copy(note = null) }

    suspend fun fix(
        id: Long,
        reply: String,
    ): ApiError? {
        val report = mutableState.value.report(id) ?: return null
        mutableState.update { it.copy(busy = it.busy + id) }
        val body = ReportFix(reply.trim().take(NOTE_MAX).ifEmpty { null })
        return when (val result = api.result(ReportsEndpoints.fix, body, "id" to id)) {
            is ApiResult.Ok -> {
                replace(result.value.report)
                note(ReportsNoteKind.FIXED, report.titleName, if (report.mine) "" else report.reporterName)
                inbox.refresh()
                null
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(busy = it.busy - id) }
                note(ReportsNoteKind.FIX_FAILED, report.titleName, error = result.error)
                result.error
            }
        }
    }

    suspend fun reopen(id: Long): ApiError? {
        val report = mutableState.value.report(id) ?: return null
        mutableState.update { it.copy(busy = it.busy + id) }
        return when (val result = api.result(ReportsEndpoints.reopen, "id" to id)) {
            is ApiResult.Ok -> {
                replace(result.value.report)
                note(ReportsNoteKind.REOPENED, report.titleName)
                inbox.refresh()
                null
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(busy = it.busy - id) }
                note(ReportsNoteKind.REOPEN_FAILED, report.titleName, error = result.error)
                result.error
            }
        }
    }

    suspend fun betterCopy(id: Long) {
        val report = mutableState.value.report(id) ?: return
        mutableState.update { it.copy(searching = it.searching + id) }
        api
            .result(ReportsEndpoints.search, searchBody(report), "id" to report.titleId)
            .onSuccess { note(ReportsNoteKind.SEARCH_STARTED, report.titleName) }
            .onFailure { note(ReportsNoteKind.SEARCH_FAILED, report.titleName, error = it) }
        mutableState.update { it.copy(searching = it.searching - id) }
    }

    suspend fun watch() {
        api.live.collect { event ->
            if (event.topic == LiveEvent.RESYNC) {
                refresh()
            } else if (event.topic == EVENT_TOPIC) {
                val kind = ((event.data as? JsonObject)?.let { it["Kind"] ?: it["kind"] })?.jsonPrimitive?.contentOrNull.orEmpty()
                if (kind.startsWith("report_")) refresh()
            }
        }
    }

    private fun replace(report: Report) =
        mutableState.update { s ->
            s.copy(reports = s.reports.map { if (it.id == report.id) report else it }, busy = s.busy - report.id)
        }

    private fun note(
        kind: ReportsNoteKind,
        name: String,
        reporter: String = "",
        error: ApiError? = null,
    ) {
        noteId++
        mutableState.update { it.copy(note = ReportsNote(noteId, kind, name, reporter, error)) }
    }

    private companion object {
        const val LIMIT = 200
        const val EVENT_TOPIC = "event"
    }
}

internal fun searchBody(report: Report): TitleSearchBody =
    when (report.titleKind) {
        "show" ->
            TitleSearchBody(
                itemId = report.itemId.takeIf { it > 0 },
                season = report.season.takeIf { report.itemId <= 0 },
                versionId = report.versionId.takeIf { it > 0 },
            )
        "artist" ->
            TitleSearchBody(
                itemId = report.itemId.takeIf { it > 0 },
                albumId = report.albumId.takeIf { it > 0 && report.itemId <= 0 },
                versionId = report.versionId.takeIf { it > 0 },
            )
        else -> TitleSearchBody()
    }

data class ReportProblemState(
    val kind: ReportKind? = null,
    val note: String = "",
    val busy: Boolean = false,
    val error: ApiError? = null,
    val missingKind: Boolean = false,
    val sent: Report? = null,
) {
    val left: Int get() = NOTE_MAX - note.length
}

class ReportProblemPresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    private val season: Int,
    private val itemId: Long,
    private val albumId: Long,
    private val versionId: Long,
) {
    private val mutableState = MutableStateFlow(ReportProblemState())

    val state: StateFlow<ReportProblemState> = mutableState.asStateFlow()

    fun setKind(kind: ReportKind) = mutableState.update { it.copy(kind = kind, missingKind = false) }

    fun setNote(note: String) = mutableState.update { it.copy(note = note.take(NOTE_MAX)) }

    suspend fun send() {
        val s = mutableState.value
        if (s.busy || s.sent != null) return
        val kind = s.kind
        if (kind == null) {
            mutableState.update { it.copy(missingKind = true) }
            return
        }
        mutableState.update { it.copy(busy = true, error = null) }
        val body =
            NewReport(
                titleId = titleId,
                kind = kind.wire,
                note = s.note.trim().ifEmpty { null },
                season = season.takeIf { it >= 0 },
                itemId = itemId.takeIf { it > 0 },
                albumId = albumId.takeIf { it > 0 },
                versionId = versionId.takeIf { it > 0 },
            )
        when (val result = api.result(ReportsEndpoints.make, body)) {
            is ApiResult.Ok -> mutableState.update { it.copy(busy = false, sent = result.value.report) }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, error = result.error) }
        }
    }
}
