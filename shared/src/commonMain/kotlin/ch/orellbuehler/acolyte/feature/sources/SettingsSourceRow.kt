package ch.orellbuehler.acolyte.feature.sources

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.SourceRef
import ch.orellbuehler.acolyte.feature.Tone

data class SettingsSourceRow(
    val id: Long,
    val name: String,
    val formatLabel: String,
    val format: String,
    val protocolName: String,
    val priority: Int,
    val state: String,
    val word: SettingsSourceWord,
    val rawWord: String,
    val tone: Tone,
    val note: String,
    val noteBad: Boolean,
    val showNote: Boolean,
    val until: String,
    val botCheckHint: Boolean,
    val cause: SettingsSourceCause?,
    val avgMs: Int,
    val calls24h: Int,
    val failures24h: Int,
    val lastSuccess: String,
    val lastFailure: String,
    val feed: Boolean,
    val autoSearch: Boolean,
    val handSearch: Boolean,
    val testing: Boolean,
    val busy: Boolean,
    val test: SettingsSourceOutcome?,
) {
    val showDot: Boolean get() = word != SettingsSourceWord.WORKING

    val isDisabled: Boolean get() = state == "disabled"

    val canTryNow: Boolean get() = state == "paused"

    val showAvg: Boolean get() = calls24h > 0 && avgMs > 0

    val showUntil: Boolean get() = state == "paused" && until.isNotEmpty()

    val usesNothing: Boolean get() = !feed && !autoSearch && !handSearch
}

internal fun settingsSourceRow(
    s: SourceRef,
    test: SettingsSourceOutcome?,
    testing: Boolean,
    busy: Boolean,
): SettingsSourceRow {
    val h = s.health
    val quiet = h.state == "working" || h.state == "disabled"
    val failedTest = test != null && !test.ok
    return SettingsSourceRow(
        id = s.id,
        name = s.name,
        formatLabel = SettingsSourcesLogic.formatLabel(s),
        format = s.format,
        protocolName = s.protocolName,
        priority = s.priority,
        state = s.state,
        word = SettingsSourcesLogic.word(h),
        rawWord = SettingsSourcesLogic.rawWord(h),
        tone = SettingsSourcesLogic.tone(h),
        note = h.text,
        noteBad = !quiet,
        showNote = !quiet && s.state != "paused" && !failedTest && h.text.isNotBlank(),
        until = h.until,
        botCheckHint = h.block == "bot_check" || test?.botCheck == true,
        cause = SettingsSourcesLogic.cause(h.cause),
        avgMs = s.summary.avgMs24h,
        calls24h = s.summary.calls24h,
        failures24h = s.summary.failures24h,
        lastSuccess = s.summary.lastSuccess.orEmpty(),
        lastFailure = s.summary.lastFailure.orEmpty(),
        feed = s.useFor.feed,
        autoSearch = s.useFor.autoSearch,
        handSearch = s.useFor.handSearch,
        testing = testing,
        busy = busy,
        test = test,
    )
}

enum class SettingsSourcesNoticeKind {
    SAVED,
    PAUSE_FAILED,
    RESUME_FAILED,
    REMOVE_FAILED,
    BRIDGE_CONNECTED,
    BRIDGE_DISCONNECTED,
    BRIDGE_SYNCED,
    BRIDGE_UP_TO_DATE,
    BRIDGE_SYNCING_AGAIN,
    BRIDGE_SYNC_FAILED,
}

data class SettingsSourcesNotice(
    val seq: Int,
    val kind: SettingsSourcesNoticeKind,
    val name: String = "",
    val detail: String = "",
    val count: Int = 0,
    val added: Int = 0,
    val removed: Int = 0,
    val changed: Int = 0,
    val error: ApiError? = null,
)
