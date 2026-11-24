package ch.orellbuehler.acolyte.feature.settings.server

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsServerEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsServerAlert
import ch.orellbuehler.acolyte.api.model.SettingsServerPackSettings
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SettingsServerAlertsNoticeKind {
    SAVED,
    REMOVE_FAILED,
    PACK_SAVED,
    PACK_FAILED,
}

data class SettingsServerAlertsNotice(
    val seq: Int,
    val kind: SettingsServerAlertsNoticeKind,
    val name: String = "",
    val error: ApiError? = null,
)

enum class SettingsServerTestKind {
    SENT,
    TYPE_FIRST,
    FAILED,
}

data class SettingsServerTestResult(
    val kind: SettingsServerTestKind,
    val error: ApiError? = null,
)

data class SettingsServerAlertRow(
    val id: Long,
    val name: String,
    val kind: String,
    val enabled: Boolean,
    val moments: List<String>,
    val missing: SettingsServerMissing,
    val lastError: String,
    val lastSentAt: String,
    val test: SettingsServerTestResult?,
    val testing: Boolean,
    val editable: Boolean,
)

data class SettingsServerPackState(
    val style: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: ApiError? = null,
)

data class SettingsServerAlertsState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val alerts: List<SettingsServerAlertRow> = emptyList(),
    val form: SettingsServerAlertForm? = null,
    val pack: SettingsServerPackState = SettingsServerPackState(),
    val notice: SettingsServerAlertsNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && alerts.isEmpty()

    val showPack: Boolean get() = allowed && !(status.phase == Phase.FAILED && pack.error != null)
}

class SettingsServerAlertsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsServerAlertsState())
    private val lock = Mutex()
    private var raw: List<SettingsServerAlert> = emptyList()
    private var results: Map<Long, SettingsServerTestResult> = emptyMap()
    private var testing: Long = 0
    private var seq = 0

    val state: StateFlow<SettingsServerAlertsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.can(Need.SETTINGS)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started(), allowed = true) }
            coroutineScope {
                val pack = async { fetchPack() }
                fetchAlerts()
                pack.await()
            }
        }
    }

    suspend fun retryPack() {
        lock.withLock { fetchPack() }
    }

    private suspend fun fetchAlerts() {
        when (val result = api.result(SettingsServerEndpoints.alerts)) {
            is ApiResult.Ok -> {
                raw = result.value.alerts
                mutableState.update { it.copy(status = LoadStatus.ready) }
                rebuild()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    private suspend fun fetchPack() {
        mutableState.update { it.copy(pack = it.pack.copy(loading = true, error = null)) }
        when (val result = api.result(SettingsServerEndpoints.packSettings)) {
            is ApiResult.Ok ->
                mutableState.update { it.copy(pack = SettingsServerPackState(style = result.value.packAlerts, loading = false)) }
            is ApiResult.Failed -> mutableState.update { it.copy(pack = it.pack.copy(loading = false, error = result.error)) }
        }
    }

    private fun rebuild() {
        val rows =
            raw.map { a ->
                SettingsServerAlertRow(
                    id = a.id,
                    name = a.name,
                    kind = a.kind,
                    enabled = a.enabled,
                    moments = a.moments,
                    missing = missingFor(a),
                    lastError = a.lastError,
                    lastSentAt = a.lastSentAt,
                    test = results[a.id],
                    testing = testing == a.id,
                    editable = SettingsServerAlertKind.of(a.kind) != null,
                )
            }
        mutableState.update { it.copy(alerts = rows) }
    }

    private fun notice(
        kind: SettingsServerAlertsNoticeKind,
        name: String = "",
        error: ApiError? = null,
    ): SettingsServerAlertsNotice = SettingsServerAlertsNotice(++seq, kind, name, error)

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    fun startAdd() {
        mutableState.update { it.copy(form = SettingsServerAlertForm.blank()) }
    }

    fun startChange(id: Long) {
        val alert = raw.firstOrNull { it.id == id } ?: return
        if (SettingsServerAlertKind.of(alert.kind) == null) return
        mutableState.update { it.copy(form = SettingsServerAlertForm.from(alert)) }
    }

    fun cancelForm() {
        mutableState.update { if (it.form?.saving == true) it else it.copy(form = null) }
    }

    private fun edit(change: (SettingsServerAlertForm) -> SettingsServerAlertForm) {
        mutableState.update { s -> s.form?.let { s.copy(form = change(it)) } ?: s }
    }

    fun setText(
        field: SettingsServerAlertField,
        value: String,
    ) = edit { it.withText(field, value) }

    fun setKind(kind: SettingsServerAlertKind) = edit { if (it.isNew) it.withKind(kind) else it }

    fun setEnabled(on: Boolean) = edit { it.copy(enabled = on) }

    fun setAllowLocalNetwork(on: Boolean) = edit { it.copy(allowLocalNetwork = on) }

    fun setClearToken(on: Boolean) = edit { it.withClearToken(on) }

    fun setMoment(
        id: String,
        on: Boolean,
    ) = edit { it.withMoment(id, on) }

    fun setGroup(
        key: String,
        on: Boolean,
    ) = edit { it.withGroup(key, on) }

    suspend fun save() {
        lock.withLock {
            val form = mutableState.value.form ?: return
            if (!form.canSave) return
            mutableState.update { s -> s.copy(form = s.form?.copy(saving = true, error = null)) }
            val body = form.toInput()
            val result =
                if (form.isNew) {
                    api.result(SettingsServerEndpoints.addAlert, body)
                } else {
                    api.result(SettingsServerEndpoints.changeAlert, body, "id" to form.id)
                }
            when (result) {
                is ApiResult.Ok -> {
                    mutableState.update { it.copy(form = null, notice = notice(SettingsServerAlertsNoticeKind.SAVED, form.name.trim())) }
                    fetchAlerts()
                }
                is ApiResult.Failed -> mutableState.update { s -> s.copy(form = s.form?.copy(saving = false, error = result.error)) }
            }
        }
    }

    suspend fun test(id: Long) {
        lock.withLock {
            val alert = raw.firstOrNull { it.id == id } ?: return
            if (missingFor(alert) != SettingsServerMissing.NOTHING) {
                results = results + (id to SettingsServerTestResult(SettingsServerTestKind.TYPE_FIRST))
                rebuild()
                return
            }
            testing = id
            rebuild()
            val result = api.result(SettingsServerEndpoints.testAlert, "id" to id)
            testing = 0
            results =
                results +
                (
                    id to
                        when (result) {
                            is ApiResult.Ok -> SettingsServerTestResult(SettingsServerTestKind.SENT)
                            is ApiResult.Failed -> SettingsServerTestResult(SettingsServerTestKind.FAILED, result.error)
                        }
                )
            rebuild()
            fetchAlerts()
        }
    }

    suspend fun remove(id: Long) {
        lock.withLock {
            when (val result = api.result(SettingsServerEndpoints.removeAlert, "id" to id)) {
                is ApiResult.Ok -> {
                    results = results - id
                    raw = raw.filter { it.id != id }
                    rebuild()
                    fetchAlerts()
                }
                is ApiResult.Failed ->
                    mutableState.update { it.copy(notice = notice(SettingsServerAlertsNoticeKind.REMOVE_FAILED, error = result.error)) }
            }
        }
    }

    suspend fun setPack(style: String) {
        lock.withLock {
            val current = mutableState.value.pack
            if (current.saving || current.loading || current.style == style) return
            mutableState.update { it.copy(pack = it.pack.copy(saving = true)) }
            when (val result = api.result(SettingsServerEndpoints.savePackSettings, SettingsServerPackSettings(packAlerts = style))) {
                is ApiResult.Ok -> {
                    val saved = result.value.packAlerts.ifEmpty { style }
                    mutableState.update {
                        it.copy(
                            pack = SettingsServerPackState(style = saved, loading = false),
                            notice = notice(SettingsServerAlertsNoticeKind.PACK_SAVED, saved),
                        )
                    }
                }
                is ApiResult.Failed ->
                    mutableState.update {
                        it.copy(
                            pack = it.pack.copy(saving = false),
                            notice = notice(SettingsServerAlertsNoticeKind.PACK_FAILED, error = result.error),
                        )
                    }
            }
        }
    }
}
