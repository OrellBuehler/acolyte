package ch.orellbuehler.acolyte.feature.settings

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsHomeEndpoints
import ch.orellbuehler.acolyte.api.SettingsSharedEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsStatusAnswer
import ch.orellbuehler.acolyte.api.model.SettingsUpdateInfo
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.downloads.DownloadBadges
import ch.orellbuehler.acolyte.feature.requests.RequestsInbox
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.Role
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsHomeState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = false,
    val groups: List<SettingsHomeGroup> = emptyList(),
    val accountPages: List<SettingsPage> = emptyList(),
    val reasons: List<SettingsHomeReason> = emptyList(),
    val showFinishSetup: Boolean = false,
    val needsAttention: Boolean = false,
) {
    val pages: List<SettingsPage> get() = groups.flatMap { group -> group.pages.map { it.page } } + accountPages
}

class SettingsHomePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val badges: DownloadBadges,
    private val inbox: RequestsInbox,
) {
    private val lock = Mutex()
    private val mutableState = MutableStateFlow(compose(account(), null, 0, 0))
    private var answer: SettingsStatusAnswer? = null
    private var update: SettingsUpdateInfo? = null
    private var decisions = 0
    private var waiting = 0

    val state: StateFlow<SettingsHomeState> = mutableState.asStateFlow()

    suspend fun load() {
        refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val me = account()
            if (me != null && me.can(Need.SETTINGS)) {
                answer = (api.result(SettingsHomeEndpoints.status) as? ApiResult.Ok)?.value
            } else {
                answer = null
            }
            update =
                if (me != null && me.isManager && me.can(Need.MANAGE_LIBRARY)) {
                    (api.result(SettingsSharedEndpoints.updateInfo) as? ApiResult.Ok)?.value
                } else {
                    null
                }
            if (me != null && me.isManager) {
                badges.refresh()
                inbox.refresh()
            }
            publish(LoadStatus.ready)
        }
    }

    suspend fun watch() {
        coroutineScope {
            launch {
                badges.state.collect { badge ->
                    decisions = badge.decide
                    publish()
                }
            }
            launch {
                inbox.state.collect { snapshot ->
                    waiting = if (snapshot.approver) snapshot.waiting else 0
                    publish()
                }
            }
            launch {
                api.live.collect { event ->
                    if (event.topic == SOURCE_TOPIC || event.topic == LiveEvent.RESYNC) refresh()
                }
            }
            launch {
                while (true) {
                    delay(POLL_MILLIS)
                    refresh()
                }
            }
        }
    }

    private fun publish(status: LoadStatus? = null) {
        mutableState.update { current -> compose(account(), answer, decisions, waiting, update).copy(status = status ?: current.status) }
    }

    private fun compose(
        me: Account?,
        answer: SettingsStatusAnswer?,
        decisions: Int,
        waiting: Int,
        update: SettingsUpdateInfo? = null,
    ): SettingsHomeState {
        if (me == null) return SettingsHomeState()
        val groups = SettingsCatalog.groups(me)
        val updateNews = update != null && update.on && update.newer && update.latest.isNotEmpty()
        val items = answer?.items.orEmpty()
        val settingItems = if (updateNews) items.filterNot { SettingsHomeLogic.isUpdateNews(it) } else items
        val reasons = mutableListOf<SettingsHomeReason>()
        if (me.isManager) {
            reasons += SettingsHomeLogic.settingReasons(settingItems)
            if (updateNews && update != null) {
                reasons +=
                    SettingsHomeReason(
                        key = "update",
                        kind = SettingsHomeReasonKind.UPDATE,
                        tone = Tone.INFO,
                        page = SettingsPage.THIS_APP,
                        body = update.latest.removePrefix("v"),
                    )
            }
            if (decisions > 0) {
                reasons += SettingsHomeReason("decisions", SettingsHomeReasonKind.DECISIONS, Tone.WAIT, null, count = decisions)
            }
            if (waiting > 0) {
                reasons += SettingsHomeReason("requests", SettingsHomeReasonKind.REQUESTS, Tone.INFO, null, count = waiting)
            }
        }
        return SettingsHomeState(
            allowed = SettingsCatalog.hasHome(me),
            groups = SettingsHomeLogic.groups(groups, answer),
            accountPages = groups.filter { it.first == SettingsGroup.ACCOUNT }.flatMap { it.second },
            reasons = reasons,
            showFinishSetup = me.setupSkipped && me.role == Role.OWNER,
            needsAttention = me.can(Need.SETTINGS) && items.isNotEmpty(),
        )
    }

    private companion object {
        const val SOURCE_TOPIC = "source"
        const val POLL_MILLIS = 120_000L
    }
}
