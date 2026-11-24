package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.RenameApply
import ch.orellbuehler.acolyte.api.model.RenameStep
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SubtitleRename(
    val from: String,
    val to: String,
    val skip: String,
)

data class RenameRow(
    val fileId: Long,
    val title: String,
    val poster: String?,
    val from: String,
    val to: String,
    val skip: String,
    val subtitles: List<SubtitleRename>,
)

enum class RenameFailure {
    NONE,
    PLAN_CHANGED,
    FAILED,
}

data class LibraryRenameState(
    val status: LoadStatus = LoadStatus(),
    val changes: List<RenameRow> = emptyList(),
    val skipped: List<RenameRow> = emptyList(),
    val same: Int = 0,
    val running: Boolean = false,
    val job: JobProgress = JobProgress(),
    val failure: RenameFailure = RenameFailure.NONE,
    val failureText: String = "",
    val canOpenNaming: Boolean = false,
    val canRename: Boolean = true,
    val notice: LibraryNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && changes.isEmpty() && skipped.isEmpty()
}

class LibraryRenamePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(LibraryRenameState())
    private val lock = Mutex()
    private val notices = Notices()
    private var art: Map<Long, String>? = null
    private var planHash = ""

    val state: StateFlow<LibraryRenameState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    canOpenNaming = me?.can(Need.SETTINGS) == true,
                    canRename = me?.handsOff != true,
                )
            }
            if (art == null) art = TitleArt.load(api)
            when (val result = api.result(LibraryEndpoints.renamePlan)) {
                is ApiResult.Ok -> {
                    planHash = result.value.planHash
                    val steps = result.value.steps
                    val changes = steps.filter { it.skip.isEmpty() && it.to.isNotEmpty() && it.to != it.from }
                    val skipped = steps.filter { it.skip.isNotEmpty() }
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            changes = changes.map { s -> row(s) },
                            skipped = skipped.map { s -> row(s) },
                            same = steps.size - changes.size - skipped.size,
                        )
                    }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    private fun row(step: RenameStep): RenameRow {
        val (from, to) = pair(step.from, step.to)
        return RenameRow(
            fileId = step.fileId,
            title = step.titleName,
            poster = art?.get(step.titleId),
            from = from,
            to = to,
            skip = step.skip,
            subtitles = step.subtitles.map { SubtitleRename(LibraryLogic.fileName(it.from), LibraryLogic.fileName(it.to), it.skip) },
        )
    }

    private fun pair(
        from: String,
        to: String,
    ): Pair<String, String> {
        fun dir(path: String) = path.substring(0, maxOf(0, path.lastIndexOf('/'), path.lastIndexOf('\\')))
        return if (to.isEmpty() || dir(from) == dir(to)) LibraryLogic.fileName(from) to LibraryLogic.fileName(to) else from to to
    }

    suspend fun rename() {
        if (planHash.isEmpty() || mutableState.value.running) return
        mutableState.update { it.copy(running = true, failure = RenameFailure.NONE, failureText = "", job = JobProgress(running = true)) }
        api
            .result(LibraryEndpoints.rename, RenameApply(planHash))
            .onSuccess { started ->
                val outcome =
                    LibraryJobs.follow(api, started.jobId) { job ->
                        mutableState.update { it.copy(job = JobProgress(true, job.progress, job.progressNote.orEmpty())) }
                    }
                val job = outcome.job
                mutableState.update {
                    when {
                        outcome.done ->
                            it.copy(
                                running = false,
                                job = JobProgress(),
                                notice = notices.make(LibraryNoticeKind.RENAMED, count = LibraryJobs.count(job, "renamed")),
                            )
                        job?.error?.code == "plan_changed" ->
                            it.copy(
                                running = false,
                                job = JobProgress(),
                                failure = RenameFailure.PLAN_CHANGED,
                            )
                        else ->
                            it.copy(
                                running = false,
                                job = JobProgress(),
                                failure = RenameFailure.FAILED,
                                failureText =
                                    job
                                        ?.error
                                        ?.message
                                        .orEmpty()
                                        .ifEmpty { outcome.error?.message.orEmpty() },
                            )
                    }
                }
                refresh()
            }.onFailure { error -> mutableState.update { it.copy(running = false, job = JobProgress(), notice = notices.failed(error)) } }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }
}
