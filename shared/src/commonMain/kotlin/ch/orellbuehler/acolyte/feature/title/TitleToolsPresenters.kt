package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.DismissBody
import ch.orellbuehler.acolyte.api.model.JobView
import ch.orellbuehler.acolyte.api.model.MatchBody
import ch.orellbuehler.acolyte.api.model.MatchItem
import ch.orellbuehler.acolyte.api.model.RemovePlan
import ch.orellbuehler.acolyte.api.model.RenameBody
import ch.orellbuehler.acolyte.api.model.RestoreRequest
import ch.orellbuehler.acolyte.api.model.TitleLeftover
import ch.orellbuehler.acolyte.api.model.TitleRemove
import ch.orellbuehler.acolyte.api.model.TitleRenamePlan
import ch.orellbuehler.acolyte.api.model.TitleRenameStep
import ch.orellbuehler.acolyte.api.model.UnmatchBody
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal suspend fun followJob(
    api: Api,
    jobId: Long,
    progress: (JobView) -> Unit = {},
): JobView? {
    if (jobId <= 0) return null
    repeat(JOB_POLLS) {
        val job = api.result(LibraryEndpoints.job, "id" to jobId).valueOrNull()
        if (job != null) {
            progress(job)
            if (job.finished) return job
        }
        delay(JOB_POLL_MILLIS)
    }
    return null
}

private const val JOB_POLLS = 300
private const val JOB_POLL_MILLIS = 1_000L

data class MatchTarget(
    val leftover: TitleLeftover,
    val season: Int,
    val picked: Set<Long> = emptySet(),
)

data class LeftoversState(
    val show: Boolean,
    val handsOff: Boolean,
    val status: LoadStatus = LoadStatus(),
    val leftovers: List<TitleLeftover> = emptyList(),
    val episodes: List<MatchEpisode> = emptyList(),
    val episodesLoaded: Boolean = false,
    val match: MatchTarget? = null,
    val rescanning: Boolean = false,
    val busy: Boolean = false,
    val lastMatched: List<Long> = emptyList(),
    val renameOffered: Boolean = false,
    val feedback: Feedback? = null,
) {
    val open: List<TitleLeftover> get() = leftovers.filter { !it.dismissed }

    fun find(fileId: Long): TitleLeftover? = leftovers.firstOrNull { it.fileId == fileId }

    val leftAlone: List<TitleLeftover> get() = leftovers.filter { it.dismissed }

    val shift: ShiftGuess? get() = if (show) TitleLogic.shiftGuess(leftovers, episodes) else null

    val seasonNumbers: List<Int> get() = TitleLogic.seasonOrder(episodes.map { it.season }.distinct())

    fun episodesIn(season: Int): List<MatchEpisode> = episodes.filter { it.season == season }.sortedBy { it.episode }

    val pickedHasFile: Boolean get() = match?.let { m -> episodes.any { it.id in m.picked && it.hasFile } } ?: false
}

class LeftoversPresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    show: Boolean,
    handsOff: Boolean,
    private val seasons: List<Int>,
    private val onChanged: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(LeftoversState(show, handsOff))
    private val feedback = FeedbackCounter()

    val state: StateFlow<LeftoversState> = mutableState.asStateFlow()

    private val current: LeftoversState get() = mutableState.value

    suspend fun load() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(TitleEndpoints.leftovers, "id" to titleId)) {
            is ApiResult.Ok -> mutableState.update { it.copy(status = LoadStatus.ready, leftovers = result.value.leftovers) }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
        if (current.show && !current.episodesLoaded) loadEpisodes()
    }

    private suspend fun loadEpisodes() {
        val all = mutableListOf<MatchEpisode>()
        for (season in seasons) {
            val list =
                api
                    .result(
                        LibraryEndpoints.episodes,
                        "id" to titleId,
                        "season" to season,
                        "files" to 1,
                    ).valueOrNull()
                    ?.episodes
                    .orEmpty()
            all +=
                list.filter { !it.outsideOrder }.map { e ->
                    MatchEpisode(e.id, e.season, e.episode, e.name, e.slots.any { it.file != null || it.fileId > 0 })
                }
        }
        mutableState.update { it.copy(episodes = all, episodesLoaded = true) }
    }

    suspend fun rescan() {
        mutableState.update { it.copy(rescanning = true) }
        when (val result = api.result(TitleEndpoints.rescan, "id" to titleId)) {
            is ApiResult.Ok -> {
                say(feedback.next(FeedbackKind.RESCANNING))
                val job = followJob(api, result.value.jobId)
                val count = (job?.result?.get("leftovers") as? JsonPrimitive)?.intOrNull ?: 0
                if (job?.state == "done") say(feedback.next(FeedbackKind.RESCAN_DONE, count = count))
                if (job?.state == "failed") say(feedback.next(FeedbackKind.FAILED, job.error?.message.orEmpty()))
            }
            is ApiResult.Failed -> say(feedback.failed(result.error))
        }
        mutableState.update { it.copy(rescanning = false, episodesLoaded = false) }
        load()
        onChanged()
    }

    suspend fun dismiss(
        leftover: TitleLeftover,
        dismissed: Boolean,
    ) {
        api
            .result(TitleEndpoints.dismiss, DismissBody(dismissed), "id" to titleId, "fid" to leftover.fileId)
            .onSuccess { v ->
                mutableState.update { st ->
                    st.copy(leftovers = st.leftovers.map { if (it.fileId == leftover.fileId) it.copy(dismissed = v.dismissed) else it })
                }
            }.onFailure { say(feedback.failed(it)) }
    }

    fun openMatch(leftover: TitleLeftover) {
        val s = current
        val seasons = s.seasonNumbers
        val guessSeason = leftover.guess?.season?.takeIf { it in seasons }
        val season = guessSeason ?: seasons.firstOrNull { it > 0 } ?: seasons.firstOrNull() ?: 0
        mutableState.update { it.copy(match = MatchTarget(leftover, season, TitleLogic.namePick(leftover, s.episodes).toSet())) }
    }

    fun closeMatch() = mutableState.update { it.copy(match = null) }

    fun matchSeason(season: Int) = mutableState.update { it.copy(match = it.match?.copy(season = season)) }

    fun togglePick(episodeId: Long) =
        mutableState.update { st ->
            val m = st.match ?: return@update st
            st.copy(match = m.copy(picked = if (episodeId in m.picked) m.picked - episodeId else m.picked + episodeId))
        }

    suspend fun matchMovie(leftover: TitleLeftover) = runMatch(listOf(MatchItem(leftover.fileId, emptyList())), replace = false)

    suspend fun confirmMatch() {
        val m = current.match ?: return
        if (m.picked.isEmpty()) return
        runMatch(listOf(MatchItem(m.leftover.fileId, m.picked.toList())), replace = current.pickedHasFile)
    }

    suspend fun matchShift() {
        val guess = current.shift ?: return
        runMatch(guess.moves.map { MatchItem(it.fileId, listOf(it.episodeId)) }, replace = false)
    }

    private suspend fun runMatch(
        items: List<MatchItem>,
        replace: Boolean,
    ) {
        mutableState.update { it.copy(busy = true) }
        when (val result = api.result(TitleEndpoints.match, MatchBody(items, replace), "id" to titleId)) {
            is ApiResult.Ok -> {
                val linked =
                    result.value.results
                        .filter { it.linked }
                        .map { it.fileId }
                val refused = result.value.results.size - linked.size
                mutableState.update {
                    it.copy(
                        match = null,
                        lastMatched = linked,
                        renameOffered = linked.isNotEmpty(),
                        feedback = feedback.next(FeedbackKind.MATCHED, count = linked.size, text = refused.toString()),
                    )
                }
            }
            is ApiResult.Failed -> say(feedback.failed(result.error))
        }
        mutableState.update { it.copy(busy = false, episodesLoaded = false) }
        load()
        onChanged()
    }

    suspend fun undoMatch() {
        val ids = current.lastMatched
        if (ids.isEmpty()) return
        api
            .result(TitleEndpoints.unmatch, UnmatchBody(ids), "id" to titleId)
            .onSuccess { v ->
                mutableState.update {
                    it.copy(
                        lastMatched = emptyList(),
                        renameOffered = false,
                        feedback = feedback.next(FeedbackKind.UNMATCHED, count = v.unmatched),
                    )
                }
            }.onFailure { say(feedback.failed(it)) }
        mutableState.update { it.copy(episodesLoaded = false) }
        load()
        onChanged()
    }

    private fun say(next: Feedback) {
        mutableState.update { it.copy(feedback = next) }
    }
}

data class RenameState(
    val season: Int,
    val status: LoadStatus = LoadStatus(),
    val plan: TitleRenamePlan = TitleRenamePlan(),
    val running: Boolean = false,
    val progress: Double = 0.0,
    val note: String = "",
    val done: Boolean = false,
    val feedback: Feedback? = null,
) {
    private fun inSeason(step: TitleRenameStep): Boolean {
        if (season < 0) return true
        val code = TitleLogic.episodeCodeOf(step.from) ?: return false
        return code.startsWith("S" + season.toString().padStart(2, '0') + "E")
    }

    val changes: List<TitleRenameStep> get() = plan.steps.filter { it.changes && inSeason(it) }

    val skipped: List<TitleRenameStep> get() = plan.steps.filter { it.skip.isNotEmpty() && it.origin != "found" && inSeason(it) }

    val alreadyRight: Int get() = plan.steps.count { !it.changes && it.skip.isEmpty() && inSeason(it) }
}

class RenamePresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    season: Int,
    private val onChanged: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(RenameState(season))
    private val feedback = FeedbackCounter()

    val state: StateFlow<RenameState> = mutableState.asStateFlow()

    suspend fun load() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(TitleEndpoints.renamePlan, "id" to titleId)) {
            is ApiResult.Ok -> mutableState.update { it.copy(status = LoadStatus.ready, plan = result.value) }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    suspend fun apply() {
        val s = mutableState.value
        val ids = s.changes.map { it.fileId }
        if (ids.isEmpty() || s.running) return
        mutableState.update { it.copy(running = true, progress = 0.0, note = "") }
        when (val result = api.result(TitleEndpoints.rename, RenameBody(s.plan.planHash, ids), "id" to titleId)) {
            is ApiResult.Ok -> {
                val job =
                    followJob(api, result.value.jobId) { job ->
                        mutableState.update { it.copy(progress = job.progress, note = job.progressNote.orEmpty()) }
                    }
                val renamed = (job?.result?.get("renamed") as? JsonPrimitive)?.intOrNull ?: ids.size
                val next =
                    when {
                        job?.state == "done" -> feedback.next(FeedbackKind.RENAMED, count = renamed)
                        job?.error?.code == "plan_changed" -> feedback.next(FeedbackKind.RENAME_CHANGED)
                        else -> feedback.next(FeedbackKind.RENAME_FAILED)
                    }
                mutableState.update { it.copy(running = false, feedback = next, done = job?.state == "done") }
                onChanged()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(running = false, feedback = feedback.failed(result.error)) }
        }
        load()
    }
}

data class RemoveState(
    val name: String,
    val show: Boolean,
    val season: Int,
    val episodeIds: List<Long>,
    val handsOff: Boolean,
    val deleteFiles: Boolean = false,
    val neverAdd: Boolean = false,
    val plan: RemovePlan? = null,
    val planError: ch.orellbuehler.acolyte.api.ApiError? = null,
    val running: Boolean = false,
    val done: Boolean = false,
    val heldIds: List<Long> = emptyList(),
    val feedback: Feedback? = null,
) {
    val whole: Boolean get() = season < 0 && episodeIds.isEmpty()

    val ready: Boolean get() = plan != null && !running
}

class RemovePresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    name: String,
    show: Boolean,
    season: Int,
    episodeIds: List<Long>,
    handsOff: Boolean,
    private val onRemoved: suspend (Boolean) -> Unit,
) {
    private val mutableState = MutableStateFlow(RemoveState(name, show, season, episodeIds, handsOff))
    private val feedback = FeedbackCounter()
    private var asked = 0

    val state: StateFlow<RemoveState> = mutableState.asStateFlow()

    private fun body(apply: Boolean): TitleRemove {
        val s = mutableState.value
        return TitleRemove(
            apply = apply,
            deleteFiles = s.deleteFiles && !s.handsOff,
            neverAdd = if (s.whole) s.neverAdd else null,
            season = s.season.takeIf { it >= 0 && s.episodeIds.isEmpty() },
            episodeIds = s.episodeIds.ifEmpty { null },
        )
    }

    suspend fun load() {
        val ticket = ++asked
        mutableState.update { it.copy(plan = null, planError = null) }
        val result = api.result(LibraryEndpoints.remove, body(apply = false), "id" to titleId)
        if (ticket != asked) return
        when (result) {
            is ApiResult.Ok -> mutableState.update { it.copy(plan = result.value) }
            is ApiResult.Failed -> mutableState.update { it.copy(planError = result.error) }
        }
    }

    suspend fun setDeleteFiles(on: Boolean) {
        mutableState.update { it.copy(deleteFiles = on) }
        load()
    }

    suspend fun setNeverAdd(on: Boolean) {
        mutableState.update { it.copy(neverAdd = on) }
        load()
    }

    suspend fun apply() {
        if (!mutableState.value.ready) return
        mutableState.update { it.copy(running = true) }
        when (val result = api.result(LibraryEndpoints.remove, body(apply = true), "id" to titleId)) {
            is ApiResult.Ok -> {
                val plan = result.value
                val held = plan.held.map { it.fileId }.filter { it > 0 }
                mutableState.update {
                    it.copy(
                        running = false,
                        done = true,
                        plan = plan,
                        heldIds = held,
                        feedback = feedback.next(FeedbackKind.DELETED, it.name, plan.holdingDays),
                    )
                }
                onRemoved(mutableState.value.whole)
            }
            is ApiResult.Failed -> mutableState.update { it.copy(running = false, feedback = feedback.failed(result.error)) }
        }
    }

    suspend fun undo() {
        val ids = mutableState.value.heldIds
        if (ids.isEmpty()) return
        api
            .result(LibraryEndpoints.restore, RestoreRequest(ids))
            .onSuccess { mutableState.update { it.copy(heldIds = emptyList(), feedback = feedback.next(FeedbackKind.RESTORED, it.name)) } }
            .onFailure { e -> mutableState.update { it.copy(feedback = feedback.failed(e)) } }
        onRemoved(false)
    }
}
