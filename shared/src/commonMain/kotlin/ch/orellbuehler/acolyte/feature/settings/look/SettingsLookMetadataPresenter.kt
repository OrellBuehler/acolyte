package ch.orellbuehler.acolyte.feature.settings.look

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SettingsLookEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsLookKeyTest
import ch.orellbuehler.acolyte.api.model.SettingsLookKeys
import ch.orellbuehler.acolyte.api.model.SettingsLookMetadata
import ch.orellbuehler.acolyte.api.model.SettingsLookMetadataChange
import ch.orellbuehler.acolyte.api.model.SettingsLookMetadataHealth
import ch.orellbuehler.acolyte.api.model.SettingsLookSource
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

internal const val METADATA_MASK = "••••••••"

private const val MAX_KEY_LENGTH = 4096
private const val MAX_HOST_LENGTH = 253

internal val METADATA_MODES: List<String> = listOf("full", "own", "off")

internal val METADATA_KEY_NAMES: List<String> = listOf("tmdb_token", "tvdb_key", "tvdb_pin", "mdblist_key", "fanart_key", "omdb_key")

private val KEY_LINKS: Map<String, String> =
    mapOf(
        "tmdb_token" to "https://www.themoviedb.org/settings/api",
        "tvdb_key" to "https://thetvdb.com/dashboard/account/apikey",
        "mdblist_key" to "https://mdblist.com/preferences/",
        "fanart_key" to "https://fanart.tv/get-an-api-key/",
    )

private val HOST = Regex("^[A-Za-z0-9.:-]+$")

data class SettingsLookKeyResult(
    val ok: Boolean,
    val code: String,
    val message: String,
    val error: ApiError?,
)

data class SettingsLookKeyField(
    val name: String,
    val saved: Boolean = false,
    val text: String = "",
    val fromDocker: Boolean = false,
    val testing: Boolean = false,
    val result: SettingsLookKeyResult? = null,
) {
    val stored: Boolean get() = saved && text == METADATA_MASK

    val removing: Boolean get() = saved && text.isEmpty()

    val changed: Boolean get() = text != if (saved) METADATA_MASK else ""

    val isPin: Boolean get() = name == "tvdb_pin"

    val link: String get() = KEY_LINKS[name].orEmpty()

    val shownText: String get() = if (text == METADATA_MASK) "" else text
}

data class SettingsLookHealth(
    val up: Boolean,
    val fakeHost: String,
)

data class SettingsLookMetadataState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val mode: String = "full",
    val locked: Boolean = false,
    val dockerValue: String = "",
    val sources: List<SettingsLookSource> = emptyList(),
    val hosts: List<String> = emptyList(),
    val keys: List<SettingsLookKeyField> = emptyList(),
    val tvdbOpen: Boolean = false,
    val health: SettingsLookHealth? = null,
    val healthError: ApiError? = null,
    val checking: Boolean = false,
    val modeSaving: Boolean = false,
    val modeError: ApiError? = null,
    val modeSavedCount: Int = 0,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val keysSavedCount: Int = 0,
) {
    val canEdit: Boolean get() = allowed

    val dirty: Boolean get() = keys.any { it.changed }

    val canSave: Boolean get() = canEdit && dirty && !saving

    val canChooseMode: Boolean get() = canEdit && !locked && !modeSaving

    val ownMode: Boolean get() = mode == "own"

    val fullMode: Boolean get() = mode == "full"

    val offSources: List<SettingsLookSource> get() = if (fullMode) emptyList() else sources.filter { it.state != "on" }

    val tvdbClosed: Boolean get() = fullMode && !tvdbSaved && !tvdbOpen

    private val tvdbSaved: Boolean get() = keys.firstOrNull { it.name == "tvdb_key" }?.saved == true

    val shownKeys: List<SettingsLookKeyField>
        get() {
            val names =
                when {
                    ownMode -> listOf("tmdb_token", "tvdb_key", "tvdb_pin", "mdblist_key", "fanart_key", "omdb_key")
                    fullMode && tvdbClosed -> listOf("omdb_key")
                    fullMode -> listOf("tvdb_key", "tvdb_pin", "omdb_key")
                    else -> emptyList()
                }
            return names.mapNotNull { name -> keys.firstOrNull { it.name == name } }
        }
}

class SettingsLookMetadataPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsLookMetadataState())
    private val lock = Mutex()

    val state: StateFlow<SettingsLookMetadataState> = mutableState.asStateFlow()

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
            if (mutableState.value.dirty) return
            mutableState.update { it.copy(status = it.status.started(), allowed = true, checking = true) }
            coroutineScope {
                val health = async { api.result(SettingsLookEndpoints.metadataHealth) }
                when (val result = api.result(SettingsLookEndpoints.metadata)) {
                    is ApiResult.Ok -> mutableState.update { take(it, result.value, keepEdits = false).copy(status = LoadStatus.ready) }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
                applyHealth(health.await())
            }
        }
    }

    suspend fun checkAgain() {
        if (mutableState.value.checking) return
        mutableState.update { it.copy(checking = true) }
        applyHealth(api.result(SettingsLookEndpoints.metadataHealth))
    }

    suspend fun chooseMode(mode: String) {
        val saved =
            lock.withLock {
                val s = mutableState.value
                if (!s.canChooseMode || mode !in METADATA_MODES || mode == s.mode) return@withLock false
                mutableState.update { it.copy(mode = mode, modeSaving = true, modeError = null) }
                when (val result = api.result(SettingsLookEndpoints.changeMetadata, SettingsLookMetadataChange(mode = mode))) {
                    is ApiResult.Ok -> {
                        mutableState.update {
                            take(it, result.value, keepEdits = mode != "off").copy(
                                modeSaving = false,
                                modeSavedCount =
                                    it.modeSavedCount + 1,
                            )
                        }
                        true
                    }
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(mode = s.mode, modeSaving = false, modeError = result.error) }
                        false
                    }
                }
            }
        if (saved) checkAgain()
    }

    fun openTvdb() = mutableState.update { it.copy(tvdbOpen = true) }

    fun setKey(
        name: String,
        text: String,
    ) = editKey(name) { it.copy(text = text.filterNot { c -> c == '\n' || c == '\r' }.take(MAX_KEY_LENGTH), result = null) }

    fun clearKey(name: String) = editKey(name) { it.copy(text = "", result = null) }

    fun restoreKey(name: String) = editKey(name) { it.copy(text = if (it.saved) METADATA_MASK else "", result = null) }

    fun discard() =
        mutableState.update { s ->
            s.copy(keys = s.keys.map { it.copy(text = if (it.saved) METADATA_MASK else "", result = null) }, saveError = null)
        }

    suspend fun testKey(name: String) {
        val s = mutableState.value
        val field = s.keys.firstOrNull { it.name == name } ?: return
        if (!s.canEdit || field.isPin || field.testing || field.fromDocker) return
        val pin =
            if (name == "tvdb_key") {
                s.keys
                    .firstOrNull { it.isPin }
                    ?.text
                    .orEmpty()
                    .trim()
            } else {
                ""
            }
        editKey(name) { it.copy(testing = true, result = null) }
        val result =
            when (val answer = api.result(SettingsLookEndpoints.testKey, SettingsLookKeyTest(name, field.text.trim(), pin))) {
                is ApiResult.Ok -> SettingsLookKeyResult(answer.value.ok, answer.value.code, answer.value.message, null)
                is ApiResult.Failed -> SettingsLookKeyResult(false, "", "", answer.error)
            }
        editKey(name) { it.copy(testing = false, result = result) }
    }

    suspend fun save() {
        lock.withLock {
            if (!mutableState.value.canSave) return
            val keys = mutableState.value.keys
            mutableState.update { it.copy(saving = true, saveError = null) }
            when (val result = api.result(SettingsLookEndpoints.changeMetadata, SettingsLookMetadataChange(keys = body(keys)))) {
                is ApiResult.Ok ->
                    mutableState.update {
                        take(it, result.value, keepEdits = false).copy(saving = false, keysSavedCount = it.keysSavedCount + 1)
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(saving = false, saveError = result.error) }
            }
        }
    }

    private fun body(keys: List<SettingsLookKeyField>): SettingsLookKeys {
        fun text(name: String) =
            keys
                .firstOrNull { it.name == name }
                ?.text
                .orEmpty()
                .trim()
        return SettingsLookKeys(
            tmdbToken = text("tmdb_token"),
            tvdbKey = text("tvdb_key"),
            tvdbPin = text("tvdb_pin"),
            mdblistKey = text("mdblist_key"),
            fanartKey = text("fanart_key"),
            omdbKey = text("omdb_key"),
        )
    }

    private fun take(
        state: SettingsLookMetadataState,
        answer: SettingsLookMetadata,
        keepEdits: Boolean,
    ): SettingsLookMetadataState {
        val fields =
            METADATA_KEY_NAMES.map { name ->
                val saved = answer.keys[name] == true
                val before = state.keys.firstOrNull { it.name == name }
                val text =
                    if (keepEdits && before != null && before.changed) {
                        before.text
                    } else if (saved) {
                        METADATA_MASK
                    } else {
                        ""
                    }
                SettingsLookKeyField(name, saved, text, fromDocker = name == "tmdb_token" && answer.tmdbKeyFromDocker)
            }
        return state.copy(
            mode = answer.mode.ifEmpty { "full" },
            locked = answer.locked,
            dockerValue = answer.dockerValue,
            sources = answer.sources,
            hosts = answer.hosts,
            keys = fields,
            saveError = null,
        )
    }

    private fun applyHealth(result: ApiResult<SettingsLookMetadataHealth>) {
        when (result) {
            is ApiResult.Ok ->
                mutableState.update {
                    val host =
                        result.value.fakeIp
                            .takeIf { h ->
                                !result.value.up && h.length <= MAX_HOST_LENGTH && HOST.matches(h)
                            }.orEmpty()
                    it.copy(health = SettingsLookHealth(result.value.up, host), healthError = null, checking = false)
                }
            is ApiResult.Failed -> mutableState.update { it.copy(healthError = result.error, checking = false) }
        }
    }

    private fun editKey(
        name: String,
        transform: (SettingsLookKeyField) -> SettingsLookKeyField,
    ) = mutableState.update { s ->
        s.copy(
            keys =
                s.keys.map { field ->
                    if (field.name != name || field.fromDocker) field else transform(field)
                },
            saveError = null,
        )
    }
}
