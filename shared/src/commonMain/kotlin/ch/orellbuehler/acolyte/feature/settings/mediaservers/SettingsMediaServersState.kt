package ch.orellbuehler.acolyte.feature.settings.mediaservers

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersInput
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersLabels
import ch.orellbuehler.acolyte.api.model.SettingsMediaServersTracks
import ch.orellbuehler.acolyte.api.model.SubtitleLanguage
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase

internal enum class SettingsMediaServersKind(
    val wire: String,
    val displayName: String,
    val urlExample: String,
) {
    PLEX("plex", "Plex", "http://192.168.1.10:32400"),
    JELLYFIN("jellyfin", "Jellyfin", "http://192.168.1.10:8096"),
    EMBY("emby", "Emby", "http://192.168.1.10:8096"),
    SUBSONIC("subsonic", "Navidrome / Subsonic", "http://192.168.1.10:4533"),
    KODI("kodi", "Kodi", "http://192.168.1.10:8080"),
    ;

    companion object {
        fun of(wire: String): SettingsMediaServersKind? = entries.firstOrNull { it.wire == wire }
    }
}

data class SettingsMediaServersKindOption(
    val wire: String,
    val name: String,
)

data class SettingsMediaServersTag(
    val ok: Boolean,
    val text: String,
    val app: String,
    val libraries: Int,
    val hover: String,
    val failure: String,
    val error: ApiError?,
    val helpUrl: String,
) {
    fun label(
        connected: String,
        extra: String,
    ): String = SettingsMediaServersLogic.testLabel(text, app, extra, connected)
}

data class SettingsMediaServersRow(
    val id: Long,
    val name: String,
    val kind: String,
    val kindName: String,
    val url: String,
    val enabled: Boolean,
    val onImport: Boolean,
    val onRename: Boolean,
    val onDelete: Boolean,
    val pathFrom: String,
    val pathTo: String,
    val lastOkAt: String,
    val lastError: String,
    val tag: SettingsMediaServersTag?,
    val testing: Boolean,
    val busy: Boolean,
) {
    val toldNothing: Boolean get() = !onImport && !onRename && !onDelete

    val hasPathMap: Boolean get() = pathFrom.isNotEmpty()

    val showsFailure: Boolean get() = tag?.ok == false

    val showsLastError: Boolean get() = !showsFailure && lastError.isNotEmpty()

    val showsLastChecked: Boolean get() = !showsFailure && lastError.isEmpty() && lastOkAt.isNotEmpty()
}

data class SettingsMediaServersForm(
    val id: Long = 0,
    val kind: String = SettingsMediaServersKind.JELLYFIN.wire,
    val name: String = "",
    val originalName: String = "",
    val url: String = "",
    val username: String = "",
    val token: String = "",
    val tokenStored: Boolean = false,
    val webUrl: String = "",
    val plainPassword: Boolean = false,
    val onImport: Boolean = true,
    val onRename: Boolean = true,
    val onDelete: Boolean = true,
    val pathFrom: String = "",
    val pathTo: String = "",
    val allowLocalNetwork: Boolean = true,
    val enabled: Boolean = true,
    val usePlexSignIn: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L

    val isPlex: Boolean get() = kind == SettingsMediaServersKind.PLEX.wire

    val isSubsonic: Boolean get() = kind == SettingsMediaServersKind.SUBSONIC.wire

    val isKodi: Boolean get() = kind == SettingsMediaServersKind.KODI.wire

    val takesUsername: Boolean get() = isSubsonic || isKodi

    val takesWebUrl: Boolean get() = kind == SettingsMediaServersKind.JELLYFIN.wire || kind == SettingsMediaServersKind.EMBY.wire

    val takesPathMap: Boolean get() = !isSubsonic

    val showsToken: Boolean get() = !(isPlex && usePlexSignIn)

    val urlExample: String get() = SettingsMediaServersKind.of(kind)?.urlExample.orEmpty()

    val canSave: Boolean get() = name.isNotBlank()

    internal fun input(): SettingsMediaServersInput =
        SettingsMediaServersInput(
            name = name.trim(),
            kind = if (isNew) kind else null,
            url = url.trim(),
            token = if (showsToken) token.trim().ifEmpty { null } else null,
            username = if (takesUsername) username.trim().ifEmpty { null } else null,
            enabled = enabled,
            onImport = onImport,
            onRename = onRename,
            onDelete = onDelete,
            pathFrom = if (takesPathMap) pathFrom.trim() else "",
            pathTo = if (takesPathMap) pathTo.trim() else "",
            allowLocalNetwork = allowLocalNetwork,
            plainPassword = if (isSubsonic) plainPassword else null,
            webUrl = if (takesWebUrl) webUrl.trim() else "",
            usePlexSignIn = if (isNew && isPlex && usePlexSignIn) true else null,
        )
}

data class SettingsMediaServersPlexRow(
    val id: String,
    val name: String,
    val address: String,
    val place: String,
    val urls: List<String>,
)

data class SettingsMediaServersPlexPick(
    val finding: Boolean = false,
    val searched: Boolean = false,
    val rows: List<SettingsMediaServersPlexRow> = emptyList(),
    val picked: Int = -1,
    val notLinked: Boolean = false,
    val error: ApiError? = null,
) {
    val addresses: List<String> get() = rows.getOrNull(picked)?.urls.orEmpty()
}

