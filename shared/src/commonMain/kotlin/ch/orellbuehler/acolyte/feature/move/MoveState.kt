package ch.orellbuehler.acolyte.feature.move

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Tone

enum class MoveDialog {
    NONE,
    BEFORE,
    CONFIRM,
    UNDO,
}

enum class MoveDoneLook {
    READY,
    PENDING,
    FAILED,
}

enum class MoveGroupNote {
    NONE,
    LEFT_OUT,
    INVITE,
    KEYS,
    KEYS_OR_PASSWORDS,
    CHECK_SHOWS,
}

enum class MoveListKind {
    PLAN_GROUP,
    REPORT_GROUP,
    BEFORE_GROUP,
    NOT_MOVED,
    NOT_MOVED_APP,
    KEYS_AGAIN,
    NOTES,
    INVITEES,
    SOURCE_KEYS,
    DOWNLOADER_KEYS,
}

enum class MoveNoticeKind {
    READ_DONE,
    READ_FAILED,
    READ_STOPPED,
    LINKED_FAILED,
    IMPORT_FINISHED,
    IMPORT_FAILED,
    IMPORT_UNDONE,
    UNDO_FAILED,
}

data class MoveProblem(
    val error: ApiError,
    val reach: MoveReachFailure = MoveReachFailure.OTHER,
    val noPassword: Boolean = false,
)

data class MoveNotice(
    val seq: Int,
    val kind: MoveNoticeKind,
    val name: String = "",
    val n: Int = 0,
    val id: Long = 0,
    val counts: List<MoveCountPart> = emptyList(),
    val problem: MoveProblem? = null,
)

data class MoveReadForm(
    val kind: String,
    val address: String = "",
    val key: String = "",
    val username: String = "",
    val password: String = "",
    val useSavedKey: Boolean = false,
    val allowLocalNetwork: Boolean = true,
)

data class MoveReadRow(
    val id: Long,
    val kind: String,
    val name: String,
    val address: String,
    val host: String,
    val showHost: Boolean,
    val version: String,
    val createdAt: String,
    val stats: List<MoveCountPart>,
    val usedBy: Long,
    val picked: Boolean,
)

data class MoveSavedKeyRow(
    val kind: String,
    val address: String,
)

data class MoveEarlierRow(
    val id: Long,
    val createdAt: String,
    val counts: List<MoveSummaryPart>,
    val word: MoveStateWord,
    val tone: Tone,
    val canRemove: Boolean,
)

data class MoveBroughtRow(
    val id: Long,
    val app: String,
    val name: String,
    val kind: String,
    val needs: String,
)

data class MoveFoundRow(
    val kind: String,
    val name: String,
    val address: String,
    val version: String,
)

data class MoveLinkedRow(
    val kind: String,
    val name: String,
    val address: String,
    val fourK: Boolean,
    val hasKey: Boolean,
    val reachable: Boolean,
    val version: String,
    val offered: Boolean,
)

data class MoveProbeResult(
    val ok: Boolean,
    val app: String,
    val version: String,
    val problem: MoveProblem? = null,
)

data class MoveSheetState(
    val probing: Boolean = false,
    val probe: MoveProbeResult? = null,
    val detected: String = "",
    val finding: Boolean = false,
    val found: List<MoveFoundRow> = emptyList(),
    val searched: List<String> = emptyList(),
    val findRan: Boolean = false,
    val findProblem: MoveProblem? = null,
    val linked: List<MoveLinkedRow> = emptyList(),
    val linkedFrom: String = "",
    val addingLinked: Boolean = false,
    val reading: Boolean = false,
    val readProblem: MoveProblem? = null,
    val stopped: Boolean = false,
) {
    val addable: List<MoveLinkedRow> get() = linked.filter { it.hasKey && it.reachable }
}

data class MoveFolderRow(
    val oldPath: String,
    val kind: String,
    val unseen: Boolean,
    val options: List<String>,
    val suggested: String,
    val pick: MovePickKind,
    val path: String,
    val other: String,
    val problem: MovePathProblem,
    val gone: String,
    val twins: List<String>,
    val canAddRule: Boolean,
)

data class MoveGroupRow(
    val id: String,
    val group: MoveGroup,
    val label: String,
    val count: Int,
    val on: Boolean,
    val canSkip: Boolean,
    val note: MoveGroupNote,
    val noteCount: Int,
    val showFormats: Boolean,
)

data class MoveTile(
    val id: String,
    val group: MoveGroup,
    val label: String,
    val n: Int,
)

data class MovePersonRow(
    val key: String,
    val name: String,
    val app: String,
    val owner: Boolean,
    val on: Boolean,
)

data class MoveRuleRow(
    val from: String,
    val to: String,
    val problem: MovePathProblem,
)

data class MoveNotMovedApp(
    val app: String,
    val name: String,
    val n: Int,
)

data class MoveChoiceRow(
    val key: String,
    val name: String,
    val why: MoveChooseWhy,
    val choice: MovePersonChoice?,
)

data class MoveBeforeRow(
    val id: String,
    val group: MoveGroup,
    val label: String,
    val n: Int,
    val word: MoveWord,
)

enum class MoveBadge {
    NONE,
    YOU,
    PLEX,
    JELLYFIN,
    EMBY,
    INVITE,
}

data class MoveItemRow(
    val key: String,
    val name: String,
    val hint: String = "",
    val word: MoveWord? = null,
    val raw: String = "",
    val tone: MoveTone = MoveTone.QUIET,
    val look: Boolean = false,
    val traits: String = "",
    val skipped: String = "",
    val needs: String = "",
    val badge: MoveBadge = MoveBadge.NONE,
    val group: MoveGroup = MoveGroup.UNKNOWN,
    val section: String = "",
    val n: Int = 1,
) {
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() || name.lowercase().contains(q) || hint.lowercase().contains(q)
    }
}

