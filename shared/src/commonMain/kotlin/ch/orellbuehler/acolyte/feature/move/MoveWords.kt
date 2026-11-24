package ch.orellbuehler.acolyte.feature.move

import ch.orellbuehler.acolyte.api.model.MoveCheck
import ch.orellbuehler.acolyte.feature.Tone

enum class SettingsMoveStep {
    READ,
    CHOOSE,
    RUN,
    DONE,
}

enum class MoveWord {
    WILL_BE_ADDED,
    ADDED,
    WILL_USE_THE_ONE,
    USING_THE_ONE,
    WILL_BE_UPDATED,
    UPDATED,
    MATCHES_ONE_HERE,
    MATCHED,
    ALREADY_HERE,
    LEFT_OUT,
    NO_FOLDER_HERE,
    WONT_WORK,
    FAILED,
    REMOVED,
    KEPT,
    NOT_REMOVED,
    OTHER,
}

enum class MoveTone {
    OK,
    WARN,
    BAD,
    QUIET,
}

enum class MoveStateWord {
    WAITING,
    IMPORTING,
    IMPORTED,
    STOPPED,
    UNDONE,
    UNDOING,
    UNDO_FAILED,
}

enum class MoveCountKind {
    MOVIE,
    SHOW,
    ARTIST,
    TITLE,
    FOLDER,
    PICK_RULES,
    TRAIT,
    WAIT_RULE,
    SOURCE,
    DOWNLOADER,
    OUTSIDE_LIST,
    ALERT,
    LINKED_APP,
    PERSON,
    REQUEST,
    TAG,
    SUBTITLE_LANGUAGE,
    SUBTITLE_SET,
    SUBTITLE_SITE,
    KEY_TO_RETYPE,
}

enum class MoveGroup {
    FOLDERS,
    PICK_RULES,
    SOURCES,
    TITLES,
    NEVER_ADD,
    OUTSIDE_LISTS,
    DOWNLOADERS,
    TURNED_DOWN,
    NAMING,
    SUBTITLES,
    PEOPLE,
    WISHLIST,
    ALERTS,
    OTHER,
    UNKNOWN,
}

enum class MovePathProblem {
    NONE,
    FILL_BOTH,
    FULL_PATH,
    DOTS,
    TYPE_PATH,
}

enum class MoveGateReason {
    NONE,
    TURN_ON_READ,
    UPDATING,
    PROGRESS,
    FIX_FOLDER,
    FIX_RULE,
    NOTHING_NEW,
    TURN_ON_GROUP,
    SAFE_MODE,
    TEST_FAILED,
    CHANGED_SINCE_TEST,
    TRYING,
    ONE_NEEDS_CHOICE,
    MANY_NEED_CHOICE,
}

enum class MoveCheckStep {
    READING,
    FOLDERS,
    TITLES,
}

enum class MovePersonChoice {
    OWNER,
    ADMIN,
    MERGE,
    SKIP,
}

enum class MoveNamesChoice {
    KEEP,
    RENAME,
}

enum class MoveChooseWhy {
    SAME_NAME,
    NUMERIC_ID,
    MERGE_GONE,
}

enum class MoveAttention {
    NONE,
    UNREACHABLE,
    UNCHECKED,
    SEASONS,
}

enum class MoveReachFailure {
    OTHER,
    TRY_HTTP,
    CANT_REACH,
    TIMEOUT,
    TOO_BIG,
    REFUSED_KEY,
    WRONG_APP,
    UNSUPPORTED,
    SAFE_MODE,
    NOT_OWNER,
}

data class MoveGate(
    val ok: Boolean,
    val reason: MoveGateReason,
    val n: Int = 0,
)

data class MoveCountPart(
    val kind: MoveCountKind,
    val n: Int,
)

data class MoveSummaryPart(
    val n: Int,
    val word: MoveWord,
    val raw: String,
)

data class MoveProgressNote(
    val done: Int,
    val total: Int,
    val waitLeft: Int,
    val waitMinutes: Int,
    val raw: String,
) {
    val isCount: Boolean get() = total > 0
}

data class MoveCheckNote(
    val step: MoveCheckStep,
    val done: Int,
    val total: Int,
    val now: String,
    val issues: Int,
) {
    val counting: Boolean get() = step == MoveCheckStep.TITLES && total > 0
}

data class MoveApp(
    val kind: String,
    val name: String,
    val port: Int,
    val canLink: Boolean,
    val keyPath: String,
)

