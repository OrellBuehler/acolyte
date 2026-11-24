package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.KindStats
import ch.orellbuehler.acolyte.api.model.TitleRemove
import ch.orellbuehler.acolyte.api.model.WatchedCleanup
import ch.orellbuehler.acolyte.api.model.WatchedSummary
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StatsBucketRow(
    val value: String,
    val files: Int,
    val bytes: Long,
    val share: Float,
)

data class StatsFact(
    val name: String,
    val buckets: List<StatsBucketRow>,
)

data class StatsKind(
    val kind: String,
    val titles: Int,
    val files: Int,
    val bytes: Long,
    val unchecked: Int,
    val facts: List<StatsFact>,
)

data class LibraryStatsState(
    val status: LoadStatus = LoadStatus(),
    val kinds: List<StatsKind> = emptyList(),
    val kind: String = "",
    val watchedStatus: LoadStatus = LoadStatus(),
    val watched: WatchedSummary = WatchedSummary(),
    val isOwner: Boolean = false,
    val canClean: Boolean = false,
    val cleanup: WatchedCleanup? = null,
    val cleanupDays: Int = DEFAULT_DAYS,
    val cleanupPicked: List<String> = emptyList(),
    val cleanupLoading: Boolean = false,
    val removing: Long = 0,
    val notice: LibraryNotice? = null,
) {
    val current: StatsKind? get() = kinds.firstOrNull { it.kind == kind } ?: kinds.firstOrNull()
    val isEmpty: Boolean get() = status.phase == Phase.READY && kinds.all { it.files == 0 }

    companion object {
        const val DEFAULT_DAYS = 30
    }
}

class LibraryStatsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(LibraryStatsState())
    private val lock = Mutex()
    private val notices = Notices()

    val state: StateFlow<LibraryStatsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            val canClean = me != null && (me.role == Role.OWNER || me.role == Role.ADMIN) && me.can(Need.USERS)
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    watchedStatus = it.watchedStatus.started(),
                    isOwner = me?.role == Role.OWNER,
                    canClean = canClean,
                )
            }
            coroutineScope {
                val stats = async { api.result(LibraryEndpoints.stats) }
                val watched = async { api.result(LibraryEndpoints.statsWatched) }
                when (val result = stats.await()) {
                    is ApiResult.Ok -> {
                        val kinds =
                            result.value.kinds
                                .filter { kindOn(me, it.kind) }
                                .map { build(it) }
                        mutableState.update {
                            it.copy(
                                status = LoadStatus.ready,
                                kinds = kinds,
                                kind = it.kind.ifEmpty { kinds.firstOrNull()?.kind.orEmpty() },
                            )
                        }
                    }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
                when (val result = watched.await()) {
                    is ApiResult.Ok -> mutableState.update { it.copy(watchedStatus = LoadStatus.ready, watched = result.value) }
                    is ApiResult.Failed -> mutableState.update { it.copy(watchedStatus = it.watchedStatus.failed(result.error)) }
                }
            }
        }
        val current = mutableState.value
        if (current.canClean && current.watched.enabled && current.watched.people.isNotEmpty() && current.cleanup == null) loadCleanup(null)
    }

    private fun kindOn(
        me: Account?,
        kind: String,
    ): Boolean =
        when (kind) {
            "movies" -> me?.isOn(Part.MOVIES) != false
            "shows" -> me?.isOn(Part.SHOWS) != false
            "music" -> me?.isOn(Part.MUSIC) != false
            else -> true
        }

    private fun build(kind: KindStats): StatsKind {
        val vague = setOf("unknown", "unchecked", "unsupported")
        val facts =
            FACTS.mapNotNull { name ->
                val buckets = kind.facts[name].orEmpty()
                if (buckets.isEmpty()) return@mapNotNull null
                StatsFact(
                    name,
                    buckets.map {
                        StatsBucketRow(
                            it.value,
                            it.files,
                            it.bytes,
                            if (kind.files >
                                0
                            ) {
                                (it.files.toFloat() / kind.files).coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                        )
                    },
                )
            }
        val clear = facts.filter { fact -> fact.buckets.any { it.value !in vague } }
        val unchecked = (kind.facts["resolution"] ?: kind.facts["container"]).orEmpty().firstOrNull { it.value == "unchecked" }?.files ?: 0
        return StatsKind(kind.kind, kind.titles, kind.files, kind.bytes, unchecked, clear.ifEmpty { facts.take(1) })
    }

    fun setKind(kind: String) {
        mutableState.update { it.copy(kind = kind) }
    }

    suspend fun setCleanupDays(days: Int) {
        mutableState.update { it.copy(cleanupDays = days.coerceIn(0, MAX_DAYS)) }
        loadCleanup(mutableState.value.cleanupPicked)
    }

    suspend fun toggleCleanupPerson(key: String) {
        val picked = mutableState.value.cleanupPicked
        val next = if (key in picked) picked - key else picked + key
        mutableState.update { it.copy(cleanupPicked = next) }
        loadCleanup(next)
    }

    private suspend fun loadCleanup(picked: List<String>?) {
        val current = mutableState.value
        if (picked != null && picked.isEmpty()) {
            mutableState.update { it.copy(cleanup = it.cleanup?.copy(found = emptyList())) }
            return
        }
        val everyone =
            picked != null &&
                current.cleanup
                    ?.people
                    ?.map { it.key }
                    ?.toSet() == picked.toSet()
        mutableState.update { it.copy(cleanupLoading = true) }
        val result =
            api.result(
                LibraryEndpoints.watchedCleanup,
                "days" to current.cleanupDays,
                "people" to if (picked == null || everyone) null else picked.joinToString(","),
            )
        when (result) {
            is ApiResult.Ok -> {
                val answer = result.value
                mutableState.update {
                    it.copy(
                        cleanupLoading = false,
                        cleanup = answer,
                        cleanupPicked = picked ?: answer.people.filter { p -> p.chosen }.map { p -> p.key },
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(cleanupLoading = false, notice = notices.failed(result.error)) }
        }
    }

    suspend fun removeWatched(
        id: Long,
        deleteFiles: Boolean,
        neverAdd: Boolean,
    ) {
        val name =
            mutableState.value.cleanup
                ?.found
                ?.firstOrNull { it.id == id }
                ?.name
                .orEmpty()
        mutableState.update { it.copy(removing = id) }
        when (val result = api.result(LibraryEndpoints.remove, TitleRemove(deleteFiles, neverAdd, apply = true), "id" to id)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        removing = 0,
                        cleanup = it.cleanup?.let { c -> c.copy(found = c.found.filter { t -> t.id != id }) },
                        notice = notices.make(LibraryNoticeKind.TITLE_DELETED, name = name),
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(removing = 0, notice = notices.failed(result.error)) }
        }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    private companion object {
        val FACTS = listOf("resolution", "video_codec", "hdr", "audio_codec", "audio_channels", "container")
        const val MAX_DAYS = 3650
    }
}
