package ch.orellbuehler.acolyte.feature.sources

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.SourcesEndpoints
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.mayEditAcquisition
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Duration.Companion.seconds

data class SettingsSourceHealthState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canEdit: Boolean = false,
    val row: SettingsSourceRow? = null,
    val days: Int = DEFAULT_DAYS,
    val bars: List<SettingsSourceBar> = emptyList(),
    val recent: List<SettingsSourceCallRow> = emptyList(),
    val totalCalls: Int = 0,
    val totalFailures: Int = 0,
    val totalGrabs: Int = 0,
    val trying: Boolean = false,
) {
    val hasCalls: Boolean get() = totalCalls > 0

    val hasTimes: Boolean get() = bars.any { it.avgMs > 0 }

    val choices: List<Int> get() = DAY_CHOICES

    companion object {
        const val DEFAULT_DAYS = 14
        val DAY_CHOICES: List<Int> = listOf(7, 14, 30, 90)
    }
}

class SettingsSourceHealthPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val id: Long,
) {
    private val mutableState = MutableStateFlow(SettingsSourceHealthState())
    private val lock = Mutex()
    private var outcome: SettingsSourceOutcome? = null

    val state: StateFlow<SettingsSourceHealthState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() = reload(quiet = false)

    private suspend fun reload(quiet: Boolean) {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(AcquisitionPage.SOURCES)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update {
                it.copy(status = if (quiet) it.status else it.status.started(), allowed = true, canEdit = me.mayEditAcquisition)
            }
            val days = mutableState.value.days
            when (val result = api.result(SourcesEndpoints.stats, "id" to id, "days" to days)) {
                is ApiResult.Ok -> {
                    val stats = result.value
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            row = settingsSourceRow(stats.source, outcome, it.trying, false),
                            bars = SettingsSourcesLogic.bars(stats.days),
                            recent = SettingsSourcesLogic.callRows(stats.recent),
                            totalCalls = stats.days.sumOf { d -> d.calls },
                            totalFailures = stats.days.sumOf { d -> d.failures },
                            totalGrabs = stats.days.sumOf { d -> d.grabs },
                        )
                    }
                }
                is ApiResult.Failed -> if (!quiet) mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    suspend fun setDays(days: Int) {
        if (days !in DAY_VALUES || days == mutableState.value.days) return
        mutableState.update { it.copy(days = days) }
        reload(quiet = true)
    }

    suspend fun tryNow() {
        val s = mutableState.value
        if (s.trying || !s.canEdit) return
        mutableState.update { it.copy(trying = true, row = it.row?.copy(testing = true)) }
        outcome =
            when (val result = api.result(SourcesEndpoints.tryNow, "id" to id)) {
                is ApiResult.Ok -> SettingsSourcesLogic.outcome(result.value)
                is ApiResult.Failed -> SettingsSourcesLogic.failedOutcome(result.error)
            }
        mutableState.update { it.copy(trying = false) }
        reload(quiet = true)
    }

    suspend fun watch() {
        coroutineScope {
            val poke = Channel<Unit>(Channel.CONFLATED)
            launch {
                api.live.collect { event ->
                    val mine = event.topic == SettingsSourcesPresenter.TOPIC && idOf(event) in setOf(id, 0L)
                    if (mine || event.topic == LiveEvent.RESYNC) poke.trySend(Unit)
                }
            }
            launch {
                for (ignored in poke) {
                    delay(1.seconds)
                    if (mutableState.value.status.phase == Phase.READY) reload(quiet = true)
                }
            }
        }
    }

    private fun idOf(event: LiveEvent): Long = ((event.data as? JsonObject)?.get("id") as? JsonPrimitive)?.longOrNull ?: 0L

    private companion object {
        val DAY_VALUES = SettingsSourceHealthState.DAY_CHOICES.toSet()
    }
}
