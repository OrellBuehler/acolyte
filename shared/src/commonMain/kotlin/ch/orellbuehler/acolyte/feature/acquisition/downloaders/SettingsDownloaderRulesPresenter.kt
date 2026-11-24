package ch.orellbuehler.acolyte.feature.acquisition.downloaders

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.DownloadersEndpoints
import ch.orellbuehler.acolyte.api.DownloadsLinkEndpoints
import ch.orellbuehler.acolyte.api.model.DownloaderSettings
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.mayEditAcquisition
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SettingsDownloaderSwitch {
    REMOVE_FAILED,
    AUTO_ADD,
    SAB_OUTSIDE,
    DELETE_FILES,
    TURN_DOWN,
    SEARCH_AGAIN,
    FAIL_DANGEROUS,
    REPLACE_VANISHED,
    TRY_NEXT,
}

enum class SettingsDownloaderNumber {
    STALLED_AFTER,
    STALLED_ACT,
    SPACING,
    DEAD_MINUTES,
    CRAWL_KBPS,
    CRAWL_HOURS,
}

enum class SettingsDownloaderRulesIssue {
    NOT_A_NUMBER,
    STALLED_AFTER_RANGE,
    ACT_SOONER,
    ACT_RANGE,
    SPACING_RANGE,
    DEAD_MINUTES_RANGE,
    KBPS_RANGE,
    HOURS_RANGE,
}

private const val STALLED_AFTER_MIN = 1.0
private const val STALLED_AFTER_MAX = 168.0
private const val ACT_MAX = 720.0
private const val SPACING_MAX = 60.0
private const val DEAD_MIN = 5.0
private const val DEAD_MAX = 1440.0
private const val KBPS_MAX = 100_000.0
private const val CRAWL_HOURS_MAX = 168.0
private const val DEAD_ON_MINUTES = 30.0
private const val DEAD_ON_KBPS = 10.0
private const val DEAD_ON_HOURS = 6.0

data class SettingsDownloaderRulesState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canEdit: Boolean = false,
    val saved: DownloaderSettings = DownloaderSettings(),
    val draft: DownloaderSettings = DownloaderSettings(),
    val text: Map<SettingsDownloaderNumber, String> = emptyMap(),
    val deadOpen: Boolean = false,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val savedCount: Int = 0,
) {
    val loaded: Boolean get() = status.phase == Phase.READY

    val deadOn: Boolean get() = deadOpen || draft.deadMetadataMinutes != 0.0 || draft.crawlKbps != 0.0

    val dirty: Boolean get() = draft != saved || text.any { (kind, value) -> value != format(saved, kind) }

    val issue: SettingsDownloaderRulesIssue? get() = findIssue()

    val canSave: Boolean get() = canEdit && dirty && issue == null && !saving

    fun number(kind: SettingsDownloaderNumber): String = text[kind] ?: format(draft, kind)

    fun flag(flag: SettingsDownloaderSwitch): Boolean =
        when (flag) {
            SettingsDownloaderSwitch.REMOVE_FAILED -> draft.removeFailed
            SettingsDownloaderSwitch.AUTO_ADD -> draft.downloadsAutoAdd
            SettingsDownloaderSwitch.SAB_OUTSIDE -> draft.downloadsSabOutside
            SettingsDownloaderSwitch.DELETE_FILES -> draft.removeDeleteFiles
            SettingsDownloaderSwitch.TURN_DOWN -> draft.removeTurnDown
            SettingsDownloaderSwitch.SEARCH_AGAIN -> draft.removeSearchAgain
            SettingsDownloaderSwitch.FAIL_DANGEROUS -> draft.failDangerous
            SettingsDownloaderSwitch.REPLACE_VANISHED -> draft.replaceVanished
            SettingsDownloaderSwitch.TRY_NEXT -> draft.tryNextDownloader
        }

    private fun findIssue(): SettingsDownloaderRulesIssue? {
        if (text.any { (_, value) -> parseNumber(value) == null }) return SettingsDownloaderRulesIssue.NOT_A_NUMBER
        val after = draft.stalledAfterHours
        val act = draft.stalledActHours
        val dead = draft.deadMetadataMinutes
        return when {
            after < STALLED_AFTER_MIN || after > STALLED_AFTER_MAX -> SettingsDownloaderRulesIssue.STALLED_AFTER_RANGE
            act < after -> SettingsDownloaderRulesIssue.ACT_SOONER
            act > ACT_MAX -> SettingsDownloaderRulesIssue.ACT_RANGE
            draft.grabSpacingSeconds < 0 || draft.grabSpacingSeconds > SPACING_MAX -> SettingsDownloaderRulesIssue.SPACING_RANGE
            dead != 0.0 && (dead < DEAD_MIN || dead > DEAD_MAX) -> SettingsDownloaderRulesIssue.DEAD_MINUTES_RANGE
            draft.crawlKbps < 0 || draft.crawlKbps > KBPS_MAX -> SettingsDownloaderRulesIssue.KBPS_RANGE
            draft.crawlHours != 0.0 && (draft.crawlHours < 1 || draft.crawlHours > CRAWL_HOURS_MAX) ->
                SettingsDownloaderRulesIssue.HOURS_RANGE
            else -> null
        }
    }
}

