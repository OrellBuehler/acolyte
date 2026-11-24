package ch.orellbuehler.acolyte.feature.settings.status

import ch.orellbuehler.acolyte.api.model.SettingsStatusInstall
import ch.orellbuehler.acolyte.api.model.SettingsUpdateInfo

enum class SettingsStatusStanding {
    LOADING,
    OFF,
    BEHIND,
    PROBLEM,
    NONE,
    CURRENT,
    UNCHECKED,
}

enum class SettingsStatusMode(
    val wire: String,
) {
    OFF("off"),
    ASK("ask"),
    AUTO("auto"),
}

enum class SettingsStatusPhase {
    IDLE,
    DOWNLOADING,
    CHECKING,
    READY,
    WAITING,
    COPYING,
    RESTARTING,
    FAILED,
}

enum class SettingsStatusAboutLoad {
    LOADING,
    READY,
    DENIED,
    FAILED,
}

data class SettingsStatusAbout(
    val version: String = "",
    val go: String = "",
    val os: String = "",
    val container: Boolean = false,
    val sqlite: String = "",
    val migration: Int = 0,
    val configDir: String = "",
    val startDir: String = "",
    val uptimeSeconds: Long = 0,
    val startedAt: String = "",
    val hasDetails: Boolean = false,
    val hasServer: Boolean = false,
    val writes: Long = 0,
    val writeWaitP95Ms: Double = 0.0,
    val urlBase: String = "",
    val liveClients: Int = 0,
    val loadedAt: Long = 0,
)

data class SettingsStatusInstallView(
    val available: Boolean = false,
    val mode: SettingsStatusMode = SettingsStatusMode.OFF,
    val modeLocked: Boolean = false,
    val checkOn: Boolean = false,
    val fromUpdate: Boolean = false,
    val previous: String = "",
    val wentBack: Boolean = false,
    val wentBackTo: String = "",
    val wentBackLeft: String = "",
    val rollback: String = "",
    val execError: String = "",
    val phase: SettingsStatusPhase = SettingsStatusPhase.IDLE,
    val progressVersion: String = "",
    val progressMessage: String = "",
    val ready: String = "",
    val oneWay: Boolean = false,
    val running: String = "",
)

object SettingsStatusLinks {
    const val SPONSOR = "https://github.com/sponsors/CoderLuii"
    const val COFFEE = "https://buymeacoffee.com/coderluii"
    const val STAR = "https://github.com/HolyArrs/HolyArrs"
    const val NOTE = "mailto:support@holyarrs.com?subject=A%20note%20about%20HolyArrs"
}

internal object SettingsStatusUpdates {
    const val RELEASE_PAGES = "https://github.com/HolyArrs/HolyArrs/releases/"

    private val MOVING =
        setOf(
            SettingsStatusPhase.DOWNLOADING,
            SettingsStatusPhase.CHECKING,
            SettingsStatusPhase.WAITING,
            SettingsStatusPhase.COPYING,
            SettingsStatusPhase.RESTARTING,
        )

    private val RELEASE_TAIL = Regex("^[A-Za-z0-9._~%+\\-/]*$")

    private val apps =
        mapOf(
            "qbittorrent" to "qBittorrent",
            "sabnzbd" to "SABnzbd",
            "transmission" to "Transmission",
            "nzbget" to "NZBGet",
            "deluge" to "Deluge",
            "rtorrent" to "rTorrent",
            "aria" to "aria2",
            "prowlarr" to "Prowlarr",
            "jackett" to "Jackett",
            "sonarr" to "Sonarr",
            "radarr" to "Radarr",
            "lidarr" to "Lidarr",
            "readarr" to "Readarr",
            "bazarr" to "Bazarr",
            "whisparr" to "Whisparr",
            "overseerr" to "Overseerr",
            "jellyseerr" to "Jellyseerr",
            "jellyfin" to "Jellyfin",
            "emby" to "Emby",
            "plex" to "Plex",
        )

    fun plainVersion(version: String): String = version.trim().removePrefix("v").removePrefix("V")

    fun standing(info: SettingsUpdateInfo?): SettingsStatusStanding =
        when {
            info == null -> SettingsStatusStanding.LOADING
            !info.on -> SettingsStatusStanding.OFF
            info.newer -> SettingsStatusStanding.BEHIND
            info.problem.isNotEmpty() -> SettingsStatusStanding.PROBLEM
            info.noRelease -> SettingsStatusStanding.NONE
            info.checkedAt.isNotEmpty() -> SettingsStatusStanding.CURRENT
            else -> SettingsStatusStanding.UNCHECKED
        }

    fun releaseLink(link: String): String {
        if (!link.startsWith(RELEASE_PAGES)) return ""
        val tail = link.removePrefix(RELEASE_PAGES)
        if (!RELEASE_TAIL.matches(tail) || tail.contains("..")) return ""
        return link
    }

    fun noteLines(info: SettingsUpdateInfo?): List<String> =
        if (info != null &&
            info.newer &&
            info.notes.isNotEmpty()
        ) {
            info.notes
                .split('\n')
                .map { it.trimEnd('\r') }
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }

    fun phase(install: SettingsStatusInstall?): SettingsStatusPhase =
        when (install?.progress?.phase) {
            "downloading" -> SettingsStatusPhase.DOWNLOADING
            "checking" -> SettingsStatusPhase.CHECKING
            "ready" -> SettingsStatusPhase.READY
            "waiting" -> SettingsStatusPhase.WAITING
            "copying" -> SettingsStatusPhase.COPYING
            "restarting" -> SettingsStatusPhase.RESTARTING
            "failed" -> SettingsStatusPhase.FAILED
            else -> SettingsStatusPhase.IDLE
        }

    fun moving(install: SettingsStatusInstall?): Boolean = phase(install) in MOVING

    fun view(install: SettingsStatusInstall?): SettingsStatusInstallView? =
        install?.let {
            SettingsStatusInstallView(
                available = it.available,
                mode = mode(it),
                modeLocked = it.modeLocked,
                checkOn = it.checkOn,
                fromUpdate = it.source == "update",
                previous = plainVersion(it.previous),
                wentBack = it.launch.chosen && it.launch.left.isNotEmpty(),
                wentBackTo = plainVersion(it.launch.to),
                wentBackLeft = plainVersion(it.launch.left),
                rollback = it.launch.rollback,
                execError = it.launch.execError,
                phase = phase(it),
                progressVersion = plainVersion(it.progress.version),
                progressMessage = it.progress.message,
                ready = plainVersion(it.ready),
                oneWay = it.oneWay,
                running = plainVersion(it.running),
            )
        }

    fun mode(install: SettingsStatusInstall?): SettingsStatusMode =
        SettingsStatusMode.entries.firstOrNull { it.wire == install?.mode } ?: SettingsStatusMode.OFF

    fun testVersion(text: String): String {
        val match =
            Regex("v?(\\d+(?:\\.\\d+)+)").findAll(text).firstOrNull { m ->
                val start = m.range.first
                start == 0 || !(text[start - 1].isLetterOrDigit() || text[start - 1] in "_./:@-")
            } ?: return ""
        return "v" +
            match.groupValues[1]
                .split('.')
                .take(3)
                .joinToString(".")
    }

    fun testApp(text: String): String {
        for (word in Regex("[a-z]+").findAll(text.lowercase())) {
            apps[word.value]?.let { return it }
        }
        return ""
    }

    fun testLabel(text: String): String = listOf(testApp(text), testVersion(text)).filter { it.isNotEmpty() }.joinToString(" ")
}
