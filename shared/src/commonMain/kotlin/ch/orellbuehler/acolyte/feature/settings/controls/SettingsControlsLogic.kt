package ch.orellbuehler.acolyte.feature.settings.controls

import ch.orellbuehler.acolyte.api.model.SettingsLinkTest
import ch.orellbuehler.acolyte.api.model.SettingsScheduleRow
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.downloads.Days
import ch.orellbuehler.acolyte.feature.settings.SettingsPage
import kotlin.math.roundToLong
import kotlin.time.Instant

enum class SettingsControlsKind {
    PART_ON,
    PART_OFF,
    PART_RESTART,
    STAGE_PAUSE,
    STAGE_RESUME,
    STAGE_RESTART,
    LINK_RECONNECT,
    LINK_TEST,
    APP_RESTART,
    APP_SAFE_MODE,
    APP_SHUTDOWN,
}

enum class SettingsControlsRun {
    OFF,
    STARTING,
    RUNNING,
    RETRYING,
    FAILED,
    STUCK,
}

enum class SettingsControlsLinkKind {
    SOURCE,
    DOWNLOADER,
    MEDIA_SERVER,
    SUBTITLE_SITE,
    OTHER,
}

enum class SettingsControlsScheduleGroup {
    LIBRARY,
    COPIES,
    METADATA,
    UPKEEP,
}

enum class SettingsControlsEveryUnit {
    ALWAYS,
    DAYS,
    HOURS,
    MINUTES,
    SECONDS,
}

data class SettingsControlsEvery(
    val unit: SettingsControlsEveryUnit,
    val count: Long,
)

enum class SettingsControlsNextKind {
    NONE,
    TURNED_OFF,
    PART_OFF,
    SAFE_MODE,
    RUNNING,
    QUEUED,
    NOW,
    MINUTES,
    HOURS,
    DAYS,
}

data class SettingsControlsNext(
    val kind: SettingsControlsNextKind,
    val count: Int = 0,
)

enum class SettingsControlsDot {
    OK,
    BUSY,
    FAIL,
    OFF,
}

enum class SettingsControlsReason {
    NONE,
    NOTHING,
    SETTING_OFF,
    NOBODY,
    STARTING,
    PLEX_OFF,
    PLEX_NOBODY,
}

enum class SettingsControlsStale {
    NONE,
    REFRESH_OFF,
    RESCAN_OFF,
}

enum class SettingsControlsUnit {
    HOURS,
    MINUTES,
}

internal class SettingsControlsLink(
    val kind: String,
    val id: String,
)

internal class SettingsControlsSeen(
    val at: String,
    val checked: Boolean,
    val why: String,
    val tookSeconds: Double?,
)

internal class SettingsControlsKindInfo(
    val group: SettingsControlsScheduleGroup,
    val label: String,
    val about: String,
    val min: Long,
    val max: Long,
    val step: Long,
    val page: SettingsPage? = null,
)

internal object SettingsControlsLogic {
    private const val MINUTE = 60L
    private const val HOUR = 3600L
    private const val DAY = 86400L
    private val TAIL = Regex(", send the same request again.*$")
    private val LINK = Regex("^link:([a-z_]+):(.+)$")
    private val LINK_KINDS = setOf("downloader", "source", "media_server", "subtitle_site")
    private val WORD = Regex("[a-z]+")
    private val VERSION = Regex("v?(\\d+(?:\\.\\d+)+)")
    private val VERSION_BLOCKERS = "./:@-"

    private val APPS =
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

