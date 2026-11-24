package ch.orellbuehler.acolyte.feature.settings.look

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.SettingsLookEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.TitleAddDefaults
import ch.orellbuehler.acolyte.api.model.TitleGateDefaults
import ch.orellbuehler.acolyte.api.model.TitleKindDefaults
import ch.orellbuehler.acolyte.api.model.TitleShowDefaults
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.settings.SettingsCatalog
import ch.orellbuehler.acolyte.feature.settings.SettingsPage
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal val GATE_CHOICES: List<String> = listOf("announced", "in_cinemas", "released")
internal val SEASON_CHOICES: List<String> = listOf("all", "future", "latest", "none")
internal val NEW_SEASON_CHOICES: List<String> = listOf("all", "none")
internal val ORDER_CHOICES: List<String> = listOf("aired", "daily", "absolute")
internal val SOURCE_CHOICES: List<String> = listOf("tmdb", "tvmaze", "tvdb")

internal const val MAX_GATE_DAYS = 365

fun settingsLookGateDays(text: String): Int? = if (text.isBlank()) 0 else text.trim().toIntOrNull()?.takeIf { it in 0..MAX_GATE_DAYS }

data class SettingsLookOption(
    val id: Long,
    val name: String,
)

data class SettingsLookDefaultsState(
    val status: LoadStatus = LoadStatus(),
    val canEdit: Boolean = false,
    val canOpenFolders: Boolean = false,
    val canOpenRequests: Boolean = false,
    val canOpenOutsideLists: Boolean = false,
    val moviesOn: Boolean = true,
    val showsOn: Boolean = true,
    val readError: ApiError? = null,
    val folders: List<LibraryFolder> = emptyList(),
    val rules: List<PickRule> = emptyList(),
    val savedDefaults: TitleAddDefaults? = null,
    val defaults: TitleAddDefaults = TitleAddDefaults(),
    val savedGate: TitleGateDefaults? = null,
    val gate: TitleGateDefaults = TitleGateDefaults(),
    val gateDaysText: String = "0",
    val savedShows: TitleShowDefaults? = null,
    val shows: TitleShowDefaults = TitleShowDefaults(),
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val savedCount: Int = 0,
) {
    val showMovies: Boolean get() = moviesOn && savedDefaults != null

    val showShows: Boolean get() = showsOn && savedDefaults != null

    val isEmpty: Boolean get() = status.phase == Phase.READY && !showMovies && !showShows && readError == null

    val showGate: Boolean get() = showMovies && savedGate != null

    val showShowSettings: Boolean get() = showShows && savedShows != null

    val gateDaysValid: Boolean get() = settingsLookGateDays(gateDaysText) != null

    private val defaultsDirty: Boolean get() = savedDefaults != null && defaults != savedDefaults

    private val gateDirty: Boolean get() = savedGate != null && (gate != savedGate || !gateDaysValid)

    private val showsDirty: Boolean get() = savedShows != null && shows != savedShows

    val dirty: Boolean get() = defaultsDirty || gateDirty || showsDirty

    val canSave: Boolean get() = canEdit && dirty && gateDaysValid && !saving

    fun folderOptions(show: Boolean): List<SettingsLookOption> =
        folders
            .filter { it.kind == kindName(show) }
            .map { SettingsLookOption(it.id, if (it.label.isNotBlank()) "${it.label} (${it.path})" else it.path) }

    fun ruleOptions(show: Boolean): List<SettingsLookOption> =
        rules
            .filter { it.kinds.isEmpty() || kindName(show) in it.kinds }
            .map { SettingsLookOption(it.id, it.name) }

    fun folderId(show: Boolean): Long = kind(show).libraryFolderId

    fun rulesId(show: Boolean): Long = kind(show).pickRulesId

    fun keepUpToDate(show: Boolean): Boolean = kind(show).keepUpToDate

    fun searchOnAdd(show: Boolean): Boolean = kind(show).searchOnAdd

    val gateChoices: List<String> get() = choices(GATE_CHOICES, gate.releaseGate)

    val seasonChoices: List<String> get() = choices(SEASON_CHOICES, shows.seasonChoice)

    val newSeasonChoices: List<String> get() = choices(NEW_SEASON_CHOICES, shows.newSeasons)

    val orderChoices: List<String> get() = choices(ORDER_CHOICES, shows.episodeOrder)

    val sourceChoices: List<String> get() = choices(SOURCE_CHOICES, shows.detailsSource)

    private fun kind(show: Boolean): TitleKindDefaults = if (show) defaults.show else defaults.movie

    private fun choices(
        known: List<String>,
        current: String,
    ): List<String> = if (current.isEmpty() || current in known) known else known + current
}

