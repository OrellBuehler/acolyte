package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.HeldFile
import ch.orellbuehler.acolyte.api.model.RestoreRequest
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil

enum class HeldShow {
    ALL,
    DELETED,
    REPLACED,
}

data class HeldRow(
    val file: HeldFile,
    val poster: String?,
    val fileName: String,
    val daysLeft: Int,
    val selected: Boolean,
)

data class TakenQuestion(
    val id: Long,
    val name: String,
    val canSwap: Boolean,
)

data class LibraryDeletedState(
    val status: LoadStatus = LoadStatus(),
    val rows: List<HeldRow> = emptyList(),
    val count: Int = 0,
    val show: HeldShow = HeldShow.ALL,
    val hasBoth: Boolean = false,
    val holdingDays: Int = -1,
    val titleId: Long = 0,
    val titleName: String = "",
    val selectedCount: Int = 0,
    val allSelected: Boolean = false,
    val busy: Boolean = false,
    val question: TakenQuestion? = null,
    val notice: LibraryNotice? = null,
    val canRestore: Boolean = true,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty()
}

class LibraryDeletedPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    titleId: Long,
) {
    private val mutableState = MutableStateFlow(LibraryDeletedState(titleId = titleId))
    private val lock = Mutex()
    private val notices = Notices()
    private var files: List<HeldFile> = emptyList()
    private var picked: Set<Long> = emptySet()
    private var art: Map<Long, String>? = null
    private var questions: List<TakenQuestion> = emptyList()

    val state: StateFlow<LibraryDeletedState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            mutableState.update { it.copy(status = it.status.started(), canRestore = me?.handsOff != true) }
            if (art == null) art = TitleArt.load(api)
            if (mutableState.value.holdingDays < 0 && me?.can(Need.MANAGE_LIBRARY) == true) {
                api.result(LibraryEndpoints.fileSettings).valueOrNull()?.let { s ->
                    mutableState.update { it.copy(holdingDays = s.holdingDays) }
                }
            }
            val titleId = mutableState.value.titleId
            when (val result = api.result(LibraryEndpoints.holding, "title_id" to titleId.takeIf { it > 0 })) {
                is ApiResult.Ok -> {
                    files = result.value.files
                    picked = picked.filter { id -> files.any { it.id == id } }.toSet()
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            titleName = files.firstOrNull { f -> f.titleId == titleId }?.title ?: it.titleName,
                        )
                    }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
            rebuild()
        }
    }

    private fun rebuild() {
        val both = files.any { it.replaced } && files.any { !it.replaced }
        val show = if (both) mutableState.value.show else HeldShow.ALL
        val now = LibraryLogic.now()
        val shown =
            files.filter {
                when (show) {
                    HeldShow.ALL -> true
                    HeldShow.DELETED -> !it.replaced
                    HeldShow.REPLACED -> it.replaced
                }
            }
        val rows =
            shown.map { f ->
                val left = LibraryLogic.millis(f.expiresAt)?.let { ceil((it - now) / DAY_MILLIS).toInt().coerceAtLeast(0) } ?: -1
                HeldRow(f, art?.get(f.titleId), LibraryLogic.fileName(f.path), left, f.id in picked)
            }
        val visible = rows.map { it.file.id }.toSet()
        val count = picked.count { it in visible }
        mutableState.update {
            it.copy(
                rows = rows,
                count = files.size,
                show = show,
                hasBoth = both,
                selectedCount = count,
                allSelected = rows.isNotEmpty() && count == rows.size,
                question = questions.firstOrNull(),
            )
        }
    }

    fun setShow(show: HeldShow) {
        mutableState.update { it.copy(show = show) }
        rebuild()
    }

    suspend fun clearTitle() {
        mutableState.update { it.copy(titleId = 0, titleName = "") }
        refresh()
    }

    fun toggle(id: Long) {
        picked = if (id in picked) picked - id else picked + id
        rebuild()
    }

    fun toggleAll() {
        val visible = mutableState.value.rows.map { it.file.id }
        picked = if (mutableState.value.allSelected) picked - visible.toSet() else picked + visible
        rebuild()
    }

    suspend fun restore() {
        val visible =
            mutableState.value.rows
                .map { it.file.id }
                .toSet()
        val ids = picked.filter { it in visible }
        if (ids.isEmpty()) return
        mutableState.update { it.copy(busy = true) }
        when (val result = api.result(LibraryEndpoints.restore, RestoreRequest(ids))) {
            is ApiResult.Ok -> {
                val results = result.value.results
                val restored = results.count { it.result == "restored" }
                questions =
                    results.filter { it.taken }.map { r ->
                        TakenQuestion(r.id, LibraryLogic.fileName(files.firstOrNull { it.id == r.id }?.path ?: r.path), r.canSwap)
                    }
                val failed = results.firstOrNull { it.result != "restored" && !it.taken }
                val notice =
                    if (failed != null) {
                        notices.make(
                            LibraryNoticeKind.RESTORE_FAILED,
                            name = LibraryLogic.fileName(files.firstOrNull { it.id == failed.id }?.path ?: failed.path),
                            error = ApiError(ApiErrorCode.CONFLICT, 0, failed.result),
                        )
                    } else {
                        notices.make(LibraryNoticeKind.RESTORED, count = restored)
                    }
                picked = emptySet()
                mutableState.update { it.copy(busy = false, notice = if (restored > 0 || failed != null) notice else it.notice) }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, notice = notices.failed(result.error)) }
        }
        refresh()
    }

    suspend fun answer(how: String) {
        val question = questions.firstOrNull() ?: return
        questions = questions.drop(1)
        if (how != "swap" && how != "both") {
            rebuild()
            return
        }
        mutableState.update { it.copy(busy = true) }
        when (val result = api.result(LibraryEndpoints.restore, RestoreRequest(listOf(question.id), how))) {
            is ApiResult.Ok -> {
                val ok = result.value.results.count { it.result == "restored" }
                val kind = if (how == "swap") LibraryNoticeKind.SWAPPED else LibraryNoticeKind.RESTORED
                mutableState.update { it.copy(busy = false, notice = notices.make(kind, count = ok, name = question.name)) }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, notice = notices.failed(result.error)) }
        }
        refresh()
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private companion object {
        const val DAY_MILLIS = 86_400_000.0
    }
}