    val kinds: Map<String, SettingsControlsKindInfo> =
        mapOf(
            "refresh_titles" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.METADATA,
                    "Refresh movie details",
                    "Reads names, dates and artwork of every movie again from TMDB.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                ),
            "refresh_shows" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.METADATA,
                    "Refresh show details",
                    "Reads seasons, episodes and air dates of every show again.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                ),
            "refresh_artists" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.METADATA,
                    "Refresh artist details",
                    "Reads albums and Pressings of every artist again from MusicBrainz.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                ),
            "artist_pictures" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.METADATA,
                    "Find artist pictures",
                    "Fills missing artist photos, wide backgrounds and logos from the HolyArrs metadata server.",
                    HOUR,
                    7 * DAY,
                    HOUR,
                ),
            "check_library_folders" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.LIBRARY,
                    "Check Library folders",
                    "Checks that every Library folder is there and has free space. " +
                        "The write and hardlink test runs at start-up, on Run now and when folders change.",
                    HOUR,
                    DAY,
                    HOUR,
                ),
            "sync_outside_lists" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.METADATA,
                    "Read Outside lists",
                    "Adds what TMDB and Trakt lists hold now.",
                    15 * MINUTE,
                    DAY,
                    5 * MINUTE,
                ),
            "check_bridges" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.UPKEEP,
                    "Check Jackett",
                    "Keeps the Sources from a connected Jackett in step with its sites.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                    SettingsPage.SOURCES,
                ),
            "sync_plex_watchlists" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.UPKEEP,
                    "Read Plex watchlists",
                    "Asks for what is new on the Plex watchlists of people who turned it on.",
                    3 * MINUTE,
                    HOUR,
                    MINUTE,
                    SettingsPage.REQUESTS,
                ),
            "feed_check" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.COPIES,
                    "Feed check",
                    "Reads the newest Releases of every Source with a feed.",
                    10 * MINUTE,
                    120 * MINUTE,
                    MINUTE,
                    SettingsPage.SEARCH,
                ),
            "grab_held" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.COPIES,
                    "Try Held releases again",
                    "Sends Held releases whose wait is over.",
                    MINUTE,
                    HOUR,
                    MINUTE,
                ),
            "search_missing" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.COPIES,
                    "Search for missing",
                    "Searches the Sources for missing titles, a few at a time.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                    SettingsPage.SEARCH,
                ),
            "rescan_library" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.LIBRARY,
                    "Rescan the Library",
                    "Looks for files added, changed or removed by hand.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                ),
            "clean_holding" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.LIBRARY,
                    "Clean the holding folder",
                    "Removes deleted files whose keep time is over.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                ),
            "health_check" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.UPKEEP,
                    "Health checks",
                    "Library folders, free space, Sources, Downloaders, backups and the update check.",
                    MINUTE,
                    DAY,
                    MINUTE,
                    SettingsPage.THIS_APP,
                ),
            "clean_releases" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.UPKEEP,
                    "Clean up old Releases",
                    "Removes search results older than 14 days that were not picked, held, turned down or sent.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                ),
            "clean_history" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.UPKEEP,
                    "Clean up History",
                    "Removes History Events older than their keep time.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                    SettingsPage.THIS_APP,
                ),
            "subtitle_missing" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.COPIES,
                    "Look for missing subtitles",
                    "Searches the subtitle sites for every file missing a wished language.",
                    HOUR,
                    7 * DAY,
                    HOUR,
                    SettingsPage.SUBTITLES,
                ),
            "subtitle_better" to
                SettingsControlsKindInfo(
                    SettingsControlsScheduleGroup.COPIES,
                    "Look for better subtitles",
                    "Searches again for subtitles under their Pass mark.",
                    6 * HOUR,
                    7 * DAY,
                    HOUR,
                    SettingsPage.SUBTITLES,
                ),
        )

    fun question(text: String): String = text.replace(TAIL, ".").trim()

    fun link(key: String): SettingsControlsLink? {
        val match = LINK.find(key) ?: return null
        val kind = match.groupValues[1]
        return if (kind in LINK_KINDS) SettingsControlsLink(kind, match.groupValues[2]) else null
    }

    fun linkKind(kind: String): SettingsControlsLinkKind =
        when (kind) {
            "source" -> SettingsControlsLinkKind.SOURCE
            "downloader" -> SettingsControlsLinkKind.DOWNLOADER
            "media_server" -> SettingsControlsLinkKind.MEDIA_SERVER
            "subtitle_site" -> SettingsControlsLinkKind.SUBTITLE_SITE
            else -> SettingsControlsLinkKind.OTHER
        }

    fun run(
        state: String,
        stuck: Boolean,
    ): SettingsControlsRun =
        when {
            stuck -> SettingsControlsRun.STUCK
            state == "off" -> SettingsControlsRun.OFF
            state == "starting" -> SettingsControlsRun.STARTING
            state == "retrying" -> SettingsControlsRun.RETRYING
            state == "failed" -> SettingsControlsRun.FAILED
            else -> SettingsControlsRun.RUNNING
        }

    fun tone(run: SettingsControlsRun): Tone =
        when (run) {
            SettingsControlsRun.RUNNING -> Tone.OK
            SettingsControlsRun.FAILED -> Tone.FAIL
            SettingsControlsRun.OFF -> Tone.IDLE
            else -> Tone.WAIT
        }

    fun testApp(text: String): String {
        for (word in WORD.findAll(text.lowercase())) {
            APPS[word.value]?.let { return it }
        }
        return ""
    }

    fun testVersion(text: String): String {
        for (match in VERSION.findAll(text)) {
            val start = match.range.first
            val before = if (start > 0) text[start - 1] else null
            if (before != null && (before.isLetterOrDigit() || before == '_' || before in VERSION_BLOCKERS)) continue
            return "v" +
                match.groupValues[1]
                    .split('.')
                    .take(3)
                    .joinToString(".")
        }
        return ""
    }

    fun testLabel(text: String): String = listOf(testApp(text), testVersion(text)).filter { it.isNotEmpty() }.joinToString(" ")

    fun linkSaid(test: SettingsLinkTest): String =
        listOf(test.text, test.message, test.error, test.found).firstOrNull { it.isNotEmpty() }.orEmpty()

    fun bytes(size: Long): String {
        if (size <= 0) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = size.toDouble()
        var index = 0
        while (value >= 1024 && index < units.size - 1) {
            value /= 1024
            index++
        }
        val text = if (index == 0 || value >= 10) value.roundToLong().toString() else oneDecimal(value)
        return "$text ${units[index]}"
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToLong()
        return "${tenths / 10}.${tenths % 10}"
    }

    fun duration(seconds: Double): String {
        if (seconds < 0) return ""
        if (seconds < 60) return "${seconds.roundToLong()}s"
        val minutes = (seconds / 60).toLong()
        if (minutes < 60) return "${minutes}m"
        val hours = minutes / 60
        if (hours < 48) return "${hours}h ${minutes % 60}m"
        return "${hours / 24}d ${hours % 24}h"
    }

    fun every(seconds: Long): SettingsControlsEvery =
        when {
            seconds <= 0 -> SettingsControlsEvery(SettingsControlsEveryUnit.ALWAYS, 0)
            seconds % DAY == 0L -> SettingsControlsEvery(SettingsControlsEveryUnit.DAYS, seconds / DAY)
            seconds % HOUR == 0L -> SettingsControlsEvery(SettingsControlsEveryUnit.HOURS, seconds / HOUR)
            seconds % MINUTE == 0L -> SettingsControlsEvery(SettingsControlsEveryUnit.MINUTES, seconds / MINUTE)
            else -> SettingsControlsEvery(SettingsControlsEveryUnit.SECONDS, seconds)
        }

    fun intervalUnit(step: Long): SettingsControlsUnit =
        if (step > 0 &&
            step % HOUR == 0L
        ) {
            SettingsControlsUnit.HOURS
        } else {
            SettingsControlsUnit.MINUTES
        }

    fun unitSize(unit: SettingsControlsUnit): Long = if (unit == SettingsControlsUnit.HOURS) HOUR else MINUTE

    fun intervalValid(
        seconds: Long,
        min: Long,
        max: Long,
        step: Long,
    ): Boolean = seconds in min..max && (step <= 0 || seconds % step == 0L)

    fun group(kind: String): SettingsControlsScheduleGroup = kinds[kind]?.group ?: SettingsControlsScheduleGroup.UPKEEP

    fun page(row: SettingsScheduleRow): SettingsPage? {
        val href = row.settings.ifEmpty { return kinds[row.kind]?.page }
        return SettingsPage.entries.firstOrNull { it.href == href } ?: kinds[row.kind]?.page
    }

    fun until(
        iso: String,
        now: Instant,
    ): SettingsControlsNext {
        val at = Days.parseInstant(iso) ?: return SettingsControlsNext(SettingsControlsNextKind.NONE)
        val seconds = ((at - now).inWholeMilliseconds / 1000.0).roundToLong()
        return when {
            seconds <= 30 -> SettingsControlsNext(SettingsControlsNextKind.NOW)
            seconds < HOUR -> SettingsControlsNext(SettingsControlsNextKind.MINUTES, (seconds / 60.0).roundToLong().toInt())
            seconds < DAY -> SettingsControlsNext(SettingsControlsNextKind.HOURS, (seconds / 3600.0).roundToLong().toInt())
            else -> SettingsControlsNext(SettingsControlsNextKind.DAYS, (seconds / 86400.0).roundToLong().toInt())
        }
    }

    fun next(
        row: SettingsScheduleRow,
        now: Instant,
    ): SettingsControlsNext =
        when {
            !row.enabled -> SettingsControlsNext(SettingsControlsNextKind.TURNED_OFF)
            row.state == "part_off" -> SettingsControlsNext(SettingsControlsNextKind.PART_OFF)
            row.state == "safe_mode" -> SettingsControlsNext(SettingsControlsNextKind.SAFE_MODE)
            row.state == "running" -> SettingsControlsNext(SettingsControlsNextKind.RUNNING)
            row.state == "queued" -> SettingsControlsNext(SettingsControlsNextKind.QUEUED)
            else -> until(row.nextRun, now)
        }

    private fun later(
        a: String,
        b: String,
    ): Boolean {
        val x = Days.parseInstant(a) ?: return false
        val y = Days.parseInstant(b) ?: return false
        return x > y
    }

    fun lastRan(
        lastRun: String,
        lastJobEnded: String,
    ): String {
        if (lastRun.isEmpty() || lastJobEnded.isEmpty()) return lastRun.ifEmpty { lastJobEnded }
        return if (later(lastJobEnded, lastRun)) lastJobEnded else lastRun
    }

    fun lastSeen(row: SettingsScheduleRow): SettingsControlsSeen {
        val ran = lastRan(row.lastRun, row.lastJobEnded)
        val checked = row.lastChecked
        if (checked.isNotEmpty() && (ran.isEmpty() || later(checked, ran))) {
            return SettingsControlsSeen(checked, true, row.lastSkip.ifEmpty { "nothing" }, null)
        }
        if (ran.isEmpty()) return SettingsControlsSeen("", false, "", null)
        val byJob = ran == row.lastJobEnded
        val why = if (byJob) row.lastJobNote else ""
        val took = if (byJob && row.lastJobSeconds > 0) row.lastJobSeconds else null
        return SettingsControlsSeen(ran, false, why, took)
    }

    fun reason(
        kind: String,
        code: String,
    ): SettingsControlsReason {
        val plex = kind == "sync_plex_watchlists"
        return when (code) {
            "" -> SettingsControlsReason.NONE
            "setting_off" -> if (plex) SettingsControlsReason.PLEX_OFF else SettingsControlsReason.SETTING_OFF
            "nobody" -> if (plex) SettingsControlsReason.PLEX_NOBODY else SettingsControlsReason.NOTHING
            "starting" -> SettingsControlsReason.STARTING
            else -> SettingsControlsReason.NOTHING
        }
    }

    fun reasonLinks(code: String): Boolean = code == "setting_off" || code == "nobody"

    fun dot(row: SettingsScheduleRow): SettingsControlsDot =
        when {
            row.state == "running" || row.state == "queued" -> SettingsControlsDot.BUSY
            row.lastJob == "failed" -> SettingsControlsDot.FAIL
            row.state == "waiting" -> SettingsControlsDot.OK
            else -> SettingsControlsDot.OFF
        }

    fun inLine(row: SettingsScheduleRow): Boolean = row.state == "running" || row.state == "queued"

    fun mayRun(row: SettingsScheduleRow): Boolean = row.state != "part_off" && row.state != "safe_mode"

    fun toggleable(kind: String): Boolean = kind == "refresh_titles" || kind == "rescan_library"

    fun stale(row: SettingsScheduleRow): SettingsControlsStale =
        when {
            row.enabled -> SettingsControlsStale.NONE
            row.kind == "refresh_titles" -> SettingsControlsStale.REFRESH_OFF
            row.kind == "rescan_library" -> SettingsControlsStale.RESCAN_OFF
            else -> SettingsControlsStale.NONE
        }

    fun historyChoices(current: Int): List<Int> = if (current in HISTORY_DAYS) HISTORY_DAYS else HISTORY_DAYS + current

    val HISTORY_DAYS: List<Int> = listOf(0, 7, 14, 30, 90, 180, 365)

    val CACHE_GB: List<Int> = listOf(1, 2, 5, 10, 20)
}
