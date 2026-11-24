package ch.orellbuehler.acolyte.feature.sources

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.SourcesEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsBridge
import ch.orellbuehler.acolyte.api.model.SettingsBridgeBody
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.feature.acquisition.mayEditAcquisition
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class SettingsBridgeDraft(
    val kind: String = KIND_JACKETT,
    val url: String = "",
    val key: String = "",
    val callbackUrl: String = "",
    val newKeys: Boolean = false,
    val sonarrKey: String = "",
    val radarrKey: String = "",
    val editingId: Long = 0,
) {
    val isProwlarr: Boolean get() = kind == KIND_PROWLARR

    val canSubmit: Boolean get() = url.isNotBlank() && key.isNotBlank()

    val defaultPort: String get() = if (isProwlarr) "9696" else "9117"

    internal fun body(): SettingsBridgeBody {
        val keys = mutableMapOf<String, String>()
        if (sonarrKey.isNotBlank()) keys["sonarr"] = sonarrKey.trim()
        if (radarrKey.isNotBlank()) keys["radarr"] = radarrKey.trim()
        return SettingsBridgeBody(
            kind = kind,
            url = url.trim(),
            key = key.trim(),
            callbackUrl = if (isProwlarr) callbackUrl.trim() else "",
            newKeys = newKeys,
            keys = keys,
        )
    }

    companion object {
        const val KIND_JACKETT = "jackett"
        const val KIND_PROWLARR = "prowlarr"
    }
}

data class SettingsBridgeResult(
    val ok: Boolean,
    val version: String,
    val siteCount: Int,
    val sites: List<String>,
    val text: String,
)

data class SettingsBridgeFormState(
    val initial: SettingsBridgeDraft = SettingsBridgeDraft(),
    val testing: Boolean = false,
    val connecting: Boolean = false,
    val result: SettingsBridgeResult? = null,
    val error: ApiError? = null,
    val kindError: String = "",
    val urlError: String = "",
    val keyError: String = "",
    val callbackError: String = "",
    val keysNeeded: List<String> = emptyList(),
) {
    val busy: Boolean get() = testing || connecting

    val needsSonarrKey: Boolean get() = "sonarr" in keysNeeded

    val needsRadarrKey: Boolean get() = "radarr" in keysNeeded
}

data class SettingsBridgeState(
    val status: LoadStatus = LoadStatus(),
    val canEdit: Boolean = false,
    val bridges: List<SettingsBridge> = emptyList(),
    val homeIp: String = "",
    val form: SettingsBridgeFormState? = null,
    val formToken: Int = 0,
    val syncing: Long = 0,
    val removing: Boolean = false,
    val removeError: ApiError? = null,
    val madeKeys: Map<String, String> = emptyMap(),
    val notice: SettingsSourcesNotice? = null,
    val changed: Int = 0,
) {
    val canAddAnother: Boolean get() = canEdit && form == null
}

class SettingsBridgePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
) {
    private val mutableState = MutableStateFlow(SettingsBridgeState())
    private val lock = Mutex()
    private var seq = 0

    val state: StateFlow<SettingsBridgeState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started(), canEdit = account()?.mayEditAcquisition == true) }
            when (val result = api.result(SourcesEndpoints.bridges)) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(status = LoadStatus.ready, bridges = result.value.bridges, homeIp = result.value.homeIp)
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    private fun notice(
        kind: SettingsSourcesNoticeKind,
        name: String = "",
        detail: String = "",
        count: Int = 0,
        added: Int = 0,
        removed: Int = 0,
        changed: Int = 0,
        error: ApiError? = null,
    ) {
        seq++
        mutableState.update {
            it.copy(notice = SettingsSourcesNotice(seq, kind, name, detail, count, added, removed, changed, error))
        }
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    fun dismissKeys() {
        mutableState.update { it.copy(madeKeys = emptyMap()) }
    }

    fun startConnect(kind: String) {
        val s = mutableState.value
        if (!s.canEdit) return
        val callback = SettingsSourcesLogic.callbackAddress(account()?.serverUrl.orEmpty(), s.homeIp, "")
        open(SettingsBridgeDraft(kind = kind, callbackUrl = callback))
    }

    fun startChange(id: Long) {
        val s = mutableState.value
        val bridge = s.bridges.firstOrNull { it.id == id } ?: return
        if (!s.canEdit) return
        val callback = SettingsSourcesLogic.callbackAddress(account()?.serverUrl.orEmpty(), s.homeIp, bridge.callbackUrl)
        open(SettingsBridgeDraft(kind = bridge.kind, url = bridge.url, callbackUrl = callback, editingId = bridge.id))
    }

    private fun open(draft: SettingsBridgeDraft) {
        mutableState.update { it.copy(form = SettingsBridgeFormState(initial = draft), formToken = it.formToken + 1) }
    }

    fun cancelForm() {
        mutableState.update { it.copy(form = null) }
    }

    private fun form(change: (SettingsBridgeFormState) -> SettingsBridgeFormState) {
        mutableState.update { s -> s.form?.let { s.copy(form = change(it)) } ?: s }
    }

    suspend fun test(draft: SettingsBridgeDraft) {
        val current = mutableState.value.form ?: return
        if (current.busy || !draft.canSubmit) return
        form { it.cleared().copy(testing = true) }
        when (val result = reauth.guard { api.result(SourcesEndpoints.testBridge, draft.body()) }) {
            is ApiResult.Ok -> {
                val answer = result.value
                val have =
                    answer.keys
                        .filterValues { it == "have" }
                        .keys
                        .toList()
                val shown =
                    SettingsBridgeResult(
                        ok = answer.ok,
                        version = SettingsSourcesLogic.version(answer.text),
                        siteCount = answer.sites.size,
                        sites = SettingsSourcesLogic.siteSample(answer.sites),
                        text = answer.text,
                    )
                form { it.copy(testing = false, result = shown, keysNeeded = have) }
            }
            is ApiResult.Failed -> failed(result.error)
        }
    }

    suspend fun link(draft: SettingsBridgeDraft): Boolean {
        val current = mutableState.value.form ?: return false
        if (current.busy || !draft.canSubmit) return false
        form { it.cleared().copy(connecting = true) }
        return when (val result = reauth.guard { api.result(SourcesEndpoints.linkBridge, draft.body()) }) {
            is ApiResult.Ok -> {
                val linked = result.value
                if (draft.editingId != 0L && linked.id != draft.editingId) {
                    api.result(SourcesEndpoints.unlinkBridge, "id" to draft.editingId, "keep_sources" to null)
                }
                notice(
                    SettingsSourcesNoticeKind.BRIDGE_CONNECTED,
                    name = linked.kind.replaceFirstChar { it.uppercase() },
                    detail = linked.kind,
                    count = linked.sources,
                )
                mutableState.update { it.copy(form = null, madeKeys = linked.keys, changed = it.changed + 1) }
                refresh()
                true
            }
            is ApiResult.Failed -> {
                failed(result.error)
                false
            }
        }
    }

    private fun failed(error: ApiError) {
        val parsed = parse(error)
        val sides = if (parsed.need == "keys") parsed.sides else emptyList()
        form {
            val base = it.copy(testing = false, connecting = false)
            when {
                sides.isNotEmpty() -> base.copy(keysNeeded = sides, error = error)
                parsed.field == "kind" -> base.copy(kindError = error.message)
                parsed.field == "url" -> base.copy(urlError = error.message)
                parsed.field == "key" -> base.copy(keyError = error.message)
                parsed.field == "callback_url" -> base.copy(callbackError = error.message)
                else -> base.copy(error = error)
            }
        }
    }

    suspend fun sync(id: Long) {
        if (mutableState.value.syncing != 0L) return
        mutableState.update { it.copy(syncing = id) }
        val name =
            mutableState.value.bridges
                .firstOrNull { it.id == id }
                ?.kind
                .orEmpty()
                .replaceFirstChar { it.uppercase() }
        when (val result = api.result(SourcesEndpoints.syncBridge, "id" to id)) {
            is ApiResult.Ok -> {
                val res = result.value.result
                when {
                    res == null -> notice(SettingsSourcesNoticeKind.BRIDGE_SYNCING_AGAIN, name = name)
                    res.added + res.removed + res.updated > 0 ->
                        notice(
                            SettingsSourcesNoticeKind.BRIDGE_SYNCED,
                            name = name,
                            added = res.added,
                            removed = res.removed,
                            changed = res.updated,
                        )
                    else -> notice(SettingsSourcesNoticeKind.BRIDGE_UP_TO_DATE, name = name)
                }
            }
            is ApiResult.Failed -> notice(SettingsSourcesNoticeKind.BRIDGE_SYNC_FAILED, name = name, error = result.error)
        }
        mutableState.update { it.copy(syncing = 0, changed = it.changed + 1) }
        refresh()
    }

    suspend fun unlink(
        id: Long,
        keepSources: Boolean,
    ): Boolean {
        val name =
            mutableState.value.bridges
                .firstOrNull { it.id == id }
                ?.kind
                .orEmpty()
                .replaceFirstChar { it.uppercase() }
        mutableState.update { it.copy(removing = true, removeError = null) }
        val result = api.result(SourcesEndpoints.unlinkBridge, "id" to id, "keep_sources" to if (keepSources) "true" else null)
        return when (result) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(removing = false, changed = it.changed + 1) }
                notice(SettingsSourcesNoticeKind.BRIDGE_DISCONNECTED, name = name)
                refresh()
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(removing = false, removeError = result.error) }
                false
            }
        }
    }

    fun clearRemoveError() {
        mutableState.update { it.copy(removeError = null) }
    }

    private fun SettingsBridgeFormState.cleared(): SettingsBridgeFormState =
        copy(result = null, error = null, kindError = "", urlError = "", keyError = "", callbackError = "")

    private data class Parsed(
        val field: String = "",
        val need: String = "",
        val sides: List<String> = emptyList(),
    )

    private fun parse(error: ApiError): Parsed {
        val json =
            try {
                ApiJson.lenient.parseToJsonElement(error.body) as? JsonObject
            } catch (e: IllegalArgumentException) {
                null
            } ?: return Parsed()
        val sides = (json["sides"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
        return Parsed(
            field = (json["field"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            need = (json["need"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            sides = sides,
        )
    }
}