data class MoveFolderOutcome(
    val came: Int,
    val attention: Int,
    val attentionFolders: List<String>,
    val unchecked: Int,
    val uncheckedFolders: List<String>,
)

object MoveVocabulary {
    val apps: List<MoveApp> =
        listOf(
            MoveApp("radarr", "Radarr", 7878, false, "/settings/general"),
            MoveApp("sonarr", "Sonarr", 8989, false, "/settings/general"),
            MoveApp("lidarr", "Lidarr", 8686, false, "/settings/general"),
            MoveApp("prowlarr", "Prowlarr", 9696, true, "/settings/general"),
            MoveApp("bazarr", "Bazarr", 6767, false, "/settings/general"),
            MoveApp("seerr", "Seerr", 5055, true, "/settings/main"),
            MoveApp("overseerr", "Overseerr", 5055, true, "/settings/main"),
            MoveApp("qbittorrent", "qBittorrent", 8080, false, ""),
            MoveApp("sabnzbd", "SABnzbd", 8080, false, "/sabnzbd/config/general/"),
        )

    val downloaderSources: Set<String> = setOf("sonarr", "radarr", "lidarr")
    val linkedSources: Set<String> = setOf("seerr", "overseerr")

    fun appName(kind: String): String = apps.firstOrNull { it.kind == kind }?.name ?: kind

    fun placeholder(kind: String): String = "http://192.168.1.10:${apps.firstOrNull { it.kind == kind }?.port ?: 8080}"

    fun normalizeAddress(input: String): String {
        val text = input.trim()
        if (text.isEmpty()) return ""
        return if (SCHEME.containsMatchIn(text)) text else "http://$text"
    }

    fun hostOf(address: String): String {
        val noScheme = address.replace(SCHEME, "")
        return noScheme.substringBefore('/').ifEmpty { address }
    }

    fun group(id: String): MoveGroup =
        when (id) {
            "folders" -> MoveGroup.FOLDERS
            "pick_rules" -> MoveGroup.PICK_RULES
            "sources" -> MoveGroup.SOURCES
            "titles" -> MoveGroup.TITLES
            "never_add" -> MoveGroup.NEVER_ADD
            "outside_lists" -> MoveGroup.OUTSIDE_LISTS
            "downloaders" -> MoveGroup.DOWNLOADERS
            "turned_down" -> MoveGroup.TURNED_DOWN
            "naming" -> MoveGroup.NAMING
            "subtitles" -> MoveGroup.SUBTITLES
            "people" -> MoveGroup.PEOPLE
            "wishlist" -> MoveGroup.WISHLIST
            "alerts" -> MoveGroup.ALERTS
            "other" -> MoveGroup.OTHER
            else -> MoveGroup.UNKNOWN
        }

    private val past = mapOf("created" to "create", "adopted" to "adopt", "changed" to "change")

    private val wordsPlanned =
        mapOf(
            "create" to MoveWord.WILL_BE_ADDED,
            "adopt" to MoveWord.WILL_USE_THE_ONE,
            "change" to MoveWord.WILL_BE_UPDATED,
            "matched" to MoveWord.MATCHES_ONE_HERE,
            "already" to MoveWord.ALREADY_HERE,
            "skipped" to MoveWord.LEFT_OUT,
            "not_found" to MoveWord.NO_FOLDER_HERE,
            "failed" to MoveWord.WONT_WORK,
        )

    private val wordsAfter =
        mapOf(
            "create" to MoveWord.ADDED,
            "adopt" to MoveWord.USING_THE_ONE,
            "change" to MoveWord.UPDATED,
            "matched" to MoveWord.MATCHED,
            "already" to MoveWord.ALREADY_HERE,
            "skipped" to MoveWord.LEFT_OUT,
            "not_found" to MoveWord.NO_FOLDER_HERE,
            "failed" to MoveWord.FAILED,
        )

    private val resultOrder =
        listOf(
            "create",
            "created",
            "adopt",
            "adopted",
            "change",
            "changed",
            "matched",
            "already",
            "removed",
            "kept",
            "skipped",
            "not_found",
            "failed",
        )

    fun resultWord(
        result: String,
        after: Boolean = false,
        undo: Boolean = false,
    ): MoveWord {
        if (undo) {
            when (result) {
                "removed" -> return MoveWord.REMOVED
                "kept" -> return MoveWord.KEPT
                "failed" -> return MoveWord.NOT_REMOVED
            }
        }
        if (result == "removed") return MoveWord.REMOVED
        if (result == "kept") return MoveWord.KEPT
        val base = past[result] ?: result
        val isPast = past.containsKey(result)
        val table = if (isPast || after) wordsAfter else wordsPlanned
        return table[base] ?: MoveWord.OTHER
    }