data class MoveItemsList(
    val rows: List<MoveItemRow>,
) {
    val anyLook: Boolean get() = rows.any { it.look }
    val showFilter: Boolean get() = anyLook && rows.any { !it.look }

    fun page(
        query: String,
        onlyLook: Boolean,
        limit: Int,
    ): MoveItemsPage {
        val ordered = if (showFilter) rows.filter { it.look } + rows.filter { !it.look } else rows
        val all = ordered.filter { (!onlyLook || !showFilter || it.look) && it.matches(query) }
        return MoveItemsPage(all.take(limit), all.size)
    }
}

data class MoveItemsPage(
    val rows: List<MoveItemRow>,
    val total: Int,
)

data class MoveChooseState(
    val planning: Boolean = false,
    val planned: Boolean = false,
    val note: MoveCheckNote? = null,
    val planProblem: MoveProblem? = null,
    val empty: Boolean = false,
    val tiles: List<MoveTile> = emptyList(),
    val alsoGroups: List<MoveGroup> = emptyList(),
    val stillChecking: Int = 0,
    val needFolders: List<MoveFolderRow> = emptyList(),
    val folders: List<MoveFolderRow> = emptyList(),
    val people: List<MovePersonRow> = emptyList(),
    val namesAsk: Boolean = false,
    val names: MoveNamesChoice = MoveNamesChoice.KEEP,
    val formatsAsk: Boolean = false,
    val noFormats: Boolean = false,
    val sourceKeys: Int = 0,
    val downloaderKeys: Int = 0,
    val invitees: Int = 0,
    val groups: List<MoveGroupRow> = emptyList(),
    val emptyGroups: List<String> = emptyList(),
    val rules: List<MoveRuleRow> = emptyList(),
    val showRules: Boolean = false,
    val notMovedCount: Int = 0,
    val notMovedApps: List<MoveNotMovedApp> = emptyList(),
    val trying: Boolean = false,
    val tried: Boolean = false,
    val stale: Boolean = false,
    val tryFailed: Boolean = false,
    val tryProblem: MoveProblem? = null,
    val tryNotice: Boolean = false,
    val problems: Int = 0,
    val openChoices: Int = 0,
    val total: Int = 0,
    val choices: List<MoveChoiceRow> = emptyList(),
    val before: List<MoveBeforeRow> = emptyList(),
    val outcome: MoveFolderOutcome? = null,
    val apps: List<String> = emptyList(),
    val toCheck: Int = 0,
    val checkingNow: Boolean = false,
)

data class MoveSeasonRow(
    val name: String,
    val titleId: Long,
)

data class MoveResultGroup(
    val id: String,
    val group: MoveGroup,
    val label: String,
    val summary: List<MoveSummaryPart>,
)

data class MoveRunState(
    val id: Long,
    val createdAt: String,
    val word: MoveStateWord,
    val tone: Tone,
    val running: Boolean,
    val undoing: Boolean,
    val undone: Boolean,
    val failed: Boolean,
    val undoFailed: Boolean,
    val progress: Float,
    val note: MoveProgressNote,
    val lost: Boolean,
    val counts: List<MoveSummaryPart>,
    val failedCount: Int,
    val hasReport: Boolean,
    val look: MoveDoneLook,
    val next: List<String>,
    val plainNext: Boolean,
    val noFolder: Int,
    val renameNext: Boolean,
    val openChoices: Int,
    val seasons: List<MoveSeasonRow>,
    val madeAlerts: Boolean,
    val outcome: MoveFolderOutcome?,
    val groups: List<MoveResultGroup>,
    val notMoved: Int,
    val keysAgain: Int,
    val notes: Int,
    val canUndo: Boolean,
    val needsYou: Boolean,
)

class MoveHolder internal constructor(
    internal val data: MoveInner,
)

data class SettingsMoveState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val owner: Boolean = false,
    val safeMode: Boolean = false,
    val step: SettingsMoveStep = SettingsMoveStep.READ,
    val reads: List<MoveReadRow> = emptyList(),
    val pickedCount: Int = 0,
    val savedKeys: List<MoveSavedKeyRow> = emptyList(),
    val moves: List<MoveEarlierRow> = emptyList(),
    val movesProblem: ApiError? = null,
    val brought: List<MoveBroughtRow> = emptyList(),
    val sheet: MoveSheetState = MoveSheetState(),
    val choose: MoveChooseState = MoveChooseState(),
    val run: MoveRunState? = null,
    val gate: MoveGate = MoveGate(true, MoveGateReason.NONE),
    val dialog: MoveDialog = MoveDialog.NONE,
    val starting: Boolean = false,
    val problem: MoveProblem? = null,
    val gone: Boolean = false,
    val notices: List<MoveNotice> = emptyList(),
    val needsPassword: Boolean = false,
    val passwordBusy: Boolean = false,
    val passwordError: ApiError? = null,
    val readingKind: String = "",
    val canStart: Boolean = false,
    internal val holder: MoveHolder = MoveHolder(MoveInner()),
) {
    val isEmpty: Boolean get() = status.phase == ch.orellbuehler.acolyte.feature.Phase.READY && reads.isEmpty() && moves.isEmpty()

    fun savedKey(
        kind: String,
        startAddress: String,
        editing: Boolean,
    ): MoveSavedKeyRow? =
        if (editing) {
            savedKeys.firstOrNull { it.kind == kind && (startAddress.isEmpty() || it.address == startAddress) }
        } else {
            savedKeys.firstOrNull { k -> k.kind == kind && reads.none { it.address == k.address } }
        }
}
