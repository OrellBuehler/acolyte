package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.model.CopySearchRequest
import ch.orellbuehler.acolyte.api.model.CopySelection
import ch.orellbuehler.acolyte.api.model.LibraryCopy
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object TitleArt {
    suspend fun load(api: Api): Map<Long, String> =
        api
            .result(LibraryEndpoints.titles, "brief" to 1)
            .valueOrNull()
            ?.titles
            ?.mapNotNull { t -> t.artwork.firstOrNull()?.let { t.id to it } }
            ?.toMap()
            .orEmpty()
}

data class CopyRow(
    val item: LibraryCopy,
    val poster: String?,
    val fileName: String,
    val selected: Boolean,
)

data class LibraryCopiesState(
    val status: LoadStatus = LoadStatus(),
    val rows: List<CopyRow> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = PAGE,
    val query: String = "",
    val codec: String = ALL,
    val sort: String = "title",
    val codecs: List<FilterOption> = emptyList(),
    val selectedCount: Int = 0,
    val review: CopyReview? = null,
    val started: Int = -1,
    val busy: Boolean = false,
    val notice: LibraryNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty()
    val hasPrevious: Boolean get() = offset > 0
    val hasNext: Boolean get() = offset + limit < total
    val from: Int get() = if (total == 0) 0 else offset + 1
    val to: Int get() = minOf(offset + limit, total)

    companion object {
        const val PAGE = 50
        const val ALL = "all"
        val SORTS = listOf("title", "codec", "added")
    }
}

class LibraryCopiesPresenter internal constructor(
    private val api: Api,
) {
    private val mutableState = MutableStateFlow(LibraryCopiesState())
    private val lock = Mutex()
    private val notices = Notices()
    private var art: Map<Long, String>? = null
    private var selected: Map<Long, CopySelection> = emptyMap()
    private var copies: List<LibraryCopy> = emptyList()

    val state: StateFlow<LibraryCopiesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val current = mutableState.value
            mutableState.update { it.copy(status = it.status.started()) }
            if (art == null) art = TitleArt.load(api)
            val result =
                api.result(
                    LibraryEndpoints.copies,
                    "q" to LibraryLogic.query(current.query).ifEmpty { null },
                    "codec" to current.codec.takeIf { it != LibraryCopiesState.ALL },
                    "sort" to current.sort,
                    "limit" to current.limit,
                    "offset" to current.offset,
                )
            when (result) {
                is ApiResult.Ok -> {
                    val page = result.value
                    copies = page.copies
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            total = page.total,
                            codecs =
                                listOf(FilterOption(LibraryCopiesState.ALL, selected = it.codec == LibraryCopiesState.ALL)) +
                                    page.codecs.entries
                                        .sortedByDescending { e -> e.value }
                                        .map { e -> FilterOption(e.key, count = e.value, selected = it.codec == e.key) },
                        )
                    }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
            rebuild()
        }
    }

    private fun rebuild() {
        mutableState.update {
            it.copy(
                rows = copies.map { c -> CopyRow(c, art?.get(c.titleId), LibraryLogic.fileName(c.relativePath), c.fileId in selected) },
                selectedCount = selected.size,
            )
        }
    }

    suspend fun search(text: String) {
        mutableState.update { it.copy(query = text.take(256), offset = 0) }
        delay(SEARCH_DELAY)
        if (mutableState.value.query == text.take(256)) refresh()
    }

    suspend fun setCodec(codec: String) {
        mutableState.update { it.copy(codec = codec, offset = 0) }
        refresh()
    }

    suspend fun setSort(sort: String) {
        mutableState.update { it.copy(sort = sort, offset = 0) }
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

    fun toggle(fileId: Long) {
        val copy = copies.firstOrNull { it.fileId == fileId } ?: return
        if (copy.isProtected) return
        selected = if (fileId in selected) selected - fileId else selected + (fileId to CopySelection(copy.fileId, copy.versionId))
        mutableState.update { it.copy(review = null, started = -1) }
        rebuild()
    }

    suspend fun review() {
        val selections = selected.values.toList().take(MAX)
        if (selections.isEmpty()) return
        mutableState.update { it.copy(busy = true) }
        api
            .result(LibraryEndpoints.copiesPreview, CopySearchRequest(selections))
            .onSuccess { preview ->
                val review =
                    CopyReview(
                        preview.copies.map { CopyReviewItem(it.title, it.version, it.codec) },
                        preview.slotCount,
                        preview.reviewToken,
                        selections,
                    )
                mutableState.update { it.copy(busy = false, review = review) }
            }.onFailure { error -> mutableState.update { it.copy(busy = false, notice = notices.failed(error)) } }
    }

    fun cancelReview() {
        mutableState.update { it.copy(review = null) }
    }

    suspend fun start() {
        val review = mutableState.value.review ?: return
        mutableState.update { it.copy(busy = true) }
        api
            .result(LibraryEndpoints.copiesSearch, CopySearchRequest(review.selections, review.token))
            .onSuccess { answer ->
                selected = emptyMap()
                mutableState.update { it.copy(busy = false, review = null, started = answer.jobIds.size) }
                rebuild()
            }.onFailure { error -> mutableState.update { it.copy(busy = false, review = null, notice = notices.failed(error)) } }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private companion object {
        const val SEARCH_DELAY = 250L
        const val MAX = 100
    }
}