    fun rawWord(result: String): String = result.replace('_', ' ')

    fun resultTone(result: String): MoveTone =
        when (result) {
            "create", "created", "adopt", "adopted", "change", "changed", "matched", "removed" -> MoveTone.OK
            "not_found", "kept" -> MoveTone.WARN
            "failed" -> MoveTone.BAD
            else -> MoveTone.QUIET
        }

    fun needsLook(
        result: String,
        attention: String = "",
    ): Boolean = result == "failed" || result == "not_found" || result == "skipped" || result == "kept" || attention.isNotEmpty()

    fun attention(value: String): MoveAttention =
        when (value) {
            "unreachable" -> MoveAttention.UNREACHABLE
            "unchecked" -> MoveAttention.UNCHECKED
            "seasons" -> MoveAttention.SEASONS
            else -> MoveAttention.NONE
        }

    fun chooseWhy(value: String): MoveChooseWhy =
        when (value) {
            "numeric_id" -> MoveChooseWhy.NUMERIC_ID
            "merge_gone" -> MoveChooseWhy.MERGE_GONE
            else -> MoveChooseWhy.SAME_NAME
        }

    fun summary(
        counts: Map<String, Int>,
        after: Boolean = false,
        undo: Boolean = false,
    ): List<MoveSummaryPart> =
        counts
            .filter { it.value > 0 }
            .entries
            .sortedBy { order(it.key) }
            .map { MoveSummaryPart(it.value, resultWord(it.key, after, undo), rawWord(it.key)) }

    private fun order(result: String): Int = resultOrder.indexOf(result).let { if (it < 0) resultOrder.size else it }

    fun moveTotal(counts: Map<String, Int>): Int =
        listOf("create", "created", "adopt", "adopted", "change", "changed", "matched").sumOf { counts[it] ?: 0 }

    fun stateWord(
        state: String,
        undoState: String,
    ): Pair<MoveStateWord, Tone> {
        if (undoState == "queued" || undoState == "running") return MoveStateWord.UNDOING to Tone.INFO
        if (undoState == "failed" && state != "undone") return MoveStateWord.UNDO_FAILED to Tone.FAIL
        return when (state) {
            "running" -> MoveStateWord.IMPORTING to Tone.INFO
            "done" -> MoveStateWord.IMPORTED to Tone.OK
            "failed" -> MoveStateWord.STOPPED to Tone.FAIL
            "undone" -> MoveStateWord.UNDONE to Tone.IDLE
            else -> MoveStateWord.WAITING to Tone.WAIT
        }
    }

    private val countKinds =
        mapOf(
            "titles" to MoveCountKind.TITLE,
            "folders" to MoveCountKind.FOLDER,
            "quality_profiles" to MoveCountKind.PICK_RULES,
            "custom_formats" to MoveCountKind.TRAIT,
            "delay_profiles" to MoveCountKind.WAIT_RULE,
            "indexers" to MoveCountKind.SOURCE,
            "download_clients" to MoveCountKind.DOWNLOADER,
            "import_lists" to MoveCountKind.OUTSIDE_LIST,
            "notifications" to MoveCountKind.ALERT,
            "app_links" to MoveCountKind.LINKED_APP,
            "users" to MoveCountKind.PERSON,
            "requests" to MoveCountKind.REQUEST,
            "tags" to MoveCountKind.TAG,
            "subtitle_languages" to MoveCountKind.SUBTITLE_LANGUAGE,
            "subtitle_profiles" to MoveCountKind.SUBTITLE_SET,
            "subtitle_sites" to MoveCountKind.SUBTITLE_SITE,
            "needs_reentry" to MoveCountKind.KEY_TO_RETYPE,
        )

    private val mainCount =
        mapOf(
            "radarr" to "titles",
            "sonarr" to "titles",
            "lidarr" to "titles",
            "prowlarr" to "indexers",
            "bazarr" to "subtitle_sites",
            "seerr" to "users",
            "overseerr" to "users",
            "qbittorrent" to "download_clients",
            "sabnzbd" to "download_clients",
        )