private fun format(
    s: DownloaderSettings,
    kind: SettingsDownloaderNumber,
): String =
    formatNumber(
        when (kind) {
            SettingsDownloaderNumber.STALLED_AFTER -> s.stalledAfterHours
            SettingsDownloaderNumber.STALLED_ACT -> s.stalledActHours
            SettingsDownloaderNumber.SPACING -> s.grabSpacingSeconds
            SettingsDownloaderNumber.DEAD_MINUTES -> s.deadMetadataMinutes
            SettingsDownloaderNumber.CRAWL_KBPS -> s.crawlKbps
            SettingsDownloaderNumber.CRAWL_HOURS -> s.crawlHours
        },
    )

class SettingsDownloaderRulesPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsDownloaderRulesState())
    private val lock = Mutex()

    val state: StateFlow<SettingsDownloaderRulesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(AcquisitionPage.DOWNLOADERS)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started(), allowed = true, canEdit = me.mayEditAcquisition) }
            when (val result = api.result(DownloadsLinkEndpoints.downloaderSettings)) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            saved = result.value,
                            draft = result.value,
                            text = emptyMap(),
                            deadOpen = false,
                            saveError = null,
                        )
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    fun setSwitch(
        flag: SettingsDownloaderSwitch,
        on: Boolean,
    ) = edit {
        when (flag) {
            SettingsDownloaderSwitch.REMOVE_FAILED -> it.copy(removeFailed = on)
            SettingsDownloaderSwitch.AUTO_ADD -> it.copy(downloadsAutoAdd = on)
            SettingsDownloaderSwitch.SAB_OUTSIDE -> it.copy(downloadsSabOutside = on)
            SettingsDownloaderSwitch.DELETE_FILES -> it.copy(removeDeleteFiles = on)
            SettingsDownloaderSwitch.TURN_DOWN -> it.copy(removeTurnDown = on)
            SettingsDownloaderSwitch.SEARCH_AGAIN -> it.copy(removeSearchAgain = on)
            SettingsDownloaderSwitch.FAIL_DANGEROUS -> it.copy(failDangerous = on)
            SettingsDownloaderSwitch.REPLACE_VANISHED -> it.copy(replaceVanished = on)
            SettingsDownloaderSwitch.TRY_NEXT -> it.copy(tryNextDownloader = on)
        }
    }

    fun setNumber(
        kind: SettingsDownloaderNumber,
        raw: String,
    ) {
        val text = raw.filter { it.isDigit() || it == '.' }.take(NUMBER_LIMIT)
        val value = parseNumber(text)
        mutableState.update { s ->
            val draft =
                if (value == null) {
                    s.draft
                } else {
                    when (kind) {
                        SettingsDownloaderNumber.STALLED_AFTER -> s.draft.copy(stalledAfterHours = value)
                        SettingsDownloaderNumber.STALLED_ACT -> s.draft.copy(stalledActHours = value)
                        SettingsDownloaderNumber.SPACING -> s.draft.copy(grabSpacingSeconds = value)
                        SettingsDownloaderNumber.DEAD_MINUTES -> s.draft.copy(deadMetadataMinutes = value)
                        SettingsDownloaderNumber.CRAWL_KBPS -> s.draft.copy(crawlKbps = value)
                        SettingsDownloaderNumber.CRAWL_HOURS -> s.draft.copy(crawlHours = value)
                    }
                }
            s.copy(draft = draft, text = s.text + (kind to text), saveError = null)
        }
    }

    fun setDeadRule(on: Boolean) =
        mutableState.update { s ->
            s.copy(
                deadOpen = on,
                draft =
                    s.draft.copy(
                        deadMetadataMinutes = if (on) DEAD_ON_MINUTES else 0.0,
                        crawlKbps = if (on) DEAD_ON_KBPS else 0.0,
                        crawlHours = if (on) DEAD_ON_HOURS else 0.0,
                    ),
                text =
                    s.text -
                        setOf(
                            SettingsDownloaderNumber.DEAD_MINUTES,
                            SettingsDownloaderNumber.CRAWL_KBPS,
                            SettingsDownloaderNumber.CRAWL_HOURS,
                        ),
                saveError = null,
            )
        }

    fun discard() = mutableState.update { it.copy(draft = it.saved, text = emptyMap(), deadOpen = false, saveError = null) }

    suspend fun save() {
        val s = mutableState.value
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        api
            .result(DownloadersEndpoints.saveSettings, s.draft)
            .onSuccess { saved ->
                mutableState.update {
                    it.copy(saving = false, saved = saved, draft = saved, text = emptyMap(), savedCount = it.savedCount + 1)
                }
            }.onFailure { error -> mutableState.update { it.copy(saving = false, saveError = error) } }
    }

    private fun edit(transform: (DownloaderSettings) -> DownloaderSettings) =
        mutableState.update { it.copy(draft = transform(it.draft), saveError = null) }

    private companion object {
        const val NUMBER_LIMIT = 8
    }
}
