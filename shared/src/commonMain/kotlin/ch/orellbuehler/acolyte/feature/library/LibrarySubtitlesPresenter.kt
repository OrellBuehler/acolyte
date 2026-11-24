package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.model.FileSubtitleSearch
import ch.orellbuehler.acolyte.api.model.MissingSubtitle
import ch.orellbuehler.acolyte.api.model.MissingSubtitleSearch
import ch.orellbuehler.acolyte.api.model.MissingTitle
import ch.orellbuehler.acolyte.api.model.SubtitleLanguage
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class SubtitleRow(
    val row: MissingSubtitle,
    val poster: String?,
    val languageName: String,
    val busy: Boolean,
)

data class LibrarySubtitlesState(
    val status: LoadStatus = LoadStatus(),
    val rows: List<SubtitleRow> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = PAGE,
    val titles: List<MissingTitle> = emptyList(),
    val languages: List<SubtitleLanguage> = emptyList(),
    val language: String = "",
    val kind: String = "",
    val titleId: Long = 0,
    val queuing: Boolean = false,
    val searchTotal: Int = 0,
    val searchDone: Int = 0,
    val notice: LibraryNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty()
    val filtered: Boolean get() = language.isNotEmpty() || kind.isNotEmpty() || titleId != 0L
    val searching: Boolean get() = searchTotal > searchDone
    val hasPrevious: Boolean get() = offset > 0
    val hasNext: Boolean get() = offset + limit < total
    val from: Int get() = if (total == 0) 0 else offset + 1
    val to: Int get() = minOf(offset + limit, total)

    companion object {
        const val PAGE = 100
    }
}

class LibrarySubtitlesPresenter internal constructor(
    private val api: Api,
) {
    private val mutableState = MutableStateFlow(LibrarySubtitlesState())
    private val lock = Mutex()
    private val notices = Notices()
    private var art: Map<Long, String>? = null
    private var rows: List<MissingSubtitle> = emptyList()
    private var busy: Set<Long> = emptySet()
    private var tracked: Set<Long> = emptySet()
    private var finished: Set<Long> = emptySet()

    val state: StateFlow<LibrarySubtitlesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            if (art == null) art = TitleArt.load(api)
            if (mutableState.value.languages.isEmpty()) {
                api.result(LibraryEndpoints.subtitleLanguages).valueOrNull()?.let { l ->
                    mutableState.update { it.copy(languages = l.languages) }
                }
            }
            val current = mutableState.value
            val result =
                api.result(
                    LibraryEndpoints.missingSubtitles,
                    "language" to current.language.ifEmpty { null },
                    "kind" to current.kind.ifEmpty { null },
                    "title" to current.titleId.takeIf { it > 0 },
                    "limit" to current.limit,
                    "offset" to current.offset,
                )
            when (result) {
                is ApiResult.Ok -> {
                    rows = result.value.missing
                    mutableState.update { it.copy(status = LoadStatus.ready, total = result.value.total, titles = result.value.titles) }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
            rebuild()
        }
    }

    private fun rebuild() {
        val names = mutableState.value.languages.associate { it.code to it.name }
        mutableState.update {
            it.copy(
                rows = rows.map { r -> SubtitleRow(r, art?.get(r.titleId), names[r.language] ?: r.language, r.fileId in busy) },
                searchTotal = tracked.size,
                searchDone = tracked.count { id -> id in finished },
            )
        }
    }

    suspend fun setLanguage(code: String) {
        mutableState.update { it.copy(language = code, offset = 0) }
        refresh()
    }

    suspend fun setKind(kind: String) {
        mutableState.update { it.copy(kind = kind, offset = 0) }
        refresh()
    }

    suspend fun setTitle(id: Long) {
        mutableState.update { it.copy(titleId = id, offset = 0) }
        refresh()
    }

    suspend fun nextPage() {
        if (!mutableState.value.hasNext) return
        mutableState.update { it.copy(offset = it.offset + it.limit) }
        refresh()
    }

    suspend fun previousPage() {
        if (!mutableState.value.hasPrevious) return
        mutableState.update { it.copy(offset = maxOf(0, it.offset - it.limit)) }
        refresh()
    }

    suspend fun searchAll() {
        val current = mutableState.value
        mutableState.update { it.copy(queuing = true) }
        val body =
            MissingSubtitleSearch(
                kind = current.kind.ifEmpty { null },
                language = current.language.ifEmpty { null },
                titleId = current.titleId.takeIf { it > 0 },
            )
        when (val result = api.result(LibraryEndpoints.searchMissingSubtitles, body)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(queuing = false, notice = notices.make(LibraryNoticeKind.SUBTITLES_SEARCHING, count = result.value.queued))
                }
                follow(result.value.jobIds)
            }
            is ApiResult.Failed -> mutableState.update { it.copy(queuing = false, notice = notices.failed(result.error)) }
        }
    }

    suspend fun searchRow(fileId: Long) {
        val row = rows.firstOrNull { it.fileId == fileId } ?: return
        busy = busy + fileId
        rebuild()
        val result = api.result(LibraryEndpoints.searchFileSubtitles, FileSubtitleSearch(fileId))
        busy = busy - fileId
        when (result) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(notice = notices.make(LibraryNoticeKind.SUBTITLES_SEARCHING, count = 1, name = row.title)) }
                follow(result.value.jobIds)
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(notice = notices.failed(result.error)) }
                rebuild()
            }
        }
    }

    private suspend fun follow(ids: List<Long>) {
        if (ids.isEmpty()) return
        tracked = tracked + ids
        rebuild()
        while (tracked.any { it !in finished }) {
            delay(POLL_MILLIS)
            val jobs = api.result(LibraryEndpoints.jobs, "limit" to JOB_LIMIT).valueOrNull()?.jobs ?: continue
            val seen = jobs.associate { it.id to it.state }
            finished = finished + tracked.filter { id -> seen[id]?.let { it in ENDED } ?: true }
            rebuild()
        }
        refresh()
    }

    suspend fun watch() {
        api.live.collect { event ->
            val data = event.data as? JsonObject
            val kind = (data?.get("Kind") as? JsonPrimitive)?.content.orEmpty()
            if (event.topic == LiveEvent.RESYNC || (event.topic == "event" && kind.startsWith("subtitle_"))) {
                delay(LIVE_DELAY)
                refresh()
            }
        }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private companion object {
        const val POLL_MILLIS = 3_000L
        const val JOB_LIMIT = 200
        const val LIVE_DELAY = 500L
        val ENDED = setOf("done", "failed", "cancelled")
    }
}
