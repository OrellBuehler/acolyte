package ch.orellbuehler.acolyte.feature.settings.status

import ch.orellbuehler.acolyte.api.ApiError

enum class SettingsStatusRead {
    PARTS,
    CHECKS,
    JOBS,
    LISTS,
    ALERTS,
}

enum class SettingsStatusNoticeKind {
    CHECKS_RAN,
    CHECKS_FAILED,
    ROW_WORKING,
    ROW_WORD,
    ROW_CONNECTED,
    ROW_CANT_CONNECT,
    ROW_CANT_CHECK,
    JOB_STARTED,
    JOB_ALREADY,
    UPDATE_CHECK_FAILED,
    UPDATE_SAVED,
    UPDATE_SAVE_FAILED,
    UPDATE_INSTALL_FAILED,
    UPDATE_GO_BACK_FAILED,
    BUNDLE_FAILED,
}

data class SettingsStatusNotice(
    val id: Int,
    val kind: SettingsStatusNoticeKind,
    val row: SettingsStatusRow? = null,
    val detail: String = "",
    val error: ApiError? = null,
)

data class SettingsStatusJobLine(
    val row: SettingsStatusRow,
    val due: SettingsStatusDue,
)
