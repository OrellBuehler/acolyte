package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.RequestSettingsEndpoints
import ch.orellbuehler.acolyte.api.model.AskFolder
import ch.orellbuehler.acolyte.api.model.AskPickRules
import ch.orellbuehler.acolyte.api.model.AskSettings
import ch.orellbuehler.acolyte.api.model.AskWatchService
import ch.orellbuehler.acolyte.api.model.CleanupCheck
import ch.orellbuehler.acolyte.api.model.PlexFriends
import ch.orellbuehler.acolyte.api.model.QualityLine
import ch.orellbuehler.acolyte.api.model.QualityLines
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class RequestPage(
    internal val need: Need,
    internal val part: Part,
) {
    RULES(Need.SETTINGS, Part.WISHLIST),
    OUTSIDE_LISTS(Need.SETTINGS, Part.MEDIA),
    NEVER_ADD(Need.MANAGE_LIBRARY, Part.MEDIA),
}

fun Account.mayOpen(page: RequestPage): Boolean = allows(page.need, page.part)

fun Account.requestPages(): List<RequestPage> = RequestPage.entries.filter { mayOpen(it) }

enum class FolderField(
    internal val kind: String,
) {
    MOVIE("movie"),
    SHOW("show"),
    MUSIC("music"),
    MOVIE_FOUR_K("movie"),
    SHOW_FOUR_K("show"),
}

enum class SettingsFlag {
    SEARCH_ON_APPROVE,
    OWNER_AUTO_APPROVE,
    PLEX_WATCHLISTS,
    PLEX_EVERYONE,
    PLEX_FRIENDS,
    PICK_MANY,
    SKIP_ON_SERVICES,
}

enum class LineKind {
    MOVIE,
    SHOW,
}

val STREAMING_REGIONS: List<String> =
    (
        "AR AT AU BE BG BR CA CH CL CO CZ DE DK EE EG ES FI FR GB GR HK HR HU ID IE IL IN IS IT JP KR LT LV MX MY NL NO NZ PE PH " +
            "PL PT RO RS RU SA SE SG SK TH TR TW UA US VN ZA"
    ).split(" ")

const val MAX_SERVICES: Int = 30
const val MAX_LINES: Int = 8

