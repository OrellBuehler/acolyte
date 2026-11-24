package ch.orellbuehler.acolyte.feature.acquisition

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SearchSettingsEndpoints
import ch.orellbuehler.acolyte.api.model.JobIds
import ch.orellbuehler.acolyte.api.model.SettingsSearchValues
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SettingsSearchNumber(
    val wire: String,
    val min: Int,
    val max: Int,
) {
    MISSING_COUNT("missing_search_count", 0, 500),
    FEED_MINUTES("feed_check_minutes", 0, 120),
    GRACE_HOURS("early_release_grace_hours", 0, 720),
    MIN_VIDEO_MB("import_min_video_mb", 0, 10000),
    MIN_TRACK_KB("import_min_track_kb", 0, 100000),
    EPISODE_MINUTES("episode_default_minutes", 1, 240),
    TRACK_TOLERANCE("album_track_tolerance", -1, 50),
    EXTRA_FILES("album_extra_files", 0, 20),
    SEARCH_CAP("artist_search_cap", 1, 2000),
    ;

    companion object {
        fun of(wire: String): SettingsSearchNumber? = entries.firstOrNull { it.wire == wire }
    }
}

enum class SettingsSearchSwitch(
    val wire: String,
) {
    MISSING_BETTER("missing_search_better"),
    MISSING_SHOWS("missing_search_shows"),
    REFUSE_EARLY("refuse_early_releases"),
    REFUSE_TINY("refuse_tiny_releases"),
    SIZE_FLOORS("import_size_floors"),
    HOLD_UNKNOWN("hold_unknown_releases"),
    MULTI_SEASON_PACKS("multi_season_packs"),
    MISSING_MUSIC("missing_search_music"),
    DISCOGRAPHY_PACKS("music_discography_packs"),
    ;

    companion object {
        fun of(wire: String): SettingsSearchSwitch? = entries.firstOrNull { it.wire == wire }
    }
}

enum class SettingsSearchChoice(
    val wire: String,
    val options: List<String>,
) {
    PACK_UPGRADE("season_pack_upgrade", listOf("all", "any")),
    PACK_PREFER("season_pack_prefer", listOf("rank", "always")),
    ;

    companion object {
        fun of(wire: String): SettingsSearchChoice? = entries.firstOrNull { it.wire == wire }
    }
}

enum class SettingsSearchRun {
    FEEDS,
    BETTER,
}

enum class SettingsSearchNumberError {
    FEED,
    RANGE,
}

fun settingsSearchNumberError(
    field: SettingsSearchNumber,
    text: String,
): SettingsSearchNumberError? {
    val value = text.trim().toIntOrNull()
    return when {
        value == null -> if (field == SettingsSearchNumber.FEED_MINUTES) SettingsSearchNumberError.FEED else SettingsSearchNumberError.RANGE
        field == SettingsSearchNumber.FEED_MINUTES ->
            if (value == 0 || value in FEED_MIN..field.max) null else SettingsSearchNumberError.FEED
        value in field.min..field.max -> null
        else -> SettingsSearchNumberError.RANGE
    }
}

private const val FEED_MIN = 10

