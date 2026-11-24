package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.SeriesRemoveRequest
import ch.orellbuehler.acolyte.api.model.SeriesTitle
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SeriesMovie(
    val id: Long,
    val name: String,
    val year: Int,
    val versions: Int,
    val files: Int,
    val hasProtected: Boolean,
    val versionIds: List<Long>,
    val fileIds: List<Long>,
)

data class SeriesReviewRow(
    val movie: SeriesMovie,
    val skip: String,
)

enum class SeriesOutcome {
    REMOVED,
    SKIPPED,
    FAILED,
}

data class SeriesOutcomeRow(
    val name: String,
    val outcome: SeriesOutcome,
    val why: String,
)

data class SeriesState(
    val status: LoadStatus = LoadStatus(),
    val id: String = "",
    val name: String = "",
    val movies: List<SeriesMovie> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val deleteFiles: Boolean = false,
    val neverAdd: Boolean = false,
    val acknowledge: Boolean = false,
    val review: List<SeriesReviewRow>? = null,
    val results: List<SeriesOutcomeRow>? = null,
    val busy: Boolean = false,
    val actionError: ApiError? = null,
    val canManage: Boolean = false,
    val showDetails: Boolean = false,
) {
    val needsAcknowledge: Boolean get() = deleteFiles && movies.any { it.id in selected && it.hasProtected }

    val canReview: Boolean get() = selected.isNotEmpty() && !busy && canManage

    val isEmpty: Boolean get() = status.phase == Phase.READY && movies.isEmpty()
}

private fun SeriesTitle.movie(): SeriesMovie =
    SeriesMovie(
        id = id,
        name = name,
        year = year,
        versions = versionIds.size,
        files = files.size,
        hasProtected = protected || files.any { it.protected },
        versionIds = versionIds,
        fileIds = files.map { it.id },
    )

class SeriesPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val id: String,
) {
    private val mutableState = MutableStateFlow(SeriesState(id = id))
    private val lock = Mutex()
    private var token: String = ""

    val state: StateFlow<SeriesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val account = account()
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    canManage = account?.can(Need.MANAGE_LIBRARY) == true,
                    showDetails = account?.can(Need.SETTINGS) == true,
                )
            }
            when (val result = api.result(DiscoverEndpoints.series, "id" to id)) {
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                is ApiResult.Ok -> {
                    val movies = result.value.titles.map { it.movie() }
                    token = ""
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            name = result.value.name,
                            movies = movies,
                            selected = movies.map { m -> m.id }.toSet(),
                            review = null,
                            results = null,
                            acknowledge = false,
                        )
                    }
                }
            }
        }
    }

    fun toggle(movie: Long) {
        if (mutableState.value.results != null) return
        token = ""
        mutableState.update {
            val next = if (movie in it.selected) it.selected - movie else it.selected + movie
            it.copy(selected = next, review = null, actionError = null)
        }
    }

    fun setDeleteFiles(on: Boolean) {
        token = ""
        mutableState.update { it.copy(deleteFiles = on, acknowledge = false, review = null, actionError = null) }
    }

    fun setNeverAdd(on: Boolean) {
        token = ""
        mutableState.update { it.copy(neverAdd = on, review = null, actionError = null) }
    }

    fun setAcknowledge(on: Boolean) {
        token = ""
        mutableState.update { it.copy(acknowledge = on, review = null, actionError = null) }
    }

    fun cancelReview() {
        token = ""
        mutableState.update { it.copy(review = null) }
    }

    suspend fun review() {
        lock.withLock {
            val current = mutableState.value
            if (!current.canReview) return
            mutableState.update { it.copy(busy = true, actionError = null) }
            when (val result = api.result(DiscoverEndpoints.seriesPreview, request(current, null), "id" to id)) {
                is ApiResult.Failed -> mutableState.update { it.copy(busy = false, actionError = result.error) }
                is ApiResult.Ok -> {
                    token = result.value.reviewToken
                    val rows =
                        result.value.titles
                            .filter { it.selected || it.id in current.selected }
                            .map { SeriesReviewRow(it.movie(), it.skip) }
                    mutableState.update { it.copy(busy = false, review = rows) }
                }
            }
        }
    }

    suspend fun apply() {
        lock.withLock {
            val current = mutableState.value
            if (token.isEmpty() || current.review == null) return
            mutableState.update { it.copy(busy = true, actionError = null) }
            when (val result = api.result(DiscoverEndpoints.seriesRemove, request(current, token), "id" to id)) {
                is ApiResult.Failed -> {
                    token = ""
                    mutableState.update { it.copy(busy = false, review = null, actionError = result.error) }
                    reloadKeepingError()
                }
                is ApiResult.Ok -> {
                    token = ""
                    val outcomes =
                        result.value.results.map {
                            SeriesOutcomeRow(
                                name = it.name,
                                outcome =
                                    when (it.status) {
                                        "removed" -> SeriesOutcome.REMOVED
                                        "skipped" -> SeriesOutcome.SKIPPED
                                        else -> SeriesOutcome.FAILED
                                    },
                                why = it.error,
                            )
                        }
                    mutableState.update { it.copy(busy = false, review = null, results = outcomes) }
                }
            }
        }
    }

    private suspend fun reloadKeepingError() {
        val result = api.result(DiscoverEndpoints.series, "id" to id)
        if (result is ApiResult.Ok) {
            val movies = result.value.titles.map { it.movie() }
            mutableState.update {
                it.copy(movies = movies, selected = it.selected.intersect(movies.map { m -> m.id }.toSet()))
            }
        }
    }

    private fun request(
        state: SeriesState,
        token: String?,
    ) = SeriesRemoveRequest(
        titleIds = state.selected.sorted(),
        deleteFiles = state.deleteFiles,
        neverAdd = state.neverAdd,
        acknowledgeProtection = state.deleteFiles && state.acknowledge,
        reviewToken = token,
    )
}