private fun kindName(show: Boolean): String = if (show) "show" else "movie"

class SettingsLookDefaultsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsLookDefaultsState())
    private val lock = Mutex()

    val state: StateFlow<SettingsLookDefaultsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            val kept = mutableState.value
            if (kept.dirty) return
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    canEdit = me?.can(Need.SETTINGS) == true,
                    canOpenFolders = me != null && SettingsCatalog.mayOpen(me, SettingsPage.LIBRARY_FOLDERS),
                    canOpenRequests = me != null && SettingsCatalog.mayOpen(me, SettingsPage.REQUESTS),
                    canOpenOutsideLists = me != null && SettingsCatalog.mayOpen(me, SettingsPage.OUTSIDE_LISTS),
                )
            }
            coroutineScope {
                val movies = me?.isOn(Part.MOVIES) == true
                val shows = me?.isOn(Part.SHOWS) == true
                val media = me?.isOn(Part.MEDIA) == true
                val defaults = async { if (media) api.result(TitleSupportEndpoints.defaults) else null }
                val gate = async { if (movies) api.result(TitleSupportEndpoints.gate) else null }
                val showSettings = async { if (shows) api.result(TitleSupportEndpoints.showDefaults) else null }
                val folders = async { if (media) api.result(LibraryEndpoints.folders).valueOrNull() else null }
                val rules = async { if (media) api.result(LibraryEndpoints.pickRules).valueOrNull() else null }
                take(
                    Reads(defaults.await(), gate.await(), showSettings.await(), movies, shows),
                    folders.await()?.libraryFolders.orEmpty(),
                    rules.await()?.pickRules.orEmpty(),
                )
            }
        }
    }

    private class Reads(
        val defaults: ApiResult<TitleAddDefaults>?,
        val gate: ApiResult<TitleGateDefaults>?,
        val shows: ApiResult<TitleShowDefaults>?,
        val moviesAllowed: Boolean,
        val showsAllowed: Boolean,
    )

    private fun take(
        reads: Reads,
        folders: List<LibraryFolder>,
        rules: List<PickRule>,
    ) {
        val all = listOf(reads.defaults, reads.gate, reads.shows)
        val problem = all.filterIsInstance<ApiResult.Failed>().map { it.error }.firstOrNull { it.code != ApiErrorCode.PART_OFF }
        val loaded = all.any { it is ApiResult.Ok }
        mutableState.update { s ->
            if (!loaded && problem != null) return@update s.copy(status = s.status.failed(problem))
            val savedDefaults = keep(reads.defaults, s.savedDefaults)
            val savedGate = keep(reads.gate, s.savedGate)
            val savedShows = keep(reads.shows, s.savedShows)
            s.copy(
                status = LoadStatus.ready,
                readError = problem,
                moviesOn = reads.moviesAllowed && !isPartOff(reads.gate),
                showsOn = reads.showsAllowed && !isPartOff(reads.shows),
                folders = folders,
                rules = rules,
                savedDefaults = savedDefaults,
                defaults = savedDefaults ?: TitleAddDefaults(),
                savedGate = savedGate,
                gate = savedGate ?: TitleGateDefaults(),
                gateDaysText = (savedGate?.gateDelayDays ?: 0).toString(),
                savedShows = savedShows,
                shows = savedShows ?: TitleShowDefaults(),
                saveError = null,
            )
        }
    }

    private fun <T> keep(
        result: ApiResult<T>?,
        previous: T?,
    ): T? =
        when {
            result is ApiResult.Ok -> result.value
            result is ApiResult.Failed && result.error.code != ApiErrorCode.PART_OFF -> previous
            else -> null
        }

    private fun isPartOff(result: ApiResult<*>?): Boolean = result is ApiResult.Failed && result.error.code == ApiErrorCode.PART_OFF

    fun setFolder(
        show: Boolean,
        id: Long,
    ) = editKind(show) { it.copy(libraryFolderId = id) }

    fun setRules(
        show: Boolean,
        id: Long,
    ) = editKind(show) { it.copy(pickRulesId = id) }

    fun setKeepUpToDate(
        show: Boolean,
        on: Boolean,
    ) = editKind(show) { it.copy(keepUpToDate = on) }

    fun setSearchOnAdd(
        show: Boolean,
        on: Boolean,
    ) = editKind(show) { it.copy(searchOnAdd = on) }

    fun setReleaseGate(value: String) = edit { it.copy(gate = it.gate.copy(releaseGate = value)) }

    fun setGateDays(text: String) =
        edit { s ->
            val digits = text.filter { it.isDigit() }.take(3)
            val days = settingsLookGateDays(digits)
            s.copy(gateDaysText = digits, gate = if (days == null) s.gate else s.gate.copy(gateDelayDays = days))
        }

    fun setSeasonChoice(value: String) = edit { it.copy(shows = it.shows.copy(seasonChoice = value)) }

    fun setNewSeasons(value: String) = edit { it.copy(shows = it.shows.copy(newSeasons = value)) }

    fun setEpisodeOrder(value: String) = edit { it.copy(shows = it.shows.copy(episodeOrder = value)) }

    fun setDetailsSource(value: String) = edit { it.copy(shows = it.shows.copy(detailsSource = value)) }

    fun discard() =
        mutableState.update {
            it.copy(
                defaults = it.savedDefaults ?: it.defaults,
                gate = it.savedGate ?: it.gate,
                gateDaysText = (it.savedGate?.gateDelayDays ?: 0).toString(),
                shows = it.savedShows ?: it.shows,
                saveError = null,
            )
        }

    suspend fun save() {
        lock.withLock {
            if (!mutableState.value.canSave) return
            mutableState.update { it.copy(saving = true, saveError = null) }
            var failure: ApiError? = null
            val first = mutableState.value
            if (first.savedDefaults != null && first.defaults != first.savedDefaults) {
                when (val result = api.result(SettingsLookEndpoints.saveDefaults, first.defaults)) {
                    is ApiResult.Ok -> mutableState.update { it.copy(savedDefaults = result.value, defaults = result.value) }
                    is ApiResult.Failed -> failure = result.error
                }
            }
            val second = mutableState.value
            if (failure == null && second.savedGate != null && second.gate != second.savedGate) {
                when (val result = api.result(SettingsLookEndpoints.saveGate, second.gate)) {
                    is ApiResult.Ok ->
                        mutableState.update {
                            it.copy(savedGate = result.value, gate = result.value, gateDaysText = result.value.gateDelayDays.toString())
                        }
                    is ApiResult.Failed -> failure = result.error
                }
            }
            val third = mutableState.value
            if (failure == null && third.savedShows != null && third.shows != third.savedShows) {
                when (val result = api.result(SettingsLookEndpoints.saveShows, third.shows)) {
                    is ApiResult.Ok -> mutableState.update { it.copy(savedShows = result.value, shows = result.value) }
                    is ApiResult.Failed -> failure = result.error
                }
            }
            mutableState.update {
                it.copy(saving = false, saveError = failure, savedCount = if (failure == null) it.savedCount + 1 else it.savedCount)
            }
        }
    }

    private fun edit(transform: (SettingsLookDefaultsState) -> SettingsLookDefaultsState) {
        mutableState.update { if (it.canEdit) transform(it).copy(saveError = null) else it }
    }

    private fun editKind(
        show: Boolean,
        transform: (TitleKindDefaults) -> TitleKindDefaults,
    ) = edit {
        if (show) {
            it.copy(defaults = it.defaults.copy(show = transform(it.defaults.show)))
        } else {
            it.copy(defaults = it.defaults.copy(movie = transform(it.defaults.movie)))
        }
    }
}