data class SettingsSearchState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canRun: Boolean = false,
    internal val saved: SettingsSearchValues = SettingsSearchValues(),
    internal val draft: SettingsSearchValues = SettingsSearchValues(),
    internal val typed: Map<SettingsSearchNumber, String> = emptyMap(),
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val savedCount: Int = 0,
    val running: SettingsSearchRun? = null,
    val note: SettingsSearchRun? = null,
    val noteCount: Int = 0,
    val runError: ApiError? = null,
    val runErrorCount: Int = 0,
) {
    val dirty: Boolean get() = draft != saved || typed.any { (field, text) -> text.trim().toIntOrNull() != numberOf(field, saved) }

    val numbersValid: Boolean get() = SettingsSearchNumber.entries.all { numberError(it) == null }

    val canSave: Boolean get() = dirty && numbersValid && !saving

    val values: List<SettingsNamingValue> get() =
        SettingsSearchNumber.entries.map { SettingsNamingValue(it.wire, number(it)) } +
            SettingsSearchChoice.entries.map { SettingsNamingValue(it.wire, choice(it)) }

    val switchesOn: List<String> get() = SettingsSearchSwitch.entries.filter { isOn(it) }.map { it.wire }

    fun number(field: SettingsSearchNumber): String = typed[field] ?: numberOf(field, draft).toString()

    fun number(wire: String): String = SettingsSearchNumber.of(wire)?.let { number(it) }.orEmpty()

    fun numberError(field: SettingsSearchNumber): SettingsSearchNumberError? = settingsSearchNumberError(field, number(field))

    fun numberMin(wire: String): Int = SettingsSearchNumber.of(wire)?.min ?: 0

    fun numberMax(wire: String): Int = SettingsSearchNumber.of(wire)?.max ?: 0

    fun numberErrorKey(wire: String): String =
        SettingsSearchNumber
            .of(wire)
            ?.let { numberError(it) }
            ?.name
            ?.lowercase()
            .orEmpty()

    fun isOn(field: SettingsSearchSwitch): Boolean =
        when (field) {
            SettingsSearchSwitch.MISSING_BETTER -> draft.missingSearchBetter
            SettingsSearchSwitch.MISSING_SHOWS -> draft.missingSearchShows
            SettingsSearchSwitch.REFUSE_EARLY -> draft.refuseEarlyReleases
            SettingsSearchSwitch.REFUSE_TINY -> draft.refuseTinyReleases
            SettingsSearchSwitch.SIZE_FLOORS -> draft.importSizeFloors
            SettingsSearchSwitch.HOLD_UNKNOWN -> draft.holdUnknownReleases
            SettingsSearchSwitch.MULTI_SEASON_PACKS -> draft.multiSeasonPacks
            SettingsSearchSwitch.MISSING_MUSIC -> draft.missingSearchMusic
            SettingsSearchSwitch.DISCOGRAPHY_PACKS -> draft.musicDiscographyPacks
        }

    fun isOn(wire: String): Boolean = SettingsSearchSwitch.of(wire)?.let { isOn(it) } ?: false

    fun choice(field: SettingsSearchChoice): String =
        when (field) {
            SettingsSearchChoice.PACK_UPGRADE -> draft.seasonPackUpgrade
            SettingsSearchChoice.PACK_PREFER -> draft.seasonPackPrefer
        }

    fun choice(wire: String): String = SettingsSearchChoice.of(wire)?.let { choice(it) }.orEmpty()
}

private fun numberOf(
    field: SettingsSearchNumber,
    values: SettingsSearchValues,
): Int =
    when (field) {
        SettingsSearchNumber.MISSING_COUNT -> values.missingSearchCount
        SettingsSearchNumber.FEED_MINUTES -> values.feedCheckMinutes
        SettingsSearchNumber.GRACE_HOURS -> values.earlyReleaseGraceHours
        SettingsSearchNumber.MIN_VIDEO_MB -> values.importMinVideoMb
        SettingsSearchNumber.MIN_TRACK_KB -> values.importMinTrackKb
        SettingsSearchNumber.EPISODE_MINUTES -> values.episodeDefaultMinutes
        SettingsSearchNumber.TRACK_TOLERANCE -> values.albumTrackTolerance
        SettingsSearchNumber.EXTRA_FILES -> values.albumExtraFiles
        SettingsSearchNumber.SEARCH_CAP -> values.artistSearchCap
    }

private fun SettingsSearchValues.with(
    field: SettingsSearchNumber,
    value: Int,
): SettingsSearchValues =
    when (field) {
        SettingsSearchNumber.MISSING_COUNT -> copy(missingSearchCount = value)
        SettingsSearchNumber.FEED_MINUTES -> copy(feedCheckMinutes = value)
        SettingsSearchNumber.GRACE_HOURS -> copy(earlyReleaseGraceHours = value)
        SettingsSearchNumber.MIN_VIDEO_MB -> copy(importMinVideoMb = value)
        SettingsSearchNumber.MIN_TRACK_KB -> copy(importMinTrackKb = value)
        SettingsSearchNumber.EPISODE_MINUTES -> copy(episodeDefaultMinutes = value)
        SettingsSearchNumber.TRACK_TOLERANCE -> copy(albumTrackTolerance = value)
        SettingsSearchNumber.EXTRA_FILES -> copy(albumExtraFiles = value)
        SettingsSearchNumber.SEARCH_CAP -> copy(artistSearchCap = value)
    }

class SettingsSearchPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsSearchState())
    private val lock = Mutex()

    val state: StateFlow<SettingsSearchState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(AcquisitionPage.SEARCH)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started()) }
            when (val result = api.result(SearchSettingsEndpoints.settings)) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            canRun = me.can(Need.MANAGE_LIBRARY),
                            saved = result.value,
                            draft = result.value,
                            typed = emptyMap(),
                            saveError = null,
                        )
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    fun setNumber(
        field: SettingsSearchNumber,
        text: String,
    ) = mutableState.update { s ->
        val digits = text.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-' && field.min < 0) }.take(MAX_DIGITS)
        val value = digits.toIntOrNull()
        val draft = if (value != null && settingsSearchNumberError(field, digits) == null) s.draft.with(field, value) else s.draft
        s.copy(draft = draft, typed = s.typed + (field to digits), saveError = null)
    }

    fun setNumber(
        wire: String,
        text: String,
    ) {
        SettingsSearchNumber.of(wire)?.let { setNumber(it, text) }
    }

    fun setSwitch(
        field: SettingsSearchSwitch,
        on: Boolean,
    ) = edit {
        when (field) {
            SettingsSearchSwitch.MISSING_BETTER -> it.copy(missingSearchBetter = on)
            SettingsSearchSwitch.MISSING_SHOWS -> it.copy(missingSearchShows = on)
            SettingsSearchSwitch.REFUSE_EARLY -> it.copy(refuseEarlyReleases = on)
            SettingsSearchSwitch.REFUSE_TINY -> it.copy(refuseTinyReleases = on)
            SettingsSearchSwitch.SIZE_FLOORS -> it.copy(importSizeFloors = on)
            SettingsSearchSwitch.HOLD_UNKNOWN -> it.copy(holdUnknownReleases = on)
            SettingsSearchSwitch.MULTI_SEASON_PACKS -> it.copy(multiSeasonPacks = on)
            SettingsSearchSwitch.MISSING_MUSIC -> it.copy(missingSearchMusic = on)
            SettingsSearchSwitch.DISCOGRAPHY_PACKS -> it.copy(musicDiscographyPacks = on)
        }
    }

    fun setSwitch(
        wire: String,
        on: Boolean,
    ) {
        SettingsSearchSwitch.of(wire)?.let { setSwitch(it, on) }
    }

    fun setChoice(
        field: SettingsSearchChoice,
        value: String,
    ) = edit {
        when (field) {
            SettingsSearchChoice.PACK_UPGRADE -> it.copy(seasonPackUpgrade = value)
            SettingsSearchChoice.PACK_PREFER -> it.copy(seasonPackPrefer = value)
        }
    }

    fun setChoice(
        wire: String,
        value: String,
    ) {
        SettingsSearchChoice.of(wire)?.let { setChoice(it, value) }
    }

    fun discard() = mutableState.update { it.copy(draft = it.saved, typed = emptyMap(), saveError = null) }

    suspend fun save() {
        val s = mutableState.value
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        when (val result = api.result(SearchSettingsEndpoints.save, s.draft)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(saving = false, saved = result.value, draft = result.value, typed = emptyMap(), savedCount = it.savedCount + 1)
                }
            is ApiResult.Failed -> mutableState.update { it.copy(saving = false, saveError = result.error) }
        }
    }

    suspend fun checkFeeds() = run(SettingsSearchRun.FEEDS)

    suspend fun findBetter() = run(SettingsSearchRun.BETTER)

    private suspend fun run(kind: SettingsSearchRun) {
        val s = mutableState.value
        if (!s.canRun || s.running != null) return
        mutableState.update { it.copy(running = kind, runError = null) }
        val result: ApiResult<JobIds> =
            when (kind) {
                SettingsSearchRun.FEEDS -> api.result(SearchSettingsEndpoints.feedCheck)
                SettingsSearchRun.BETTER -> api.result(SearchSettingsEndpoints.better)
            }
        when (result) {
            is ApiResult.Ok -> mutableState.update { it.copy(running = null, note = kind, noteCount = it.noteCount + 1) }
            is ApiResult.Failed ->
                mutableState.update { it.copy(running = null, runError = result.error, runErrorCount = it.runErrorCount + 1) }
        }
    }

    private fun edit(transform: (SettingsSearchValues) -> SettingsSearchValues) =
        mutableState.update { it.copy(draft = transform(it.draft), saveError = null) }

    private companion object {
        const val MAX_DIGITS = 6
    }
}