data class SettingsMediaServersTrackRow(
    val index: Int,
    val code: String,
    val name: String,
    val options: List<SubtitleLanguage>,
    val canUp: Boolean,
    val canDown: Boolean,
)

enum class SettingsMediaServersNoticeKind {
    SAVED,
    REMOVE_FAILED,
    WATCH_ON,
    WATCH_OFF,
    WATCH_FAILED,
    LABELS_SAVED,
    LABELS_FAILED,
    LABELS_STARTED,
    LABELS_START_FAILED,
    TRACKS_SAVED,
    TRACKS_SAVED_ON,
    TRACKS_FAILED,
}

data class SettingsMediaServersNotice(
    val id: Long,
    val kind: SettingsMediaServersNoticeKind,
    val name: String = "",
    val error: ApiError? = null,
) {
    val detail: String get() = SettingsMediaServersLogic.detail(error)
}

data class SettingsMediaServersState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val isOwner: Boolean = false,
    val mediaOn: Boolean = false,
    val subtitlesOn: Boolean = false,
    val canPickPlex: Boolean = false,
    val servers: List<SettingsMediaServersRow> = emptyList(),
    val form: SettingsMediaServersForm? = null,
    val formRevision: Int = 0,
    val formError: ApiError? = null,
    val saving: Boolean = false,
    val plex: SettingsMediaServersPlexPick = SettingsMediaServersPlexPick(),
    val watchLoaded: Boolean = false,
    val watchOn: Boolean = false,
    val watchBusy: Boolean = false,
    val labels: SettingsMediaServersLabels? = null,
    val labelDraft: SettingsMediaServersLabels = SettingsMediaServersLabels(),
    val cardsRevision: Int = 0,
    val labelBusy: Boolean = false,
    val labelRunBusy: Boolean = false,
    val tracks: SettingsMediaServersTracks? = null,
    val trackDraft: SettingsMediaServersTracks = SettingsMediaServersTracks(),
    val trackBusy: Boolean = false,
    val languages: List<SubtitleLanguage> = emptyList(),
    val notice: SettingsMediaServersNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && servers.isEmpty()

    val formOpen: Boolean get() = form != null

    val formDetail: String get() = SettingsMediaServersLogic.detail(formError)

    val kinds: List<SettingsMediaServersKindOption>
        get() = SettingsMediaServersKind.entries.map { SettingsMediaServersKindOption(it.wire, it.displayName) }

    val hasPlex: Boolean get() = servers.any { it.kind == SettingsMediaServersKind.PLEX.wire }

    val hasTrackServer: Boolean
        get() =
            servers.any {
                it.kind == SettingsMediaServersKind.PLEX.wire ||
                    it.kind == SettingsMediaServersKind.JELLYFIN.wire ||
                    it.kind == SettingsMediaServersKind.EMBY.wire
            }

    val canSave: Boolean get() = form?.canSave == true && !saving

    val showsPlexPicker: Boolean get() = canPickPlex && form?.isNew == true && form.isPlex

    val showWatch: Boolean get() = isOwner && mediaOn && watchLoaded

    val showLabels: Boolean get() = isOwner && mediaOn && hasPlex && labels != null

    val showTracks: Boolean get() = isOwner && mediaOn && subtitlesOn && hasTrackServer && tracks != null

    val labelsChanged: Boolean
        get() {
            val saved = labels ?: return false
            return saved.enabled != labelDraft.enabled || saved.label != labelDraft.label.trim() || saved.askers != labelDraft.askers
        }

    val labelsCanSave: Boolean
        get() = labelsChanged && !labelBusy && !(labelDraft.enabled && labelDraft.label.isBlank())

    val labelsCanRun: Boolean get() = labels?.enabled == true && !labelRunBusy

    val tracksChanged: Boolean get() = tracks != null && tracks != trackDraft

    val tracksCanSave: Boolean
        get() = tracksChanged && !trackBusy && !(trackDraft.enabled && trackDraft.audio.isEmpty() && trackDraft.subtitles.isEmpty())

    val canAddAudio: Boolean get() = trackDraft.audio.size < SettingsMediaServersLogic.MAX_TRACK_LANGUAGES && languages.isNotEmpty()

    val canAddSubtitle: Boolean
        get() =
            trackDraft.subtitles.size < SettingsMediaServersLogic.MAX_TRACK_LANGUAGES &&
                SettingsMediaServersLogic.OFF !in trackDraft.subtitles &&
                languages.isNotEmpty()

    val audioRows: List<SettingsMediaServersTrackRow> get() = trackRows(trackDraft.audio, false)

    val subtitleRows: List<SettingsMediaServersTrackRow> get() = trackRows(trackDraft.subtitles, true)

    private fun trackRows(
        list: List<String>,
        subtitles: Boolean,
    ): List<SettingsMediaServersTrackRow> {
        val off = SettingsMediaServersLogic.OFF
        return list.mapIndexed { index, code ->
            val free = languages.filter { it.code == code || it.code !in list }
            SettingsMediaServersTrackRow(
                index = index,
                code = code,
                name = languages.firstOrNull { it.code == code }?.name ?: code,
                options = if (subtitles) listOf(SubtitleLanguage(off, "")) + free else free,
                canUp = index > 0 && code != off,
                canDown = index < list.lastIndex && code != off && list[index + 1] != off,
            )
        }
    }
}