data class RequestSettingsState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val saved: AskSettings = AskSettings(),
    val draft: AskSettings = AskSettings(),
    val limitText: Map<LimitKind, String> = emptyMap(),
    val folders: List<AskFolder> = emptyList(),
    val rules: List<AskPickRules> = emptyList(),
    val plexFriends: PlexFriends? = null,
    val services: List<AskWatchService> = emptyList(),
    val servicesRegion: String = "",
    val servicesLoading: Boolean = false,
    val servicesError: ApiError? = null,
    val cleanup: CleanupCheck? = null,
    val cleanupError: ApiError? = null,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val savedCount: Int = 0,
) {
    val dirty: Boolean get() = draft != saved || limitText.any { (kind, text) -> text != limitOf(saved, kind).toString() }

    val limitsValid: Boolean get() =
        LimitKind.entries.all { kind ->
            parseLimit(
                limitText[kind] ?: limitOf(draft, kind).toString(),
            ) != null
        }

    val canSave: Boolean get() = dirty && limitsValid && !saving

    val servicesSaved: Boolean get() = saved.streamingRegion.isNotEmpty() && saved.streamingServices.isNotEmpty()

    val canCheck: Boolean get() = servicesSaved && !dirty && cleanup?.running != true

    val pickedServices: List<AskWatchService>
        get() {
            val picked = draft.streamingServices
            val known = services.associateBy { it.id }
            return picked.mapNotNull { known[it] }
        }

    val sortedServices: List<AskWatchService>
        get() = pickedServices + services.filter { it.id !in draft.streamingServices }

    fun limit(kind: LimitKind): String = limitText[kind] ?: limitOf(draft, kind).toString()

    fun foldersFor(field: FolderField): List<AskFolder> = folders.filter { it.kind == field.kind }

    fun rulesFor(field: FolderField): List<AskPickRules> = rules.filter { it.kinds.isEmpty() || field.kind in it.kinds }

    fun folder(field: FolderField): Long =
        when (field) {
            FolderField.MOVIE -> draft.libraryFolderId
            FolderField.SHOW -> draft.showLibraryFolderId
            FolderField.MUSIC -> draft.musicLibraryFolderId
            FolderField.MOVIE_FOUR_K -> draft.fourKLibraryFolderId
            FolderField.SHOW_FOUR_K -> draft.showFourKLibraryFolderId
        }

    fun pickRules(field: FolderField): Long =
        when (field) {
            FolderField.MOVIE -> draft.pickRulesId
            FolderField.SHOW -> draft.showPickRulesId
            FolderField.MUSIC -> draft.musicPickRulesId
            FolderField.MOVIE_FOUR_K -> draft.fourKPickRulesId
            FolderField.SHOW_FOUR_K -> draft.showFourKPickRulesId
        }

    fun flag(flag: SettingsFlag): Boolean =
        when (flag) {
            SettingsFlag.SEARCH_ON_APPROVE -> draft.searchOnApprove
            SettingsFlag.OWNER_AUTO_APPROVE -> draft.ownerAutoApprove
            SettingsFlag.PLEX_WATCHLISTS -> draft.plexWatchlists
            SettingsFlag.PLEX_EVERYONE -> draft.plexWatchlistsEveryone
            SettingsFlag.PLEX_FRIENDS -> draft.plexFriends
            SettingsFlag.PICK_MANY -> draft.pickMany
            SettingsFlag.SKIP_ON_SERVICES -> draft.skipOnServices
        }

    fun flagEnabled(flag: SettingsFlag): Boolean =
        when (flag) {
            SettingsFlag.PLEX_EVERYONE, SettingsFlag.PLEX_FRIENDS -> draft.plexWatchlists
            SettingsFlag.SKIP_ON_SERVICES -> draft.streamingRegion.isNotEmpty() && draft.streamingServices.isNotEmpty()
            else -> true
        }

    fun lines(kind: LineKind): List<QualityLine> = if (kind == LineKind.MOVIE) draft.qualities.movie else draft.qualities.show

    fun canAddLine(kind: LineKind): Boolean = lines(kind).size < MAX_LINES && rulesFor(kind.field).isNotEmpty()

    fun ruleName(id: Long): String = rules.firstOrNull { it.id == id }?.name.orEmpty()

    val showFriends: Boolean get() = draft.plexWatchlists && draft.plexFriends

    val regions: List<String> get() = STREAMING_REGIONS

    val maxServices: Int get() = MAX_SERVICES
}

internal val LineKind.field: FolderField get() = if (this == LineKind.MOVIE) FolderField.MOVIE else FolderField.SHOW

internal fun limitOf(
    s: AskSettings,
    kind: LimitKind,
): Int =
    when (kind) {
        LimitKind.MOVIES -> s.moviesPerWeek
        LimitKind.SHOWS -> s.showsPerWeek
        LimitKind.MUSIC -> s.musicPerWeek
    }

fun parseLimit(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 0..1000 }

class RequestSettingsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(RequestSettingsState())
    private val lock = Mutex()

    val state: StateFlow<RequestSettingsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(RequestPage.RULES)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started()) }
            coroutineScope {
                val folders =
                    async { if (me.can(Need.MANAGE_LIBRARY)) api.result(RequestSettingsEndpoints.folders).valueOrNull() else null }
                val rules =
                    async { if (me.can(Need.MANAGE_LIBRARY)) api.result(RequestSettingsEndpoints.pickRules).valueOrNull() else null }
                val friends = async { if (me.can(Need.USERS)) api.result(RequestSettingsEndpoints.plexFriends).valueOrNull() else null }
                val cleanup = async { api.result(RequestSettingsEndpoints.cleanup).valueOrNull() }
                when (val result = api.result(RequestSettingsEndpoints.settings)) {
                    is ApiResult.Ok ->
                        mutableState.update {
                            it.copy(
                                status = LoadStatus.ready,
                                saved = result.value,
                                draft = result.value,
                                limitText = emptyMap(),
                                folders = folders.await()?.libraryFolders.orEmpty(),
                                rules = rules.await()?.pickRules.orEmpty(),
                                plexFriends = friends.await(),
                                cleanup = cleanup.await(),
                            )
                        }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
            }
        }
        loadServices()
    }

    fun setLimit(
        kind: LimitKind,
        text: String,
    ) = mutableState.update { s ->
        val digits = text.filter { it.isDigit() }.take(4)
        val value = parseLimit(digits)
        val draft =
            if (value == null) {
                s.draft
            } else {
                when (kind) {
                    LimitKind.MOVIES -> s.draft.copy(moviesPerWeek = value)
                    LimitKind.SHOWS -> s.draft.copy(showsPerWeek = value)
                    LimitKind.MUSIC -> s.draft.copy(musicPerWeek = value)
                }
            }
        s.copy(draft = draft, limitText = s.limitText + (kind to digits))
    }

    fun setFolder(
        field: FolderField,
        id: Long,
    ) = edit {
        when (field) {
            FolderField.MOVIE -> it.copy(libraryFolderId = id)
            FolderField.SHOW -> it.copy(showLibraryFolderId = id)
            FolderField.MUSIC -> it.copy(musicLibraryFolderId = id)
            FolderField.MOVIE_FOUR_K -> it.copy(fourKLibraryFolderId = id)
            FolderField.SHOW_FOUR_K -> it.copy(showFourKLibraryFolderId = id)
        }
    }

    fun setPickRules(
        field: FolderField,
        id: Long,
    ) = edit {
        when (field) {
            FolderField.MOVIE -> it.copy(pickRulesId = id)
            FolderField.SHOW -> it.copy(showPickRulesId = id)
            FolderField.MUSIC -> it.copy(musicPickRulesId = id)
            FolderField.MOVIE_FOUR_K -> it.copy(fourKPickRulesId = id)
            FolderField.SHOW_FOUR_K -> it.copy(showFourKPickRulesId = id)
        }
    }

    fun setFlag(
        flag: SettingsFlag,
        on: Boolean,
    ) = edit {
        when (flag) {
            SettingsFlag.SEARCH_ON_APPROVE -> it.copy(searchOnApprove = on)
            SettingsFlag.OWNER_AUTO_APPROVE -> it.copy(ownerAutoApprove = on)
            SettingsFlag.PLEX_WATCHLISTS ->
                if (on) {
                    it.copy(
                        plexWatchlists = true,
                    )
                } else {
                    it.copy(plexWatchlists = false, plexWatchlistsEveryone = false, plexFriends = false)
                }
            SettingsFlag.PLEX_EVERYONE -> it.copy(plexWatchlistsEveryone = on)
            SettingsFlag.PLEX_FRIENDS -> it.copy(plexFriends = on)
            SettingsFlag.PICK_MANY -> it.copy(pickMany = on)
            SettingsFlag.SKIP_ON_SERVICES -> it.copy(skipOnServices = on)
        }
    }

    fun setLineName(
        kind: LineKind,
        index: Int,
        name: String,
    ) = editLine(kind, index) { it.copy(name = name.take(LINE_NAME_MAX)) }

    fun setLineLabel(
        kind: LineKind,
        index: Int,
        label: String,
    ) = editLine(kind, index) { it.copy(label = label.take(LINE_NAME_MAX)) }

    fun setLineShown(
        kind: LineKind,
        index: Int,
        shown: Boolean,
    ) = editLine(kind, index) { it.copy(hidden = !shown) }

    fun setLineRules(
        kind: LineKind,
        index: Int,
        id: Long,
    ) = editLine(kind, index) { if (it.isOwn) it.copy(pickRulesId = id) else it }

    fun moveLine(
        kind: LineKind,
        index: Int,
        up: Boolean,
    ) = editLines(kind) { lines ->
        val target = if (up) index - 1 else index + 1
        if (index !in lines.indices || target !in lines.indices) return@editLines lines
        lines.toMutableList().also {
            val line = it.removeAt(index)
            it.add(target, line)
        }
    }

    fun addLine(kind: LineKind) {
        val s = mutableState.value
        if (!s.canAddLine(kind)) return
        val rules = s.rulesFor(kind.field).first().id
        editLines(kind) { lines ->
            val used =
                lines
                    .mapNotNull {
                        it.id
                            .removePrefix("own-")
                            .takeIf { _ -> it.isOwn }
                            ?.toIntOrNull()
                    }.toSet()
            val n = generateSequence(1) { it + 1 }.first { it !in used }
            lines + QualityLine(id = "own-$n", pickRulesId = rules, ready = true)
        }
    }

    fun removeLine(
        kind: LineKind,
        index: Int,
    ) = editLines(kind) { lines -> if (lines.getOrNull(index)?.isOwn == true) lines.filterIndexed { i, _ -> i != index } else lines }

    suspend fun setRegion(region: String) {
        edit {
            if (it.streamingRegion ==
                region
            ) {
                it
            } else {
                it.copy(streamingRegion = region, streamingServices = emptyList(), skipOnServices = false)
            }
        }
        loadServices()
    }

    fun toggleService(id: Long) =
        edit {
            val picked = it.streamingServices
            when {
                id in picked -> it.copy(streamingServices = picked - id, skipOnServices = it.skipOnServices && picked.size > 1)
                picked.size >= MAX_SERVICES -> it
                else -> it.copy(streamingServices = picked + id)
            }
        }

    fun discard() = mutableState.update { it.copy(draft = it.saved, limitText = emptyMap(), saveError = null) }

    suspend fun save() {
        val s = mutableState.value
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        api
            .result(RequestSettingsEndpoints.save, s.draft)
            .onSuccess { saved ->
                mutableState.update {
                    it.copy(saving = false, saved = saved, draft = saved, limitText = emptyMap(), savedCount = it.savedCount + 1)
                }
            }.onFailure { error -> mutableState.update { it.copy(saving = false, saveError = error) } }
    }

    suspend fun startCleanup() {
        if (!mutableState.value.canCheck) return
        mutableState.update { it.copy(cleanupError = null) }
        when (val result = api.result(RequestSettingsEndpoints.startCleanup)) {
            is ApiResult.Ok -> mutableState.update { it.copy(cleanup = result.value) }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(cleanupError = result.error) }
                return
            }
        }
        followCleanup()
    }

    suspend fun followCleanup() {
        while (mutableState.value.cleanup?.running == true) {
            delay(CLEANUP_POLL_MILLIS)
            api.result(RequestSettingsEndpoints.cleanup).onSuccess { check -> mutableState.update { it.copy(cleanup = check) } }
        }
    }

    fun dropFound(id: Long) =
        mutableState.update { s -> s.copy(cleanup = s.cleanup?.let { c -> c.copy(found = c.found.filter { it.id != id }) }) }

    private suspend fun loadServices() {
        val region = mutableState.value.draft.streamingRegion
        if (region.isEmpty()) {
            mutableState.update { it.copy(services = emptyList(), servicesRegion = "", servicesError = null) }
            return
        }
        if (region == mutableState.value.servicesRegion) return
        mutableState.update { it.copy(servicesLoading = true, servicesError = null) }
        when (val result = api.result(RequestSettingsEndpoints.watchServices, "region" to region)) {
            is ApiResult.Ok ->
                mutableState.update { it.copy(services = result.value.services, servicesRegion = region, servicesLoading = false) }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(
                        services = emptyList(),
                        servicesRegion = "",
                        servicesLoading = false,
                        servicesError = result.error,
                    )
                }
        }
    }

    private fun edit(transform: (AskSettings) -> AskSettings) =
        mutableState.update { it.copy(draft = transform(it.draft), saveError = null) }

    private fun editLines(
        kind: LineKind,
        transform: (List<QualityLine>) -> List<QualityLine>,
    ) = edit { s ->
        val q = s.qualities
        s.copy(
            qualities =
                if (kind ==
                    LineKind.MOVIE
                ) {
                    QualityLines(transform(q.movie), q.show)
                } else {
                    QualityLines(q.movie, transform(q.show))
                },
        )
    }

    private fun editLine(
        kind: LineKind,
        index: Int,
        transform: (QualityLine) -> QualityLine,
    ) = editLines(kind) { lines -> lines.mapIndexed { i, line -> if (i == index) transform(line) else line } }

    private companion object {
        const val LINE_NAME_MAX = 30
        const val CLEANUP_POLL_MILLIS = 2_000L
    }
}
