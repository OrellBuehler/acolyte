package ch.orellbuehler.acolyte.feature.settings.keys

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.SettingsKeysPushed

enum class SettingsKeysNoticeKind {
    KEY_REVOKED,
    KEY_REVOKE_FAILED,
    KEY_MAKE_FAILED,
    SEERR_SAVE_FAILED,
    SEERR_KEY_MAKE_FAILED,
    SEERR_KEY_REMOVED,
    SEERR_KEY_REMOVE_FAILED,
    AI_ON,
    AI_OFF,
    AI_SAVE_FAILED,
    GROUPS_SAVED,
}

data class SettingsKeysNotice(
    val seq: Int,
    val kind: SettingsKeysNoticeKind,
    val name: String = "",
    val error: ApiError? = null,
) {
    val detail: String get() = error?.let { SettingsKeysLogic.detail(it) }.orEmpty()
}

data class SettingsKeysFresh(
    val id: Long,
    val name: String,
    val key: String,
)

data class SettingsKeysAiKey(
    val id: Long,
    val label: String,
    val hint: String,
    val createdAt: String,
    val lastUsedAt: String?,
    val groups: List<String>,
)

data class SettingsSeerrFresh(
    val side: String,
    val key: String,
)

data class SettingsSeerrSide(
    val side: String,
    val path: String,
    val hasKey: Boolean,
    val hint: String,
    val owner: String,
    val ownerActive: Boolean,
    val lastUsedAt: String?,
    val busy: Boolean,
)

data class SettingsSeerrSent(
    val key: String,
    val name: String,
    val kind: String,
    val side: String,
    val updatedAt: String,
)

data class SettingsSeerrCard(
    val enabled: Boolean = false,
    val deleteFiles: Boolean = false,
    val sides: List<SettingsSeerrSide> = emptyList(),
    val sent: List<SettingsSeerrSent> = emptyList(),
)

internal class SettingsKeysNotices {
    private var seq = 0

    fun make(
        kind: SettingsKeysNoticeKind,
        name: String = "",
        error: ApiError? = null,
    ): SettingsKeysNotice = SettingsKeysNotice(++seq, kind, name, error)
}

internal object SettingsKeysLogic {
    const val LABEL_MAX = 64
    const val AI_SCOPE = "mcp"
    const val AI_FEATURE = "ai_apps"
    const val PLUGINS = "plugins"

    val offeredGroups: List<String> = listOf("see", "add", "control", "settings", "delete", "admin", "approve")
    val allGroups: List<String> = offeredGroups + PLUGINS
    val defaultGroups: List<String> = listOf("see", "add", "control", "settings")

    private val detailed = setOf(400, 403, 409, 422, 502, 503)

    fun cleanLabel(text: String): String? = text.trim().takeIf { it.isNotEmpty() && it.length <= LABEL_MAX }

    fun orderedGroups(
        ticked: Collection<String>,
        keepPlugins: Boolean = false,
    ): List<String> = allGroups.filter { if (it == PLUGINS) keepPlugins else it in ticked }

    fun knownGroups(groups: Collection<String>): List<String> = allGroups.filter { it in groups }

    fun keyHint(hint: String): String = hint.trimStart('.', '…')

    fun detail(error: ApiError): String = if (error.status in detailed) error.message else ""

    fun pushedRows(pushed: SettingsKeysPushed): List<SettingsSeerrSent> {
        val formats = pushed.formats.map { SettingsSeerrSent("f-${it.side}-${it.name}", it.name, "format", it.side, it.updatedAt) }
        val profiles = pushed.profiles.map { SettingsSeerrSent("p-${it.pickRulesId}", it.name, "profile", it.side, it.updatedAt) }
        return (formats + profiles).sortedWith(compareByDescending<SettingsSeerrSent> { it.updatedAt }.thenBy { it.name.lowercase() })
    }
}