    private val bestKeys =
        mapOf(
            "radarr" to listOf("titles", "indexers", "download_clients"),
            "sonarr" to listOf("titles", "indexers", "download_clients"),
            "lidarr" to listOf("titles", "indexers", "download_clients"),
            "bazarr" to listOf("subtitle_languages", "subtitle_sites"),
            "seerr" to listOf("users", "requests"),
            "overseerr" to listOf("users", "requests"),
            "prowlarr" to listOf("indexers"),
            "qbittorrent" to listOf("download_clients"),
            "sabnzbd" to listOf("download_clients"),
        )

    private fun titleKind(kind: String): MoveCountKind =
        when (kind) {
            "radarr" -> MoveCountKind.MOVIE
            "sonarr" -> MoveCountKind.SHOW
            "lidarr" -> MoveCountKind.ARTIST
            else -> MoveCountKind.TITLE
        }

    fun bestStats(
        kind: String,
        counts: Map<String, Int>,
    ): List<MoveCountPart> =
        bestKeys[kind].orEmpty().mapNotNull { key ->
            val n = counts[key] ?: 0
            if (n <= 0) return@mapNotNull null
            val part = if (key == "titles") titleKind(kind) else countKinds[key]
            part?.let { MoveCountPart(it, n) }
        }

    fun countParts(
        kind: String,
        counts: Map<String, Int>,
        max: Int = 3,
    ): List<MoveCountPart> {
        val main = mainCount[kind]
        val known =
            counts
                .filter { (key, n) -> countKinds.containsKey(key) && n > 0 }
                .entries
                .sortedWith(
                    compareBy<Map.Entry<String, Int>> { if (it.key == main) 0 else 1 }
                        .thenByDescending { it.value }
                        .thenBy { it.key },
                )
        return known.take(max).map { (key, n) -> MoveCountPart(if (key == "titles") titleKind(kind) else countKinds.getValue(key), n) }
    }

    fun progressNote(note: String): MoveProgressNote {
        val match = PROGRESS.matchEntire(note.trim()) ?: return MoveProgressNote(0, 0, 0, 0, note)
        val (done, total, seconds, left) = match.destructured
        val wait = left.toIntOrNull() ?: 0
        val secs = seconds.toIntOrNull() ?: 0
        val minutes = if (wait > 0) maxOf(1, (secs + 59) / 60) else 0
        return MoveProgressNote(done.toInt(), total.toInt(), wait, minutes, note)
    }

    internal fun checkNote(check: MoveCheck): MoveCheckNote? {
        if (check.state != "running") return null
        val step =
            when (check.step) {
                "titles" -> MoveCheckStep.TITLES
                "folders" -> MoveCheckStep.FOLDERS
                else -> MoveCheckStep.READING
            }
        val issues = (check.counts["not_found"] ?: 0) + (check.counts["failed"] ?: 0)
        return MoveCheckNote(step, check.done, check.total, check.now, issues)
    }

    fun reachFailure(
        status: Int,
        text: String,
    ): MoveReachFailure {
        val lower = text.lowercase()
        return when {
            TRY_HTTP.containsMatchIn(lower) -> MoveReachFailure.TRY_HTTP
            status == 413 -> MoveReachFailure.TOO_BIG
            lower.contains("safe mode") -> MoveReachFailure.SAFE_MODE
            lower.contains("only the owner") -> MoveReachFailure.NOT_OWNER
            lower.contains("took too long") || status == 504 -> MoveReachFailure.TIMEOUT
            lower.contains("not the app") || lower.contains("not a ") && lower.contains("backup") -> MoveReachFailure.WRONG_APP
            lower.contains("refused the key") ||
                lower.contains("key or password") ||
                lower.contains("api key") ||
                lower.contains("sign-in") ||
                lower.contains("unauthorized") -> MoveReachFailure.REFUSED_KEY
            lower.contains("version") && (lower.contains("not one") || lower.contains("unsupported")) -> MoveReachFailure.UNSUPPORTED
            CANT_REACH.containsMatchIn(lower) || status == 502 -> MoveReachFailure.CANT_REACH
            else -> MoveReachFailure.OTHER
        }
    }

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val PROGRESS = Regex("^(\\d+) of (\\d+)\\s+(?:imported|undone)\\.?(?: wait (\\d+) (\\d+))?$")
    private val TRY_HTTP = Regex("answers on http, not https|http response to https client|does not look like a tls handshake")
    private val CANT_REACH =
        Regex(
            "connection (was )?refused|does not answer|refused to connect|no such host|timed? ?out|unreachable|dial tcp|no route|" +
                "connection reset|could not (connect|reach)|couldn.t (connect|reach)|can.t connect|eof",
        )
}
